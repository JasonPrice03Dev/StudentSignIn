package com.mad.studentsignin.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun QRCodeScanner(navController: NavHostController, course: String, currentClassId: String, viewModel: StartPageViewModel) {
    // Retrieve the current Activity context
    val context = LocalContext.current as Activity

    // Variables to manage loading and permission status
    var isLoading by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    // Launcher to request camera permission if not granted
    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted -> hasPermission = isGranted }) // Update permission status based on user's decision

    // Launcher to handle the QR code scanner result
    val qrScannerLauncher =
        rememberLauncherForActivityResult(contract = ScanContract(), onResult = { result ->
            result?.contents?.let { scannedData ->
                if (!isLoading) {
                    isLoading = true  // Loading state set as true to indicate QR code is processing

                    val scannedClassId = scannedData.substringAfter("classSessionID_").substringBefore("_")

                    if (scannedClassId == currentClassId) {
                        viewModel.isQRCodeValid(scannedData) { isValid ->
                            if (isValid) {
                                navController.navigate("facialRecognitionPage/$course/$scannedData/$currentClassId")
                            } else {
                                Log.e("QRCodeScanner", "QR Code has expired.")
                                showExpiredQRCodeMessage(navController, course)
                            }
                            isLoading = false  // Reset loading state after validation is complete
                        }
                    } else {
                        Log.e("QRCodeScanner", "Invalid QR code for this class.")
                        isLoading = false  // Reset loading state if class ID doesn't match
                    }
                }
            } ?: run {
                Log.e("QRCodeScanner", "Scan result was null")
                isLoading = false  // Reset loading state if null scan result
            }
        })


    // Launched effect to check camera permission
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)  // Request permission if not granted
        } else {
            hasPermission = true  // Set permission state to true if already granted
        }
    }

    // Launched effect to start QR scanner if permission is granted and not loading
    LaunchedEffect(hasPermission) {
        if (hasPermission && !isLoading) {
            // Configure scan options for QR scanner
            val scanOptions = ScanOptions().apply {
                setOrientationLocked(false)
                setPrompt("")
                setBeepEnabled(true)
            }
            qrScannerLauncher.launch(scanOptions)  // Launch QR scanner
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
                )
            ), contentAlignment = Alignment.Center
    ) {
        when {
            // If camera permission is not granted, show request message
            !hasPermission -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Camera access is required to scan the QR Code.",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }

            // If scanning is in progress, show loading indicator
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.Blue, modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        "Processing your sign-in...",
                        color = Color.DarkGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // UI (when permission is granted)
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Scanning",
                        tint = Color.Blue,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Scanning QR Code...",
                        color = Color.Black,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

// Function to show expired QR code message
fun showExpiredQRCodeMessage(navController: NavHostController, course: String) {
    Toast.makeText(navController.context, "QR Code has expired.", Toast.LENGTH_SHORT).show()
    // Navigate back to the QR code page to get a fresh code
    navController.navigate("hubPage/$course")
}
