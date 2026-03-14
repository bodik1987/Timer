package com.bodik.timer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object ShapeDefaults {
    val topListItemShape: RoundedCornerShape
        @Composable get() =
            RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )

    val middleListItemShape: RoundedCornerShape
        @Composable get() = RoundedCornerShape(4.dp)

    val bottomListItemShape: RoundedCornerShape
        @Composable get() =
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            )

    val FirstLazyRowItemShape: RoundedCornerShape
        @Composable get() =
            RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 4.dp,
                bottomStart = 12.dp,
                bottomEnd = 4.dp
            )
    val LastLazyRowItemShape: RoundedCornerShape
        @Composable get() =
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 12.dp,
                bottomStart = 4.dp,
                bottomEnd = 12.dp
            )
}