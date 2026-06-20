package com.example.pulse.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.warmCardStyle(
    elevation: Dp = 4.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = RoundedCornerShape(20.dp),
        ambientColor = Color(0xFF1C1917).copy(alpha = 0.04f),
        spotColor = Color(0xFF1C1917).copy(alpha = 0.08f)
    )
    .background(
        color = CardBackground,
        shape = RoundedCornerShape(20.dp)
    )
    .padding(16.dp)
