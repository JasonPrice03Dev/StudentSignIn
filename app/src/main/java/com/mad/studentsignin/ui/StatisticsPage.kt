package com.mad.studentsignin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatisticsPage(
    studentId: String,
    email: String,
    course: String,
    moduleName: String,
    viewModel: StartPageViewModel
) {
    // Obtaining attendance statistics of students through viewModel
    val attendanceStats by viewModel.attendanceStats.observeAsState(emptyList())

    // On launch retrieving the calculated attendance score of a student for a module
    LaunchedEffect(studentId) {
        viewModel.calculateAttendance(studentId, moduleName, course)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF6F6EE), Color(0xFFF6F6EE))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(vertical = 36.dp)
                ) {
                    Text(
                        text = "StudentSignIn ✔",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Attendance Statistics for $email",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                if (attendanceStats.isEmpty()) {
                    Text(
                        text = "No attendance data available.",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                } else {
                    // Displaying statistics for the subject
                    attendanceStats.forEach { stat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Text(
                                text = stat.subject,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Circular progress indicator to show attendance as a percentage
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = stat.percentage / 100f, // Convert percentage to fraction
                                    color = when {
                                        stat.percentage >= 75 -> Color(0xFF4CAF50)
                                        stat.percentage >= 50 -> Color(0xFFFFC107)
                                        else -> Color(0xFFF44336)
                                    }, modifier = Modifier.size(100.dp), strokeWidth = 8.dp
                                )
                                // Display the attendance as a percentage inside the circle
                                Text(
                                    text = "${stat.percentage.toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = when {
                                    stat.percentage >= 75 -> "High Attendance"
                                    stat.percentage >= 50 -> "Needs Improvement"
                                    else -> "Poor Attendance"
                                }, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = when {
                                    stat.percentage >= 75 -> Color(0xFF4CAF50)
                                    stat.percentage >= 50 -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                }, textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
