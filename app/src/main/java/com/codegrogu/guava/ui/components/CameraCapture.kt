package com.codegrogu.guava.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.Surface as AndroidSurface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.codegrogu.guava.ui.theme.SafetyOrange
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun CameraCapture(
    onImageCaptured: (Uri) -> Unit,
    onImageCleared: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember(context) { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember(context) {
        ProcessCameraProvider.getInstance(context)
    }

    var hasCameraPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var shouldOpenSettings by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isCompressing by remember { mutableStateOf(false) }

    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        shouldOpenSettings = !granted &&
            permissionRequested &&
            activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.CAMERA
                )
            } == true
    }

    fun requestCameraPermission() {
        permissionRequested = true
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Request permission on first composition if Android can show the dialog.
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission && !permissionRequested) {
            requestCameraPermission()
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = context.hasCameraPermission()
                if (hasCameraPermission) {
                    shouldOpenSettings = false
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(capturedImageUri) {
        if (capturedImageUri != null && cameraProviderFuture.isDone) {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
            imageCaptureUseCase = null
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "CONDITION PHOTO",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            letterSpacing = 1.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )

        when {
            !hasCameraPermission -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(1.dp, SafetyOrange.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = SafetyOrange.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (shouldOpenSettings) {
                                "CAMERA ACCESS DISABLED"
                            } else {
                                "CAMERA PERMISSION REQUIRED"
                            },
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                if (shouldOpenSettings) {
                                    context.openAppSettings()
                                } else {
                                    requestCameraPermission()
                                }
                            },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(SafetyOrange)
                            )
                        ) {
                            Text(
                                if (shouldOpenSettings) "OPEN SETTINGS" else "GRANT ACCESS",
                                fontFamily = FontFamily.Monospace,
                                color = SafetyOrange,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            capturedImageUri != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, SafetyOrange, RoundedCornerShape(4.dp))
                ) {
                    AsyncImage(
                        model = capturedImageUri,
                        contentDescription = "Captured vehicle condition",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // "Captured" badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = SafetyOrange,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "CAPTURED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black
                            )
                        }
                    }

                    // Compression loading overlay
                    if (isCompressing) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = SafetyOrange)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "COMPRESSING...",
                                    fontFamily = FontFamily.Monospace,
                                    color = SafetyOrange,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Retake button
                OutlinedButton(
                    onClick = {
                        capturedImageUri = null
                        onImageCleared()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SafetyOrange)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = SafetyOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "RETAKE PHOTO",
                        fontFamily = FontFamily.Monospace,
                        color = SafetyOrange,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            1.dp,
                            SafetyOrange.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    // CameraX preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setTargetRotation(
                                        previewView.display?.rotation ?: AndroidSurface.ROTATION_0
                                    )
                                    .build()

                                imageCaptureUseCase = capture

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        capture
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Capture button overlaid at bottom center
                    IconButton(
                        onClick = {
                            val capture = imageCaptureUseCase ?: return@IconButton
                            val file = createImageFile(context)
                            val outputOptions = ImageCapture.OutputFileOptions
                                .Builder(file).build()

                            capture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(
                                        output: ImageCapture.OutputFileResults
                                    ) {
                                        scope.launch {
                                            isCompressing = true
                                            // NFR-5: Compress before storing
                                            val compressed = compressImage(context, file)
                                            isCompressing = false
                                            capturedImageUri = Uri.fromFile(compressed)
                                            onImageCaptured(Uri.fromFile(compressed))
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .size(64.dp)
                            .border(3.dp, SafetyOrange, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Capture",
                            tint = SafetyOrange,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun createImageFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        .format(System.currentTimeMillis())
    val storageDir = context.cacheDir
    return File.createTempFile("VEHICLE_${timestamp}_", ".jpg", storageDir)
}

// NFR-5: Compress the raw image to reduce Firebase Storage bandwidth
// Target: 720p, 70% quality JPEG — good enough for condition records
private suspend fun compressImage(context: Context, file: File): File {
    return Compressor.compress(context, file) {
        resolution(1280, 720)
        quality(70)
        format(android.graphics.Bitmap.CompressFormat.JPEG)
    }
}

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    startActivity(intent)
}
