package com.mad.studentsignin.ui;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.util.Size;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.getValue;
import androidx.compose.runtime.mutableStateOf;
import androidx.compose.runtime.remember;
import androidx.compose.runtime.setValue;
import androidx.compose.ui.platform.LocalContext;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.compose.foundation.background;
import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.unit.dp;
import androidx.compose.ui.viewinterop.AndroidView;
import androidx.compose.ui.graphics.Color;
import androidx.compose.ui.platform.LocalLifecycleOwner;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.unit.sp;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavHostController;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

@Composable
fun FacialRecognitionPage(
    navController: NavHostController,
    viewModel: StartPageViewModel,
    course: String,
    scannedData: String,
    classId: String
) {
    // Local context and camera provider setup
    val context = LocalContext.current;
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) };
    val previewView = remember { PreviewView(context) };
    val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current);

    // Variables required for facial recognition
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) };
    var isFaceDetected by remember { mutableStateOf(false) };
    var showError by remember { mutableStateOf(false) };

    // Initializing and binding camera
    DisposableEffect(cameraProviderFuture) {
        try {
            val cameraProvider = cameraProviderFuture.get();
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider);
            };
            imageCapture = ImageCapture.Builder().build();

            // Configure image analysis for face detection
            val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build();

            // Process the image and detect faces
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                processImageProxy(imageProxy, viewModel, navController, course, classId) { result ->
                    val (isFaceDetectedResult, showErrorResult) = result;
                    isFaceDetected = isFaceDetectedResult;
                    showError = showErrorResult;
                };
            };

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
            );
        } catch (e: Exception) {
            Log.e("FacialRecognitionPage", "Camera initialization failed", e);
        }

        onDispose {
            // Unbind camera resources after use
            cameraProviderFuture.get().unbindAll();
        };
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F6EE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Facial Recognition",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            );

            Spacer(modifier = Modifier.height(16.dp));

            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.Gray)
            );

            Spacer(modifier = Modifier.height(16.dp));

            if (isFaceDetected) {
                Text(
                    text = "Face Detected! Proceeding...",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                );
            }

            if (showError) {
                Text(
                    text = "Error: Face not detected. Please try again.",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                );
            }
        }
    }
}

// Function to process the image and detect faces
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun processImageProxy(
    imageProxy: ImageProxy,
    viewModel: StartPageViewModel,
    navController: NavHostController,
    course: String,
    classId: String,
    callback: (Pair<Boolean, Boolean>) -> Unit
) {
    val mediaImage = imageProxy.image;
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees);
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE).build();
        val faceDetector = FaceDetection.getClient(faceDetectorOptions);

        faceDetector.process(image).addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                // Convert and rotate image for further processing
                val capturedFaceBitmap = imageProxy.toBitmap();
                val rotatedBitmap = rotateBitmapIfNeeded(capturedFaceBitmap, imageProxy);

                viewModel.compareFaces(rotatedBitmap) { isMatch ->
                    if (isMatch) {
                        viewModel.markAttendance(course, classId) {
                            navController.navigate("hubPage/$course");
                        };
                        callback(Pair(true, false));
                    } else {
                        Log.e("FacialRecognition", "Face does not match");
                        callback(Pair(false, true));
                    }
                };
            } else {
                callback(Pair(false, true));
            }
        }.addOnFailureListener { e ->
            Log.e("FaceDetection", "Face detection failed", e);
            callback(Pair(false, true));
        }.addOnCompleteListener {
            // Close image proxy to release resources (camera)
            imageProxy.close();
        };
    } else {
        imageProxy.close();
    }
}

// Helper function to rotate bitmap based on image (ImageProxy)
private fun rotateBitmapIfNeeded(bitmap: Bitmap, imageProxy: ImageProxy): Bitmap {
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees;
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) };
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true);
    } else {
        bitmap;
    }
}