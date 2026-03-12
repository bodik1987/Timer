package com.bodik.timer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.bodik.timer.ui.theme.TimerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

val CustomFontFamily = FontFamily(
    Font(R.font.font_regular, FontWeight.Normal),
    Font(R.font.font_bold, FontWeight.Bold)
)

@SuppressLint("DefaultLocale")
fun formatTime(seconds: Int): String = String.format("%d:%02d", seconds / 60, seconds % 60)

val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    private var timerService: TimerService? = null
    private var isBound = false

    private val _serviceState = MutableStateFlow(TimerState())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as TimerService.LocalBinder
            timerService = localBinder.getService()
            isBound = true
            lifecycleScope.launch {
                timerService!!.state.collect { _serviceState.value = it }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, 0) // 0 = не запускать автоматически
        }

        setContent {
            TimerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) { innerPadding ->
                    TimerScreen(
                        modifier = Modifier.padding(innerPadding),
                        serviceState = _serviceState,
                        onStart = { work, rest, repeats -> startTimer(work, rest, repeats) },
                        onPause = { sendAction(TimerService.ACTION_PAUSE) },
                        onResume = { sendAction(TimerService.ACTION_RESUME) },
                        onStop = { sendAction(TimerService.ACTION_STOP) }
                    )
                }
            }
        }
    }

    private fun startTimer(workSeconds: Int, restSeconds: Int, repeats: Int) {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_WORK_SECONDS, workSeconds)
            putExtra(TimerService.EXTRA_REST_SECONDS, restSeconds)
            putExtra(TimerService.EXTRA_REPEATS, repeats)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        if (!isBound) {
            bindService(Intent(this, TimerService::class.java), connection, 0)
        }
    }

    private fun sendAction(action: String) {
        val intent = Intent(this, TimerService::class.java).apply { this.action = action }
        startService(intent)
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    serviceState: StateFlow<TimerState>,
    onStart: (Int, Int, Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val timerState by serviceState.collectAsState()

    val WORK_KEY = floatPreferencesKey("work_seconds")
    val REST_KEY = floatPreferencesKey("rest_seconds")
    val REPEATS_KEY = floatPreferencesKey("repeats")

    var setWorkSeconds by remember { mutableStateOf(120f) }
    var setRestSeconds by remember { mutableStateOf(30f) }
    var setRepeats by remember { mutableStateOf(10f) }

    var showSheet by remember { mutableStateOf(false) }
    var activePicker by remember { mutableStateOf("") }

    val smoothProgress = remember { Animatable(1f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(Unit) {
        context.dataStore.data.map { prefs ->
            setWorkSeconds = prefs[WORK_KEY] ?: 120f
            setRestSeconds = prefs[REST_KEY] ?: 30f
            setRepeats = prefs[REPEATS_KEY] ?: 10f
        }.first()
    }

    LaunchedEffect(
        timerState.isRunning,
        timerState.isFinished,
        timerState.timeLeft,
        timerState.isWorkPhase
    ) {
        when {
            // FIX: при завершении таймера сбрасываем дугу в 0
            timerState.isFinished -> smoothProgress.snapTo(0f)
            timerState.isRunning && timerState.timeLeft > 0 -> {
                val totalTime =
                    if (timerState.isWorkPhase) timerState.workSeconds else timerState.restSeconds
                smoothProgress.animateTo(
                    targetValue = (timerState.timeLeft - 1).toFloat() / totalTime,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
            }

            !timerState.isRunning -> smoothProgress.stop()
        }
    }

    LaunchedEffect(timerState.isWorkPhase, timerState.currentRepeat) {
        if (timerState.currentRepeat > 0) smoothProgress.snapTo(1f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun playSound(resId: Int) {
        MediaPlayer.create(context.applicationContext, resId)?.apply {
            setOnCompletionListener { release() }
            start()
        }
    }

    LaunchedEffect(timerState.timeLeft) {
        if (timerState.isRunning && timerState.timeLeft in 1..3) {
            val soundId = when (timerState.timeLeft) {
                3 -> R.raw.finish_1
                2 -> R.raw.finish_2
                else -> R.raw.finish_3
            }
            playSound(soundId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrate()
        }
    }
    val isActive = (timerState.isRunning || timerState.currentRepeat > 0) && !timerState.isFinished

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        if (!isActive) {
            // Экран настройки
            Spacer(modifier = Modifier.weight(0.5f))
            TimerValueDisplay(stringResource(R.string.work), formatTime(setWorkSeconds.toInt())) {
                activePicker = "work"; showSheet = true
            }
            Spacer(modifier = Modifier.height(40.dp))
            TimerValueDisplay(stringResource(R.string.rest), formatTime(setRestSeconds.toInt())) {
                activePicker = "rest"; showSheet = true
            }
            Spacer(modifier = Modifier.height(40.dp))
            TimerValueDisplay(stringResource(R.string.repeats), "${setRepeats.toInt()}") {
                activePicker = "repeats"; showSheet = true
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Экран таймера
            Spacer(modifier = Modifier.weight(0.5f))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = trackColor, style = Stroke(width = 10.dp.toPx()))
                    drawArc(
                        color = if (timerState.isWorkPhase) primaryColor else errorColor,
                        startAngle = -90f,
                        sweepAngle = 360 * smoothProgress.value,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (timerState.isWorkPhase) stringResource(R.string.work).uppercase() else stringResource(
                            R.string.rest
                        ).uppercase(),
                        fontFamily = CustomFontFamily,
                        fontSize = 20.sp,
                        color = if (timerState.isWorkPhase) primaryColor else errorColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatTime(timerState.timeLeft),
                        fontFamily = CustomFontFamily,
                        fontSize = 84.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        stringResource(
                            R.string.round,
                            timerState.currentRepeat,
                            timerState.totalRepeats
                        ),
                        fontFamily = CustomFontFamily,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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
            if (isActive) {
                AnimatedTomatoButton(
                    onClick = {
                        onStop()
                        scope.launch { smoothProgress.snapTo(1f) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = primaryColor,
                    isOutlined = true
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stop),
                        contentDescription = "",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            AnimatedTomatoButton(
                onClick = {
                    if (!isActive) {
                        onStart(setWorkSeconds.toInt(), setRestSeconds.toInt(), setRepeats.toInt())
                    } else if (timerState.isRunning) {
                        onPause()
                    } else {
                        onResume()
                    }
                },
                modifier = Modifier
                    .weight(2f)
                    .height(72.dp),
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = if (timerState.isRunning) painterResource(id = R.drawable.pause)
                    else painterResource(id = R.drawable.play),
                    contentDescription = "",
                    modifier = Modifier.size(24.dp)
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
                    it[WORK_KEY] = setWorkSeconds
                    it[REST_KEY] = setRestSeconds
                    it[REPEATS_KEY] = setRepeats
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
                    if (isWork) stringResource(R.string.work).uppercase()
                    else if (activePicker == "rest") stringResource(R.string.rest).uppercase()
                    else stringResource(R.string.repeats).uppercase(),
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
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "${setRepeats.toInt()}",
                        fontFamily = CustomFontFamily,
                        fontSize = 48.sp,
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
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        formatTime(if (isWork) setWorkSeconds.toInt() else setRestSeconds.toInt()),
                        fontFamily = CustomFontFamily,
                        fontSize = 48.sp,
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
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Text(
            label.uppercase(),
            fontFamily = CustomFontFamily,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            fontFamily = CustomFontFamily,
            fontSize = 84.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}