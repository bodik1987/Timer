package com.bodik.timer

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bodik.timer.ui.theme.TimerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// ОПРЕДЕЛЕНИЕ ШРИФТОВ
val CustomFontFamily = FontFamily(
    Font(R.font.font_regular, FontWeight.Normal),
    Font(R.font.font_bold, FontWeight.Bold)
)

// Настройки DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) { innerPadding ->
                    TimerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val WORK_KEY = floatPreferencesKey("work_seconds")
    val REST_KEY = floatPreferencesKey("rest_seconds")
    val REPEATS_KEY = floatPreferencesKey("repeats")

    var setWorkSeconds by remember { mutableStateOf(120f) }
    var setRestSeconds by remember { mutableStateOf(30f) }
    var setRepeats by remember { mutableStateOf(10f) }

    var timeLeft by remember { mutableStateOf(0) }
    var currentRepeat by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var isWorkPhase by remember { mutableStateOf(true) }

    var showSheet by remember { mutableStateOf(false) }
    var activePicker by remember { mutableStateOf("") }

    val smoothProgress = remember { Animatable(1f) }

    // Цвета из стандартной схемы Material3
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(Unit) {
        context.dataStore.data.map { prefs ->
            setWorkSeconds = prefs[WORK_KEY] ?: 120f
            setRestSeconds = prefs[REST_KEY] ?: 30f
            setRepeats = prefs[REPEATS_KEY] ?: 10f
        }.first()
    }

    LaunchedEffect(isRunning, timeLeft, isWorkPhase) {
        if (isRunning && timeLeft > 0) {
            val totalTime = if (isWorkPhase) setWorkSeconds else setRestSeconds
            smoothProgress.animateTo(
                targetValue = (timeLeft - 1).toFloat() / totalTime,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )
        } else if (!isRunning) {
            smoothProgress.stop()
        }
    }

    LaunchedEffect(isWorkPhase, currentRepeat) {
        if (currentRepeat > 0) {
            smoothProgress.snapTo(1f)
        }
    }

    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    fun playSound(resId: Int) {
        MediaPlayer.create(context, resId)?.apply {
            setOnCompletionListener { release() }
            start()
        }
    }

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            if (timeLeft <= 3) {
                val soundId = when (timeLeft) {
                    3 -> R.raw.finish_1
                    2 -> R.raw.finish_2
                    else -> R.raw.finish_3
                }
                playSound(soundId)
                vibrate()
            }
            delay(1000L)
            timeLeft -= 1
        } else if (isRunning && timeLeft == 0) {
            val totalRounds = setRepeats.toInt()
            if (isWorkPhase) {
                if (currentRepeat >= totalRounds) {
                    isRunning = false
                    currentRepeat = 0
                } else {
                    isWorkPhase = false
                    timeLeft = setRestSeconds.toInt()
                }
            } else {
                currentRepeat += 1
                isWorkPhase = true
                timeLeft = setWorkSeconds.toInt()
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(seconds: Int): String = String.format("%d:%02d", seconds / 60, seconds % 60)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        if (!isRunning && currentRepeat == 0) {
            Spacer(modifier = Modifier.weight(0.5f))
            TimerValueDisplay("Работа", formatTime(setWorkSeconds.toInt())) {
                activePicker = "work"; showSheet = true
            }
            Spacer(modifier = Modifier.height(40.dp))
            TimerValueDisplay("Отдых", formatTime(setRestSeconds.toInt())) {
                activePicker = "rest"; showSheet = true
            }
            Spacer(modifier = Modifier.height(40.dp))
            TimerValueDisplay("Повторы", "${setRepeats.toInt()}") {
                activePicker = "repeats"; showSheet = true
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(0.5f))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = trackColor, style = Stroke(width = 10.dp.toPx()))
                    drawArc(
                        color = if (isWorkPhase) primaryColor else errorColor,
                        startAngle = -90f,
                        sweepAngle = 360 * smoothProgress.value,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isWorkPhase) "Работа" else "Отдых",
                        fontFamily = CustomFontFamily,
                        fontSize = 18.sp,
                        color = if (isWorkPhase) primaryColor else errorColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatTime(timeLeft),
                        fontFamily = CustomFontFamily,
                        fontSize = 84.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Круг: $currentRepeat из ${setRepeats.toInt()}",
                        fontFamily = CustomFontFamily,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isRunning || currentRepeat > 0) {
                AnimatedTomatoButton(
                    onClick = {
                        isRunning = false; currentRepeat = 0; isWorkPhase =
                        true; scope.launch { smoothProgress.snapTo(1f) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = primaryColor,
                    isOutlined = true
                ) { Text("СТОП", fontFamily = CustomFontFamily, fontWeight = FontWeight.Bold) }
            }
            AnimatedTomatoButton(
                onClick = {
                    if (!isRunning && currentRepeat == 0) {
                        timeLeft = setWorkSeconds.toInt(); currentRepeat = 1; isWorkPhase = true
                    }
                    isRunning = !isRunning
                },
                modifier = Modifier
                    .weight(2f)
                    .height(72.dp),
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    if (isRunning) "ПАУЗА" else "СТАРТ",
                    fontFamily = CustomFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = {
            showSheet = false
            scope.launch {
                context.dataStore.edit {
                    it[WORK_KEY] = setWorkSeconds; it[REST_KEY] = setRestSeconds; it[REPEATS_KEY] =
                    setRepeats
                }
            }
        }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 64.dp, start = 32.dp, end = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isWork = activePicker == "work"
                Text(
                    if (isWork) "Работа (мин 30 сек)" else if (activePicker == "rest") "Отдых" else "Повторы",
                    fontFamily = CustomFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (activePicker == "repeats") {
                    Slider(
                        value = setRepeats,
                        onValueChange = { setRepeats = it.roundToInt().toFloat() },
                        valueRange = 1f..20f,
                        steps = 19
                    )
                    Text(
                        "${setRepeats.toInt()}",
                        fontFamily = CustomFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                } else {
                    Slider(
                        value = if (isWork) setWorkSeconds else setRestSeconds,
                        onValueChange = {
                            val step = if (isWork) 30f else 10f
                            val value = (it / step).roundToInt() * step
                            if (isWork) setWorkSeconds = max(30f, value) else setRestSeconds = value
                        },
                        valueRange = (if (isWork) 30f else 0f)..(if (isWork) 1800f else 300f)
                    )
                    Text(
                        formatTime(if (isWork) setWorkSeconds.toInt() else setRestSeconds.toInt()),
                        fontFamily = CustomFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedTomatoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    isOutlined: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "")

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(36.dp),
        color = containerColor,
        contentColor = contentColor,
        border = if (isOutlined) ButtonDefaults.outlinedButtonBorder else null,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun TimerValueDisplay(label: String, value: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(
            label.uppercase(),
            fontFamily = CustomFontFamily,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            fontFamily = CustomFontFamily,
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}