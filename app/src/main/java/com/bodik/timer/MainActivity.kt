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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.TextUnit
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

// ─── Globals ─────────────────────────────────────────────────────────────────

val CustomFontFamily = FontFamily(
    Font(R.font.font_regular, FontWeight.Normal),
    Font(R.font.font_bold, FontWeight.Bold)
)

@SuppressLint("DefaultLocale")
fun formatTime(seconds: Int): String = String.format("%d:%02d", seconds / 60, seconds % 60)

val Context.dataStore by preferencesDataStore(name = "settings")

@RequiresApi(Build.VERSION_CODES.O)
fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
}

fun playSound(context: Context, resId: Int) {
    MediaPlayer.create(context.applicationContext, resId)?.apply {
        setOnCompletionListener { release() }
        start()
    }
}

// ─── Activity ─────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private var timerService: TimerService? = null
    private var isBound = false
    private val _serviceState = MutableStateFlow(TimerState())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            timerService = (binder as TimerService.LocalBinder).getService()
            isBound = true
            lifecycleScope.launch { timerService!!.state.collect { _serviceState.value = it } }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bindService(Intent(this, TimerService::class.java), connection, 0)
        setContent {
            TimerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) { innerPadding ->
                    TimerScreen(
                        modifier = Modifier.padding(innerPadding),
                        serviceState = _serviceState,
                        onStart = ::startTimer,
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        if (!isBound) bindService(Intent(this, TimerService::class.java), connection, 0)
    }

    private fun sendAction(action: String) =
        startService(Intent(this, TimerService::class.java).apply { this.action = action })

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection); isBound = false
        }
        super.onDestroy()
    }
}

// ─── DataStore keys (stable, created once) ────────────────────────────────────

private val WORK_KEY = floatPreferencesKey("work_seconds")
private val REST_KEY = floatPreferencesKey("rest_seconds")
private val REPEATS_KEY = floatPreferencesKey("repeats")

// ─── TimerScreen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    var setWorkSeconds by remember { mutableStateOf(120f) }
    var setRestSeconds by remember { mutableStateOf(30f) }
    var setRepeats by remember { mutableStateOf(10f) }
    var showSheet by remember { mutableStateOf(false) }
    var activePicker by remember { mutableStateOf("") }

    val smoothProgress = remember { Animatable(1f) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    // Load saved settings
    LaunchedEffect(Unit) {
        context.dataStore.data.map { prefs ->
            setWorkSeconds = prefs[WORK_KEY] ?: 120f
            setRestSeconds = prefs[REST_KEY] ?: 30f
            setRepeats = prefs[REPEATS_KEY] ?: 10f
        }.first()
    }

    // Animate arc progress
    LaunchedEffect(
        timerState.isRunning,
        timerState.isFinished,
        timerState.timeLeft,
        timerState.isWorkPhase
    ) {
        when {
            timerState.isFinished -> smoothProgress.snapTo(0f)
            timerState.isRunning && timerState.timeLeft > 0 -> {
                val total =
                    if (timerState.isWorkPhase) timerState.workSeconds else timerState.restSeconds
                smoothProgress.animateTo(
                    targetValue = (timerState.timeLeft - 1).toFloat() / total,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
            }

            !timerState.isRunning -> smoothProgress.stop()
        }
    }

    // Snap to full at phase start
    LaunchedEffect(timerState.isWorkPhase, timerState.currentRepeat) {
        if (timerState.currentRepeat > 0) smoothProgress.snapTo(1f)
    }

    // Countdown beeps + vibration
    LaunchedEffect(timerState.timeLeft) {
        if (timerState.isRunning && timerState.timeLeft in 1..3) {
            val soundId = when (timerState.timeLeft) {
                3 -> R.raw.finish_1
                2 -> R.raw.finish_2
                else -> R.raw.finish_3
            }
            playSound(context, soundId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrate(context)
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
            IdleDisplay(
                workSeconds = setWorkSeconds.toInt(),
                restSeconds = setRestSeconds.toInt(),
                repeats = setRepeats.toInt(),
                onPickerOpen = { picker -> activePicker = picker; showSheet = true }
            )
        } else {
            ActiveTimerDisplay(
                timerState = timerState,
                smoothProgress = smoothProgress.value,
                primaryColor = primaryColor,
                errorColor = errorColor,
                trackColor = trackColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isActive) {
                AnimatedButton(
                    onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrate(context) },
                    onLongClick = {
                        onStop()
                        scope.launch { smoothProgress.snapTo(1f) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    containerColor = Color.Transparent,
                    contentColor = primaryColor,
                    isOutlined = true
                ) {
                    Icon(
                        painter = painterResource(R.drawable.stop),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            AnimatedButton(
                onClick = {
                    when {
                        !isActive -> onStart(
                            setWorkSeconds.toInt(),
                            setRestSeconds.toInt(),
                            setRepeats.toInt()
                        )

                        timerState.isRunning -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrate(context); onPause()
                        }

                        else -> onResume()
                    }
                },
                modifier = Modifier
                    .weight(2f)
                    .height(72.dp),
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(if (timerState.isRunning) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                scope.launch {
                    context.dataStore.edit {
                        it[WORK_KEY] = setWorkSeconds
                        it[REST_KEY] = setRestSeconds
                        it[REPEATS_KEY] = setRepeats
                    }
                }
            }
        ) {
            SettingsPicker(
                activePicker = activePicker,
                workSeconds = setWorkSeconds,
                restSeconds = setRestSeconds,
                repeats = setRepeats,
                primaryColor = primaryColor,
                onWorkChange = { setWorkSeconds = it },
                onRestChange = { setRestSeconds = it },
                onRepeatsChange = { setRepeats = it }
            )
        }
    }
}

// ─── Idle screen ──────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.IdleDisplay(
    workSeconds: Int,
    restSeconds: Int,
    repeats: Int,
    onPickerOpen: (String) -> Unit
) {
    Spacer(modifier = Modifier.weight(0.5f))
    TimerValueDisplay(
        label = stringResource(R.string.work),
        value = formatTime(workSeconds),
        labelFontSize = 24.sp,
        valueFontSize = 94.sp,
        onClick = { onPickerOpen("work") }
    )
    Spacer(modifier = Modifier.height(40.dp))
    TimerValueDisplay(
        label = stringResource(R.string.rest),
        value = formatTime(restSeconds),
        onClick = { onPickerOpen("rest") }
    )
    Spacer(modifier = Modifier.height(40.dp))
    TimerValueDisplay(
        label = stringResource(R.string.repeats),
        value = "$repeats",
        onClick = { onPickerOpen("repeats") }
    )
    Spacer(modifier = Modifier.weight(1f))
}

// ─── Active timer display ─────────────────────────────────────────────────────

@Composable
private fun ColumnScope.ActiveTimerDisplay(
    timerState: TimerState,
    smoothProgress: Float,
    primaryColor: Color,
    errorColor: Color,
    trackColor: Color
) {
    val phaseColor = if (timerState.isWorkPhase) primaryColor else errorColor

    Spacer(modifier = Modifier.weight(0.5f))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = trackColor, style = Stroke(width = 10.dp.toPx()))
            drawArc(
                color = phaseColor,
                startAngle = -90f,
                sweepAngle = 360 * smoothProgress,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (timerState.isWorkPhase) stringResource(R.string.work).uppercase()
                else stringResource(R.string.rest).uppercase(),
                fontFamily = CustomFontFamily,
                fontSize = 20.sp,
                color = phaseColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(timerState.timeLeft),
                fontFamily = CustomFontFamily,
                fontSize = 84.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(
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


// ─── Settings bottom sheet content ────────────────────────────────────────────

@Composable
private fun SettingsPicker(
    activePicker: String,
    workSeconds: Float,
    restSeconds: Float,
    repeats: Float,
    primaryColor: Color,
    onWorkChange: (Float) -> Unit,
    onRestChange: (Float) -> Unit,
    onRepeatsChange: (Float) -> Unit
) {
    val isWork = activePicker == "work"
    val isRepeats = activePicker == "repeats"

    val title = when (activePicker) {
        "work" -> stringResource(R.string.work)
        "rest" -> stringResource(R.string.rest)
        else -> stringResource(R.string.repeats)
    }.uppercase()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 64.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontFamily = CustomFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (isRepeats) {
            Slider(
                value = repeats,
                onValueChange = { onRepeatsChange(it.roundToInt().toFloat()) },
                valueRange = 1f..20f,
                steps = 19
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "${repeats.toInt()}",
                fontFamily = CustomFontFamily,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        } else {
            val step = if (isWork) 30f else 10f
            val range = if (isWork) 30f..1800f else 0f..300f
            val current = if (isWork) workSeconds else restSeconds

            Slider(
                value = current,
                onValueChange = { raw ->
                    val snapped = max(range.start, (raw / step).roundToInt() * step)
                    if (isWork) onWorkChange(snapped) else onRestChange(snapped)
                },
                valueRange = range
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = formatTime(current.toInt()),
                fontFamily = CustomFontFamily,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }
    }
}

// ─── Reusable components ──────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    isOutlined: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")
    val shape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(shape)
            .background(containerColor)
            .then(
                if (isOutlined) Modifier.border(ButtonDefaults.outlinedButtonBorder, shape)
                else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
fun TimerValueDisplay(
    label: String,
    value: String,
    labelFontSize: TextUnit = 20.sp,
    valueFontSize: TextUnit = 84.sp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = CustomFontFamily,
            color = MaterialTheme.colorScheme.outline,
            fontSize = labelFontSize,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontFamily = CustomFontFamily,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}