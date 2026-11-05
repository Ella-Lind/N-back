package com.example.nback.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nback.db.Score
import com.example.nback.db.ScoreSortType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScoresScreen(
    scores: List<Score>,
    currentSortType: ScoreSortType,
    onSortTypeSelected: (ScoreSortType) -> Unit,
    onBackToGame: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Leaderboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBackToGame) {
                Text("Play Game")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sort Options
        Text(text = "Sort By:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = currentSortType == ScoreSortType.DATE_DESC,
                onClick = { onSortTypeSelected(ScoreSortType.DATE_DESC) },
                label = { Text("Date") }
            )
            FilterChip(
                selected = currentSortType == ScoreSortType.SCORE_DESC,
                onClick = { onSortTypeSelected(ScoreSortType.SCORE_DESC) },
                label = { Text("Score") }
            )
            FilterChip(
                selected = currentSortType == ScoreSortType.NAME_ASC,
                onClick = { onSortTypeSelected(ScoreSortType.NAME_ASC) },
                label = { Text("Name") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scores List
        if (scores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved scores yet. Play a round and save your score!",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(scores) { score ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = score.playerName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "N = ${score.nValue} | ${score.gridSize}x${score.gridSize} Grid",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormat.format(Date(score.timestamp)),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${score.score} / ${score.totalEvents}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Hits",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
