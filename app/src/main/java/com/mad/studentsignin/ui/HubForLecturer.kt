package com.mad.studentsignin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun HubForLecturer(navController: NavHostController, viewModel: StartPageViewModel) {
    // Observe timetable entries from the ViewModel
    val timetableEntries by viewModel.timetableEntries.observeAsState(emptyList())

    // Variables for dropdown menu and selected day
    var expanded by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf("Monday") }

    // List of days displayed in the dropdown menu
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    // Load timetable entries for the lecturer on launch of page
    LaunchedEffect(Unit) {
        val lecturerId = viewModel.auth.currentUser?.uid ?: return@LaunchedEffect
        viewModel.loadLecturerTimetableEntries(lecturerId)
    }

    // Filter timetable entries based on the selected day
    val filteredEntries = timetableEntries.filter { it.day == selectedDay }

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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .background(Color(0xFFE0E0E0))
                            .padding(vertical = 12.dp, horizontal = 16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedDay, fontSize = 16.sp, color = Color.Black
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            days.forEach { day ->
                                DropdownMenuItem(text = { Text(text = day) }, onClick = {
                                    selectedDay = day
                                    expanded = false
                                })
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    items(filteredEntries) { entry ->
                        TimetableItemLecturer(entry = entry,
                            navController = navController,
                            onGenerateQRCodeClick = {
                                viewModel.generateQRCodeForClass(entry.classId, navController)
                            })
                        Divider(
                            color = Color.LightGray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableItemLecturer(
    entry: StartPageViewModel.TimetableEntry,
    navController: NavHostController,
    onGenerateQRCodeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color(0xFFEEEEEE))
            .padding(16.dp)
    ) {
        Text(
            text = entry.subject,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "${entry.startTime} - ${entry.endTime}",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = entry.day, fontSize = 12.sp, color = Color(0xFF888888)
        )

        Button(
            onClick = {
                navController.navigate("attendanceViewingPage/${entry.id}/${entry.course}/${entry.classId}")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black, contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(4.dp, shape = MaterialTheme.shapes.large)
        ) {
            Text(text = "View attendance")
        }

        Button(
            onClick = onGenerateQRCodeClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black, contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(4.dp, shape = MaterialTheme.shapes.large)
        ) {
            Text(text = "Generate QR Code")
        }
    }
}
