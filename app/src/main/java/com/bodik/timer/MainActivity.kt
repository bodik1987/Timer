package com.bodik.timer

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
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

    val showSheet = remember { mutableStateOf(false) }
    val activePicker = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        context.dataStore.data.map { prefs ->
            setWorkSeconds = prefs[WORK_KEY] ?: 120f
            setRestSeconds = prefs[REST_KEY] ?: 30f
            setRepeats = prefs[REPEATS_KEY] ?: 10f
        }.first()
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

    LaunchedEffect(isRunning, timeLeft, isWorkPhase) {
        if (isRunning && timeLeft > 0) {
            when (timeLeft) {
                3 -> {
                    playSound(R.raw.finish_1); vibrate()
                }

                2 -> {
                    playSound(R.raw.finish_2); vibrate()
                }

                1 -> {
                    playSound(R.raw.finish_3); vibrate()
                }
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
                    if (setRestSeconds > 0) {
                        isWorkPhase = false
                        timeLeft = setRestSeconds.toInt()
                    } else {
                        currentRepeat += 1
                        timeLeft = setWorkSeconds.toInt()
                    }
                }
            } else {
                currentRepeat += 1
                isWorkPhase = true
                timeLeft = setWorkSeconds.toInt()
            }
        }
    }

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
                activePicker.value = "work"; showSheet.value = true
            }
            Spacer(modifier = Modifier.height(40.dp))
            TimerValueDisplay("Отдых", formatTime(setRestSeconds.toInt())) {
                activePicker.value = "rest"; showSheet.value = true
            }
            Spacer(modifier = Modifier.height(40.dp))
            TimerValueDisplay("Повторы", "${setRepeats.toInt()}") {
                activePicker.value = "repeats"; showSheet.value = true
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(0.5f))

            // КРУГОВАЯ АНИМАЦИЯ
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                val totalPhaseTime = if (isWorkPhase) setWorkSeconds else setRestSeconds
                val progress by animateFloatAsState(
                    targetValue = if (totalPhaseTime > 0) timeLeft / totalPhaseTime else 0f,
                    label = "TimerProgress"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Фоновое кольцо (серое)
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        style = Stroke(width = 12.dp.toPx())
                    )

                    // Активный прогресс
                    drawArc(
                        color = if (isWorkPhase) Color(0xFF4CAF50) else Color(0xFFF44336),
                        startAngle = -90f,
                        sweepAngle = 360 * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isWorkPhase) "РАБОТА" else "ОТДЫХ",
                        fontSize = 20.sp,
                        color = if (isWorkPhase) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = formatTime(timeLeft),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Круг: $currentRepeat из ${setRepeats.toInt()}",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // КНОПКИ УПРАВЛЕНИЯ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isRunning || currentRepeat > 0) {
                OutlinedButton(
                    onClick = { isRunning = false; currentRepeat = 0; isWorkPhase = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    shape = RoundedCornerShape(24.dp)
                ) { Text("СТОП", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }

            Button(
                onClick = {
                    if (!isRunning && currentRepeat == 0) {
                        timeLeft = setWorkSeconds.toInt()
                        currentRepeat = 1
                        isWorkPhase = true
                    }
                    isRunning = !isRunning
                },
                modifier = Modifier
                    .weight(2f)
                    .height(72.dp),
                shape = RoundedCornerShape(36.dp)
            ) {
                Text(
                    if (isRunning) "ПАУЗА" else "СТАРТ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    // BOTTOM SHEET
    if (showSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet.value = false
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs[WORK_KEY] = setWorkSeconds
                        prefs[REST_KEY] = setRestSeconds
                        prefs[REPEATS_KEY] = setRepeats
                    }
                }
            }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 64.dp, start = 32.dp, end = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isWork = activePicker.value == "work"
                val isRest = activePicker.value == "rest"
                Text(
                    if (isWork) "Работа (мин 30 сек)" else if (isRest) "Отдых (макс 5 мин)" else "Повторы",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                when (activePicker.value) {
                    "work", "rest" -> {
                        Slider(
                            value = if (isWork) setWorkSeconds else setRestSeconds,
                            onValueChange = {
                                val step = if (isWork) 30f else 10f
                                val stepped = (it / step).roundToInt() * step
                                if (isWork) setWorkSeconds = max(30f, stepped) else setRestSeconds =
                                    stepped
                            },
                            valueRange = (if (isWork) 30f else 0f)..(if (isWork) 1800f else 300f)
                        )
                        Text(
                            formatTime(if (isWork) setWorkSeconds.toInt() else setRestSeconds.toInt()),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    "repeats" -> {
                        Slider(
                            value = setRepeats,
                            onValueChange = { setRepeats = it.roundToInt().toFloat() },
                            valueRange = 1f..20f,
                            steps = 19
                        )
                        Text(
                            "${setRepeats.toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
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
            color = MaterialTheme.colorScheme.outline,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            value,
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}