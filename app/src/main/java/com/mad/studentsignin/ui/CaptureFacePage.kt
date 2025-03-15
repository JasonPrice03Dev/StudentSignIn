package com.mad.studentsignin.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureFacePage(navController: NavHostController, viewModel: StartPageViewModel = viewModel()) {
    // Obtain the current context
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var faceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Manage camera permissions
    val permissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Request camera permission when the composable is launched
    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    // Once finished unbind camera
    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }

    if (permissionState.status.isGranted) {
        // If permission is granted, set up camera
        LaunchedEffect(Unit) {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                // Image analysis for face detection
                val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build()

                // Analyze images to detect faces
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processImageProxy(imageProxy) { bitmap ->
                        faceBitmap = bitmap
                    }
                }

                try {
                    // Bind the camera to the lifecycle
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        context as androidx.lifecycle.LifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CaptureFacePage", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
            }

            if (isLoading) {
                Text(
                    text = "Capturing face...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            faceBitmap?.let {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            isLoading = true
                            viewModel.storeFaceData(it)

                            // Save the captured face image to Firebase
                            val userId =
                                FirebaseAuth.getInstance().currentUser?.uid ?: return@Button
                            uploadImageToFirebaseStorage(it, userId) { success ->
                                isLoading = false
                                if (success) {
                                    navController.popBackStack()
                                    Toast.makeText(
                                        context, "Face data saved successfully", Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context, "Failed to save face data", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(16.dp)
                            .then(if (isLoading) Modifier.alpha(0.5f) else Modifier)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Face,
                            contentDescription = "Capture Icon",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = if (isLoading) "Processing..." else "Capture Face")
                    }
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    } else {
        // Show message if permission is denied
        Text(
            text = "Please grant camera permission to use this feature.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
    }
}

// Rotates a bitmap if required based on the images (ImageProxy) rotation
private fun rotateBitmapIfNeeded(bitmap: Bitmap, imageProxy: ImageProxy): Bitmap {
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

// Processes an image from the camera and detects faces
@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy, onFaceDetected: (Bitmap) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val faceDetector = com.google.mlkit.vision.face.FaceDetection.getClient()
        faceDetector.process(image).addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces.first()
                var bitmap = imageProxy.toBitmap()
                bitmap = rotateBitmapIfNeeded(bitmap, imageProxy)
                onFaceDetected(bitmap)
            }
        }.addOnFailureListener { e ->
            Log.e("CaptureFacePage", "Face detection failed", e)
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }
}

// Uploads the face image to Firebase Storage and stores the URL in Firestore
private fun uploadImageToFirebaseStorage(
    bitmap: Bitmap, userId: String, onComplete: (Boolean) -> Unit
) {
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference.child("faces/${UUID.randomUUID()}.jpg")
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    val data = baos.toByteArray()

    val uploadTask = storageRef.putBytes(data)
    uploadTask.addOnSuccessListener {
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            val imageUrl = uri.toString()
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(userId)
            userRef.update("faceImageUrl", imageUrl).addOnSuccessListener {
                onComplete(true)
            }.addOnFailureListener {
                onComplete(false)
            }
        }
    }.addOnFailureListener {
        onComplete(false)
    }
}