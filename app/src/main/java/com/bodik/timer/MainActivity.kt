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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bodik.timer.ui.theme.AppTheme
import com.bodik.timer.ui.theme.AppThemes
import com.bodik.timer.ui.theme.AvailableFonts
import com.bodik.timer.ui.theme.FontOption
import com.bodik.timer.ui.theme.LocalFontFamily
import com.bodik.timer.ui.theme.ShapeDefaults.FirstLazyRowItemShape
import com.bodik.timer.ui.theme.ShapeDefaults.LastLazyRowItemShape
import com.bodik.timer.ui.theme.ShapeDefaults.middleListItemShape
import com.bodik.timer.ui.theme.TimerTheme
import com.bodik.timer.ui.theme.fontById
import com.bodik.timer.ui.theme.themeById
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
    val errorColor = Color(0xFFE53935)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val timerTextColor = activeTheme.timerTextColor ?: MaterialTheme.colorScheme.onSurface
    val accentColor = activeTheme.accentColor ?: primaryColor


//    val lottieLading by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))
//    val progressLottieLading by animateLottieCompositionAsState(
//        composition = lottieLading,
//        iterations = LottieConstants.IterateForever,
//        // Если хочешь, чтобы она крутилась ВСЕГДА (даже когда таймер стоит):
//        isPlaying = true,
//        // А здесь управляй скоростью: если таймер стоит — скорость 0.5, если идет — 1.0
//        speed = if (timerState.isRunning) 1f else 0.5f
//    )

    val lottieRun by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.runs))
    val progressLottieRun by animateLottieCompositionAsState(
        composition = lottieRun,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
        speed = if (timerState.isWorkPhase) 1f else 0.5f
    )

    val lottiePause by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.pause))
    val progressLottiePause by animateLottieCompositionAsState(
        composition = lottiePause,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
        speed = 0.6f
    )


    val lottieRepeats by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.repeats))
    val progressLottieRepeats by animateLottieCompositionAsState(
        composition = lottieRepeats,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
        speed = 0.5f
    )

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
                .padding(bottom = 16.dp)
                .offset(y = (-40).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lottieRun != null) {
                LottieAnimation(
                    composition = if (timerState.isWorkPhase) lottieRun else lottiePause,
                    progress = { progressLottieRun },
                    modifier = Modifier
                        .size(200.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isActive) {
                                    activePicker = "work"
                                    showSettingsSheet = true
                                }
                            }
                        )
                )
            }
            if (!isActive) {
                Text(
                    text = formatTime(setWorkSeconds.toInt()),
                    fontFamily = LocalFontFamily.current,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Bold,
                    color = timerTextColor,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                activePicker = "work"
                                showSettingsSheet = true
                            }
                        )
                )
                Spacer(modifier = Modifier.height(20.dp))
                if (lottiePause != null) {
                    LottieAnimation(
                        composition = lottiePause,
                        progress = { progressLottiePause },
                        modifier = Modifier
                            .size(100.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    activePicker = "rest"
                                    showSettingsSheet = true
                                }
                            )
                    )
                }
                Text(
                    text = formatTime(setRestSeconds.toInt()),
                    fontFamily = LocalFontFamily.current,
                    fontSize = 74.sp,
                    fontWeight = FontWeight.Bold,
                    color = timerTextColor,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                activePicker = "rest"
                                showSettingsSheet = true
                            }
                        )
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (lottieRepeats != null) {
                    LottieAnimation(
                        composition = lottieRepeats,
                        progress = { progressLottieRepeats },
                        modifier = Modifier
                            .size(50.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    activePicker = "repeats"
                                    showSettingsSheet = true
                                }
                            )
                    )
                }
                Text(
                    text = "${setRepeats.toInt()}",
                    fontFamily = LocalFontFamily.current,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = timerTextColor,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                activePicker = "repeats"
                                showSettingsSheet = true
                            }
                        )
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                ActiveTimerDisplay(
                    timerState = timerState,
                    smoothProgress = smoothProgress.value,
                    accentColor = accentColor,
                    errorColor = errorColor,
                    trackColor = trackColor,
                    timerTextColor = timerTextColor,
                )
            }

            val onPrimary = MaterialTheme.colorScheme.onPrimary

            if (!isActive) {
                IslandButtonRow(
                    buttons = listOf(
                        IslandButton(
                            icon = painterResource(R.drawable.play),
                            shape = ButtonShape.WIDE,
                            containerColor = accentColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ),
                    onTap = {
                        onStart(
                            setWorkSeconds.toInt(),
                            setRestSeconds.toInt(),
                            setRepeats.toInt()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            } else {
                IslandButtonRow(
                    buttons = listOf(
                        IslandButton(
                            icon = painterResource(R.drawable.stop),
                            shape = ButtonShape.CIRCLE,
                            containerColor = Color.Transparent,
                            contentColor = accentColor,
                            isOutlined = true
                        ),
                        IslandButton(
                            icon = painterResource(if (timerState.isRunning) R.drawable.pause else R.drawable.play),
                            shape = ButtonShape.WIDE,
                            containerColor = accentColor,
                            contentColor = onPrimary
                        )
                    ),
                    onTap = { index ->
                        when (index) {
                            0 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrate(context)
                            }

                            1 -> {
                                if (timerState.isRunning) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrate(
                                        context
                                    )
                                    onPause()
                                } else {
                                    onResume()
                                }
                            }
                        }
                    },
                    onLongPress = { index ->
                        if (index == 0) {
                            onStop()
                            scope.launch { smoothProgress.snapTo(1f) }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

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
            },
            dragHandle = null
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
            onDismissRequest = { showThemeSheet = false },
            containerColor = Color.White, // Цвет фона самой шторки
            scrimColor = Color.Transparent, // Цвет затемнения фона (прозрачность)
            contentColor = Color.Black, // Цвет контента по умолчанию (текст, иконки)
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 16.dp)
            ) {
                ThemeSelector(
                    activeTheme = activeTheme,
                    onThemeChange = onThemeChange
                )
                Spacer(modifier = Modifier.height(24.dp))
                FontSelector(
                    selectedFont = selectedFont,
                    onFontChange = onFontChange
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ActiveTimerDisplay(
    timerState: TimerState,
    smoothProgress: Float,
    accentColor: Color,
    errorColor: Color,
    trackColor: Color,
    timerTextColor: Color,
) {
    val phaseColor = if (timerState.isWorkPhase) accentColor else errorColor
    Spacer(modifier = Modifier.weight(0.5f))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
        CircularProgressIndicator(
            progress = { smoothProgress },
            modifier = Modifier.fillMaxSize(),
            color = phaseColor,
            trackColor = trackColor,
            strokeWidth = 16.dp,
            strokeCap = StrokeCap.Round,
            gapSize = 4.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSelector(
    activeTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppThemes.forEach { theme ->
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
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(AvailableFonts) { index, font ->
            ListItem(
                headlineContent = {
                    val isSelected = font.id == selectedFont.id
                    Text(
                        text = font.label,
                        fontFamily = font.fontFamily,
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .clip(
                        when (index) {
                            0 -> FirstLazyRowItemShape
                            AvailableFonts.size - 1 -> LastLazyRowItemShape
                            else -> middleListItemShape
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFontChange(font) }
            )
        }
    }
}

