package com.bodik.timer

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.bodik.timer.ui.theme.LocalFontFamily
import com.bodik.timer.ui.theme.TimerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// ─── Globals ─────────────────────────────────────────────────────────────────

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
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var activeTheme by remember { mutableStateOf(AppThemes.first()) }
            var selectedFont by remember { mutableStateOf(AvailableFonts.first()) }

            LaunchedEffect(Unit) {
                context.dataStore.data.map { it[THEME_KEY] }.first()
                    ?.let { activeTheme = themeById(it) }
                context.dataStore.data.map { it[FONT_KEY] }.first()
                    ?.let { selectedFont = fontById(it) }
            }

            TimerTheme(
                appTheme = activeTheme,
                fontFamily = selectedFont.fontFamily
            ) {
                TimerScreen(
                    serviceState = _serviceState,
                    onStart = ::startTimer,
                    onPause = { sendAction(TimerService.ACTION_PAUSE) },
                    onResume = { sendAction(TimerService.ACTION_RESUME) },
                    onStop = { sendAction(TimerService.ACTION_STOP) },
                    activeTheme = activeTheme,
                    onThemeChange = { theme ->
                        activeTheme = theme
                        scope.launch {
                            context.dataStore.edit { it[THEME_KEY] = theme.id }
                        }
                    },
                    selectedFont = selectedFont,
                    onFontChange = { font ->
                        selectedFont = font
                        scope.launch {
                            context.dataStore.edit { it[FONT_KEY] = font.id }
                        }
                    }
                )
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

// ─── DataStore keys ───────────────────────────────────────────────────────────

private val WORK_KEY = floatPreferencesKey("work_seconds")
private val REST_KEY = floatPreferencesKey("rest_seconds")
private val REPEATS_KEY = floatPreferencesKey("repeats")
private val THEME_KEY = stringPreferencesKey("theme_id")
private val FONT_KEY = stringPreferencesKey("font_id")

// ─── Status Bar Management ────────────────────────────────────────────────────

@Composable
private fun UpdateStatusBar() {
    val view = LocalView.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val isBackgroundLight = backgroundColor.luminance() > 0.5f

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                isBackgroundLight
        }
    }
}

// ─── TimerScreen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    serviceState: StateFlow<TimerState>,
    onStart: (Int, Int, Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    activeTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    selectedFont: FontOption,
    onFontChange: (FontOption) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val timerState by serviceState.collectAsState()

    var setWorkSeconds by remember { mutableStateOf(120f) }
    var setRestSeconds by remember { mutableStateOf(30f) }
    var setRepeats by remember { mutableStateOf(10f) }

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var activePicker by remember { mutableStateOf("") }

    val smoothProgress = remember { Animatable(1f) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val timerTextColor = activeTheme.timerTextColor ?: MaterialTheme.colorScheme.onSurface
    val accentColor = activeTheme.accentColor ?: primaryColor
    val labelColor = activeTheme.labelColor ?: primaryColor.copy(alpha = 0.7f)

    // Автоматически обновляем цвет иконок статус-бара в зависимости от яркости фона
    UpdateStatusBar()

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

    LaunchedEffect(timerState.isWorkPhase, timerState.currentRepeat) {
        if (timerState.currentRepeat > 0) smoothProgress.snapTo(1f)
    }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = { showThemeSheet = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Themes",
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isActive) {
                IdleDisplay(
                    workSeconds = setWorkSeconds.toInt(),
                    restSeconds = setRestSeconds.toInt(),
                    repeats = setRepeats.toInt(),
                    timerTextColor = timerTextColor,
                    labelColor = labelColor,
                    onPickerOpen = { picker -> activePicker = picker; showSettingsSheet = true }
                )
            } else {
                ActiveTimerDisplay(
                    timerState = timerState,
                    smoothProgress = smoothProgress.value,
                    accentColor = accentColor,
                    errorColor = errorColor,
                    trackColor = trackColor,
                    timerTextColor = timerTextColor,
                    labelColor = labelColor,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isActive) {
                    AnimatedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrate(
                                context
                            )
                        },
                        onLongClick = {
                            onStop()
                            scope.launch { smoothProgress.snapTo(1f) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp),
                        containerColor = Color.Transparent,
                        contentColor = accentColor,
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
                        .height(84.dp),
                    containerColor = accentColor,
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
    }

    // --- Bottom Sheet: Настройки времени ---
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSettingsSheet = false
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

    // --- Bottom Sheet: Выбор темы и шрифта ---
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Themes",
                    fontFamily = LocalFontFamily.current,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ThemeSelector(
                    activeTheme = activeTheme,
                    onThemeChange = onThemeChange
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Fonts",
                    fontFamily = LocalFontFamily.current,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FontSelector(
                    selectedFont = selectedFont,
                    onFontChange = onFontChange
                )
            }
        }
    }
}

// ─── UI Sub-components (Idle, Active, Settings, etc) ──────────────────────────

@Composable
private fun ColumnScope.IdleDisplay(
    workSeconds: Int,
    restSeconds: Int,
    repeats: Int,
    timerTextColor: Color,
    labelColor: Color,
    onPickerOpen: (String) -> Unit
) {
    Spacer(modifier = Modifier.weight(0.3f))
    TimerValueDisplay(
        label = stringResource(R.string.work),
        value = formatTime(workSeconds),
        labelFontSize = 24.sp,
        valueFontSize = 94.sp,
        valueColor = timerTextColor,
        labelColor = labelColor,
        onClick = { onPickerOpen("work") }
    )
    Spacer(modifier = Modifier.height(30.dp))
    TimerValueDisplay(
        label = stringResource(R.string.rest),
        value = formatTime(restSeconds),
        valueColor = timerTextColor,
        labelColor = labelColor,
        onClick = { onPickerOpen("rest") }
    )
    Spacer(modifier = Modifier.height(30.dp))
    TimerValueDisplay(
        label = stringResource(R.string.repeats),
        value = "$repeats",
        valueColor = timerTextColor,
        labelColor = labelColor,
        onClick = { onPickerOpen("repeats") }
    )
    Spacer(modifier = Modifier.weight(1f))
}

@Composable
private fun ColumnScope.ActiveTimerDisplay(
    timerState: TimerState,
    smoothProgress: Float,
    accentColor: Color,
    errorColor: Color,
    trackColor: Color,
    timerTextColor: Color,
    labelColor: Color,
) {
    val phaseColor = if (timerState.isWorkPhase) accentColor else errorColor
    Spacer(modifier = Modifier.weight(0.5f))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = trackColor, style = Stroke(width = 16.dp.toPx()))
            drawArc(
                color = phaseColor,
                startAngle = -90f,
                sweepAngle = 360 * smoothProgress,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (timerState.isWorkPhase) stringResource(R.string.work).uppercase() else stringResource(
                    R.string.rest
                ).uppercase(),
                fontFamily = LocalFontFamily.current,
                fontSize = 20.sp,
                color = labelColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(timerState.timeLeft),
                fontFamily = LocalFontFamily.current,
                fontSize = 84.sp,
                fontWeight = FontWeight.Black,
                color = timerTextColor
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(
                    R.string.round,
                    timerState.currentRepeat,
                    timerState.totalRepeats
                ),
                fontFamily = LocalFontFamily.current,
                fontSize = 34.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(modifier = Modifier.weight(1f))
}

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
        Text(
            title,
            fontFamily = LocalFontFamily.current,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
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
                "${repeats.toInt()}",
                fontFamily = LocalFontFamily.current,
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
                formatTime(current.toInt()),
                fontFamily = LocalFontFamily.current,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }
    }
}

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
    val shape = CircleShape

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(if (isOutlined) Modifier.border(1.5.dp, contentColor, shape) else Modifier)
            .clip(shape)
            .background(containerColor)
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
fun ThemeSelector(
    activeTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(AppThemes) { theme ->
            val isSelected = theme.id == activeTheme.id
            val themeColor = if (isDark) theme.darkColors.primary else theme.lightColors.primary
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) themeColor else Color.Transparent)
                    .border(width = 1.5.dp, color = themeColor, shape = shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onThemeChange(theme) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = theme.label,
                    fontFamily = LocalFontFamily.current,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else themeColor
                )
            }
        }
    }
}

@Composable
fun FontSelector(
    selectedFont: FontOption,
    onFontChange: (FontOption) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(AvailableFonts) { font ->
            val isSelected = font.id == selectedFont.id
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFontChange(font) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = font.label,
                    fontFamily = font.fontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TimerValueDisplay(
    label: String, value: String, labelFontSize: TextUnit = 20.sp, valueFontSize: TextUnit = 84.sp,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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
            fontFamily = LocalFontFamily.current,
            color = labelColor,
            fontSize = labelFontSize,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontFamily = LocalFontFamily.current,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
    }
}