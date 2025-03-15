package com.mad.studentsignin.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceViewingPage(
    navController: NavHostController, course: String, classId: String, viewModel: StartPageViewModel
) {
    // Observing data from the viewModel
    val students by viewModel.students.observeAsState(emptyList())
    val attendanceRecords by viewModel.attendanceRecords.observeAsState(emptyList())

    // Variables for managing UI state
    var showDialog by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Effect to load attendance data
    LaunchedEffect(Unit) {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModel.loadStudents(moduleName = course) // Loading students for specific course
        viewModel.loadAttendance(
            classId = classId, courseName = course, todaysDate = todaysDate
        )
        isLoading = false
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

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Color(0xFF4CAF50)
                    )
                } else {
                    if (students.isEmpty()) {
                        Text(
                            text = "No students found.",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFD6D6D6))
                                .padding(vertical = 8.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Student Email",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "Attendance",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }

                        // List of students with attendance status
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(students) { student ->
                                val isPresent =
                                    attendanceRecords.any { it.userId == student.UID } // Check if student is present
                                val backgroundColor =
                                    if (students.indexOf(student) % 2 == 0) Color(0xFFF8F8F8) else Color.White

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor)
                                        .padding(vertical = 8.dp, horizontal = 16.dp)
                                        .clickable {
                                            Log.d(
                                                "StatisticsPage",
                                                "Student UID: ${student.UID}, Email: ${student.email}, ClassId: ${classId}\""
                                            )
                                            // Navigating to the statistics page for a selected student
                                            navController.navigate("statisticsPage/${student.UID}/${student.email}/${course}/${classId}")
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = student.email,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = if (isPresent) "Present" else "Absent",
                                        color = if (isPresent) Color(0xFF4CAF50) else Color(
                                            0xFFF44336
                                        ), // Green if present, red if absent
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedStudent = student.UID
                                                showDialog = true
                                            },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Confirmation Dialog for Attendance Alteration
            if (showDialog) {
                AlertDialog(onDismissRequest = { showDialog = false },
                    title = { Text(text = "Alter Attendance Status") },
                    text = { Text(text = "Do you want to alter attendance status?") },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedStudent?.let { studentId ->
                                val isPresent = attendanceRecords.any { it.userId == studentId }
                                if (isPresent) {
                                    // Remove attendance if present
                                    viewModel.removeAttendanceForDay(userId = studentId,
                                        classId = classId,
                                        timetableEntryId = course,
                                        specificDate = Date(),
                                        onSuccess = {
                                            Log.d(
                                                "AttendanceRemoval",
                                                "Attendance successfully removed for student: $studentId"
                                            )
                                        },
                                        onFailure = { errorMessage ->
                                            Log.e("AttendanceRemoval", errorMessage)
                                        })
                                } else {
                                    // Mark attendance if absent
                                    viewModel.addAttendance(
                                        studentId, classId, course, Date()
                                    )
                                }
                            }
                            showDialog = false
                        }) {
                            Text("Yes", color = Color(0xFF4CAF50))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("No", color = Color(0xFFF44336))
                        }
                    })
            }
        }
    }
}