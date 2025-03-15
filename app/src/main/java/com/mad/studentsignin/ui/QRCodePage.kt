package com.mad.studentsignin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController

@Composable
fun QRCodePage(uniqueIdentifier: String, viewModel: StartPageViewModel, navController: NavHostController) {
    // Generate the QR code bitmap using the unique identifier
    val qrCodeBitmap = remember { mutableStateOf(viewModel.generateQRCode(uniqueIdentifier)) }
    val isRegenerating = remember { mutableStateOf(viewModel.isQRCodeRegenerating()) }

    // Start QR code regeneration when the page opens
    LaunchedEffect(Unit) {
        if (!isRegenerating.value) {
            viewModel.startQRCodeRegenerationTimer(uniqueIdentifier, navController)
            isRegenerating.value = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Scan this QR Code to Sign In",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Image to display the generated QR code
            Image(
                bitmap = qrCodeBitmap.value.asImageBitmap(),  // Convert QR code bitmap to ImageBitmap
                contentDescription = "QR Code",
                modifier = Modifier
                    .size(300.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isRegenerating.value) {
                        viewModel.stopQRCodeRegenerationTimer()
                    } else {
                        viewModel.startQRCodeRegenerationTimer(uniqueIdentifier, navController)
                    }
                    isRegenerating.value = !isRegenerating.value
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) {
                Text(text = if (isRegenerating.value) "Stop QR Code Regeneration" else "Start QR Code Regeneration")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.stopQRCodeRegenerationTimer()
                    navController.popBackStack("hubForLecturer", inclusive = false)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Back to Hub")
            }
        }
    }
}
