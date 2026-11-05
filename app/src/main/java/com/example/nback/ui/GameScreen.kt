package com.example.nback.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.nback.viewmodel.AppScreen
import com.example.nback.viewmodel.GameStatus
import com.example.nback.viewmodel.NBackViewModel

@Composable
fun NBackGameApp(viewModel: NBackViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initTtsAndDb(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.cancelGame()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (state.showSaveScoreDialog) {
        var playerName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveScoreDialog() },
            title = { Text(text = "Round Complete!", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Score: ${state.correctHits} / ${state.totalEvents} (N = ${state.nValue})")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Enter Player Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveScore(playerName) },
                    enabled = playerName.trim().isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSaveScoreDialog() }) {
                    Text("Skip")
                }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(text = "Game Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // N Value setting
                    Text(text = "N Value", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.decreaseN() },
                            enabled = state.nValue > 1
                        ) {
                            Text("-")
                        }
                        Text(text = "N = ${state.nValue}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { viewModel.increaseN() }
                        ) {
                            Text("+")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid Size setting
                    Text(text = "Grid Size", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(3, 4, 5).forEach { size ->
                            FilterChip(
                                selected = state.gridSize == size,
                                onClick = { viewModel.setGridSize(size) },
                                label = { Text("${size}x${size}") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Events Per Round setting
                    Text(text = "Events per Round", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.decreaseTotalEvents() },
                            enabled = state.totalEvents - 5 > state.nValue
                        ) {
                            Text("-5")
                        }
                        Text(text = "${state.totalEvents}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { viewModel.increaseTotalEvents() }
                        ) {
                            Text("+5")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Distinct Spoken Letters setting
                    Text(text = "Spoken Letters Count", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.decreaseLetterCount() },
                            enabled = state.letterCount > 3
                        ) {
                            Text("-")
                        }
                        Text(text = "${state.letterCount} letters", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { viewModel.increaseLetterCount() },
                            enabled = state.letterCount < 13
                        ) {
                            Text("+")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (state.currentScreen == AppScreen.SCORES) {
        ScoresScreen(
            scores = state.scores,
            currentSortType = state.sortType,
            onSortTypeSelected = { viewModel.setSortType(it) },
            onBackToGame = { viewModel.setScreen(AppScreen.GAME) }
        )
    } else {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // HUD Header with Three Lines (Menu) Icon Settings Tab & Leaderboard Button
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.cancelGame()
                            showSettings = true
                        }) {
                            val iconColor = MaterialTheme.colorScheme.onSurface
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val strokeWidth = 2.5.dp.toPx()
                                val startX = 2.dp.toPx()
                                val endX = size.width - 2.dp.toPx()
                                val yGap = size.height / 4

                                for (i in 1..3) {
                                    val y = yGap * i
                                    drawLine(
                                        color = iconColor,
                                        start = Offset(startX, y),
                                        end = Offset(endX, y),
                                        strokeWidth = strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                        Text(text = "N = ${state.nValue}", fontSize = 28.sp, style = MaterialTheme.typography.headlineLarge)
                        TextButton(onClick = { viewModel.setScreen(AppScreen.SCORES) }) {
                            Text("Scores")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.status == GameStatus.RUNNING)
                            "Event: ${state.currentIndex + 1} / ${state.totalEvents}  |  Score: ${state.correctHits}"
                        else "Press Start to Begin",
                        fontSize = 16.sp
                    )
                }

                // Dynamic Stimulus Grid (3x3, 4x4, 5x5)
                val gridSize = state.gridSize
                val cellSize = when (gridSize) {
                    3 -> 80.dp
                    4 -> 60.dp
                    5 -> 48.dp
                    else -> 80.dp
                }

                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .aspectRatio(1f)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                        for (row in 0 until gridSize) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                for (col in 0 until gridSize) {
                                    val cellIndex = row * gridSize + col
                                    val isHighlighted = state.currentStimulus?.position == cellIndex

                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
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
                    AnimatedVisibility(
                        visible = state.isWrongGuessFeedbackActive,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "Incorrect",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

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
}