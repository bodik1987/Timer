package com.bodik.timer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class ButtonShape { WIDE, CIRCLE, NARROW }

data class IslandButton(
    val icon: String,
    val shape: ButtonShape = ButtonShape.WIDE
)

@Composable
fun IslandButtonRow(
    buttons: List<IslandButton>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val n = buttons.size
    val weights = remember(n) { List(n) { Animatable(1f) } }
    val radii = remember(n) { List(n) { Animatable(50f) } }
    val interactionSources = remember(n) { List(n) { MutableInteractionSource() } }

    fun onPress(index: Int): suspend PressGestureScope.(Offset) -> Unit = { offset ->
        val pressInteraction = PressInteraction.Press(offset)
        val growJob = scope.launch {
            launch { interactionSources[index].emit(pressInteraction) }
            launch { weights[index].animateTo(1.1f, tween(100, easing = FastOutSlowInEasing)) }
            launch { radii[index].animateTo(12f, tween(100, easing = FastOutSlowInEasing)) }
            repeat(n) { i ->
                if (i != index)
                    launch { weights[i].animateTo(0.95f, tween(100, easing = FastOutSlowInEasing)) }
            }
        }
        awaitRelease()
        growJob.cancel()
        scope.launch {
            launch { interactionSources[index].emit(PressInteraction.Release(pressInteraction)) }
            repeat(n) { i ->
                launch { weights[i].animateTo(1f, springSpec) }
                launch { radii[i].animateTo(50f, springSpec) }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        buttons.forEachIndexed { index, button ->
            val shape = RoundedCornerShape(radii[index].value.dp)
            val bgColor = if (index == 0) colors.surfaceVariant else colors.secondaryContainer
            val weightMultiplier = when (button.shape) {
                ButtonShape.WIDE -> 1.0f
                ButtonShape.NARROW -> 0.5f
                ButtonShape.CIRCLE -> 0.35f
            }
            Box(
                modifier = Modifier
                    .weight(weights[index].value * weightMultiplier)
                    .height(82.dp)
                    .clip(shape)
                    .indication(interactionSources[index], ripple())
                    .background(bgColor)
                    .pointerInput(Unit) { detectTapGestures(onPress = onPress(index)) },
                contentAlignment = Alignment.Center
            ) {
                Text(button.icon, fontSize = 24.sp, color = colors.onSecondaryContainer)
            }
        }
    }
}