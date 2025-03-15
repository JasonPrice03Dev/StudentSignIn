package com.mad.studentsignin.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HubPage(navController: NavHostController, viewModel: StartPageViewModel, course: String) {
    // Observing timetable entries and signed-in classes
    val timetableEntries by viewModel.timetableEntries.observeAsState(emptyList())
    val signedInClasses = remember { mutableStateOf(setOf<String>()) }

    // Variables to manage the day of week dropdown menu
    var expanded by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf("Monday") }

    // List of days for the dropdown menu
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    // Load timetable entries and signed-in classes for the selected course
    LaunchedEffect(course) {
        viewModel.loadTimetableEntries(course)
    }

    LaunchedEffect(course) {
        viewModel.getUserSignedInClasses(course) { signedInClassIds ->
            signedInClasses.value = signedInClassIds
        }
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
                        TimetableItem(
                            entry, navController, entry.classId, course, signedInClasses.value
                        )
                        Divider(
                            color = Color.LightGray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    navController.navigate("captureFacePage")
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(Color.Black)
            ) {
                Text(
                    text = "Capture Face",
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    }
}

// Displays timetable entries subject, time, and sign-in button
@SuppressLint("SuspiciousIndentation")
@Composable
fun TimetableItem(
    entry: StartPageViewModel.TimetableEntry,
    navController: NavHostController,
    classId: String,
    course: String,
    signedInClasses: Set<String>
) {
    val isSignedIn = signedInClasses.contains(classId)

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
        // Display class time range
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
                if (!isSignedIn) {
                    navController.navigate("qrCodeScanner/$course/$classId")
                }
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(4.dp, shape = MaterialTheme.shapes.large),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSignedIn) Color.Green else Color.Black,
                contentColor = Color.White
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignedIn) "Signed In" else "Sign In",
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isSignedIn) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
