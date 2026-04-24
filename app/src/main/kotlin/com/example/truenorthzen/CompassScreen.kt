package com.example.truenorthzen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen(viewModel: DailyChallengeViewModel = viewModel()) {
    val targetDegree by viewModel.targetDegree.collectAsState()
    val currentAzimuth by viewModel.currentAzimuth.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val holdProgress by viewModel.holdProgress.collectAsState()
    val completedDegrees = remember(isCompleted) { viewModel.getCompletedDegrees() }

    DisposableEffect(Unit) {
        viewModel.startCompass()
        onDispose {
            viewModel.stopCompass()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "True North Zen",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Daily Challenge",
            style = MaterialTheme.typography.h6
        )
        Text(
            text = "Target: ${targetDegree}° ${getDirection(targetDegree)}",
            style = MaterialTheme.typography.h5,
            color = if (isCompleted) Color(0xFF4CAF50) else Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CompassRose(
                currentAzimuth = currentAzimuth,
                targetDegree = targetDegree.toFloat(),
                completedDegrees = completedDegrees,
                isCompleted = isCompleted
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isCompleted) {
            Text(
                text = "Challenge Completed!",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.h6
            )
        } else {
            Text(
                text = "Align and hold for 3 seconds",
                style = MaterialTheme.typography.body1
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = holdProgress,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(8.dp),
                color = Color(0xFF2196F3)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Current Heading: ${currentAzimuth.toInt()}°",
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@Composable
fun CompassRose(
    currentAzimuth: Float,
    targetDegree: Float,
    completedDegrees: Set<Int>,
    isCompleted: Boolean
) {
    var previousAzimuth by remember { mutableStateOf(currentAzimuth) }
    var rotationOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(currentAzimuth) {
        val diff = currentAzimuth - previousAzimuth
        val shortestDiff = when {
            diff > 180f -> diff - 360f
            diff < -180f -> diff + 360f
            else -> diff
        }
        rotationOffset += shortestDiff
        previousAzimuth = currentAzimuth
    }

    val animatedRotation by animateFloatAsState(targetValue = rotationOffset)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Draw outer ring
        drawCircle(
            color = Color.LightGray,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        // Draw completed coordinates (the "filled" pieces of the rose)
        completedDegrees.forEach { degree ->
            val angleRad = Math.toRadians(degree.toDouble() - 90).toFloat()
            val lineEnd = Offset(
                center.x + radius * cos(angleRad),
                center.y + radius * sin(angleRad)
            )
            drawLine(
                color = Color(0xFFFFC107).copy(alpha = 0.6f),
                start = center,
                end = lineEnd,
                strokeWidth = 6.dp.toPx()
            )
        }

        // Rotate the world relative to current heading
        rotate(-animatedRotation, center) {
            // Draw cardinal marks
            val cardinalPoints = listOf(0f, 90f, 180f, 270f)
            cardinalPoints.forEach { degree ->
                val angleRad = Math.toRadians(degree.toDouble() - 90).toFloat()
                drawLine(
                    color = Color.Black,
                    start = Offset(
                        center.x + (radius - 15.dp.toPx()) * cos(angleRad),
                        center.y + (radius - 15.dp.toPx()) * sin(angleRad)
                    ),
                    end = Offset(
                        center.x + radius * cos(angleRad),
                        center.y + radius * sin(angleRad)
                    ),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Draw target marker
            if (!isCompleted) {
                val targetRad = Math.toRadians(targetDegree.toDouble() - 90).toFloat()
                val targetPos = Offset(
                    center.x + radius * cos(targetRad),
                    center.y + radius * sin(targetRad)
                )
                drawCircle(
                    color = Color.Red,
                    radius = 10.dp.toPx(),
                    center = targetPos
                )
                
                // Draw a line to the target
                drawLine(
                    color = Color.Red.copy(alpha = 0.3f),
                    start = center,
                    end = targetPos,
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Draw current heading indicator (fixed at top)
        val needlePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x - 10.dp.toPx(), center.y - radius + 20.dp.toPx())
            lineTo(center.x + 10.dp.toPx(), center.y - radius + 20.dp.toPx())
            close()
        }
        drawPath(
            path = needlePath,
            color = Color.Blue
        )
    }
}

fun getDirection(degree: Int): String {
    return when (degree) {
        in 0..22 -> "N"
        in 23..67 -> "NE"
        in 68..112 -> "E"
        in 113..157 -> "SE"
        in 158..202 -> "S"
        in 203..247 -> "SW"
        in 248..292 -> "W"
        in 293..337 -> "NW"
        else -> "N"
    }
}
