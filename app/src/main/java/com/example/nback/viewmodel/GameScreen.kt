package com.example.nback.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nback.viewmodel.GameStatus
import com.example.nback.viewmodel.NBackViewModel

@Composable
fun NBackGameApp(viewModel: NBackViewModel) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initTts(context)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HUD Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "N = ${state.nValue}", fontSize = 28.sp, style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.status == GameStatus.RUNNING)
                        "Event: ${state.currentIndex + 1} / ${state.totalEvents}  |  Score: ${state.correctHits}"
                    else "Press Start to Begin",
                    fontSize = 16.sp
                )
            }

            // 3x3 Stimulus Grid
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .aspectRatio(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                    for (row in 0..2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (col in 0..2) {
                                val cellIndex = row * 3 + col
                                val isHighlighted = state.currentStimulus?.position == cellIndex

                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            color = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Animated Match Button with feedback on incorrect press
            val buttonColor by animateColorAsState(
                targetValue = if (state.isWrongGuessFeedbackActive) Color.Red else MaterialTheme.colorScheme.primary,
                animationSpec = tween(durationMillis = 200), label = "FeedbackAnim"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.onMatchButtonPressed() },
                    enabled = state.status == GameStatus.RUNNING && state.currentIndex >= state.nValue && !state.hasPressedMatchThisTick,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp) // Meets accessibility touch-target minimums
                ) {
                    Text(text = "MATCH", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.startGame() },
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(text = if (state.status == GameStatus.RUNNING) "Restart" else "Start Round")
                }
            }
        }
    }
}