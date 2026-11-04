package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MitmatCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    pastelColorLight: Color = WarmCreamCard,
    pastelColorDark: Color = AshCard,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val isAmoled = LocalIsAmoled.current

    val cardColor = when {
        isAmoled -> if (pastelColorDark == AshCard) AmoledCard else pastelColorDark.copy(alpha = 0.85f)
        isDark -> pastelColorDark
        else -> pastelColorLight
    }

    val cardBorder = when {
        isAmoled -> BorderStroke(1.dp, AmoledDivider)
        isDark -> BorderStroke(1.dp, AshDivider.copy(alpha = 0.6f))
        else -> BorderStroke(1.dp, PastelLavenderBorder.copy(alpha = 0.35f))
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = cardBorder
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = cardBorder
        ) {
            content()
        }
    }
}

/**
 * Renders the screen background: cream in light mode, ash in dark mode, or pure black in AMOLED mode.
 */
@Composable
fun MitmatScreenScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val isAmoled = LocalIsAmoled.current

    val bgColor = when {
        isAmoled -> AmoledBackground
        isDark -> AshBackground
        else -> WarmCreamBackground
    }

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        content()
    }
}
