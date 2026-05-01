package com.baskaeva.pipette.presentation.camera

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.baskaeva.pipette.presentation.theme.BackgroundDark
import com.baskaeva.pipette.presentation.theme.CardDark
import com.baskaeva.pipette.presentation.theme.SurfaceDark
import java.io.File
import java.nio.ByteBuffer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.res.stringResource
import com.baskaeva.pipette.R
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotoTaken: (Uri, Int) -> Unit,
    onOpenFavourites: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val colorCount by viewModel.colorCount.collectAsState()
    val pipetteColor by viewModel.pipetteColor.collectAsState()
    val currentZoomRatio by viewModel.currentZoomRatio.collectAsState()

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var showColorCountPicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onPhotoTaken(it, colorCount) } }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (cameraPermission.status.isGranted) {

            // Camera preview + pinch-to-zoom gesture
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        setupCameraWithAnalysis(
                            context = ctx,
                            lifecycleOwner = lifecycleOwner,
                            previewView = pv,
                            onCameraReady = { cam, ic ->
                                camera = cam
                                imageCapture = ic
                            },
                            onCenterColor = { rgb -> viewModel.setPipetteColor(rgb) }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomDelta, _ ->
                            val cam = camera ?: return@detectTransformGestures
                            val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                            val current = zoomState.zoomRatio
                            val min = zoomState.minZoomRatio
                            val max = zoomState.maxZoomRatio
                            val newZoom = (current * zoomDelta).coerceIn(min, max)
                            cam.cameraControl.setZoomRatio(newZoom)
                            viewModel.setZoomRatio(newZoom)
                        }
                    }
            )

            // Crosshair
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 2.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .size(width = 2.dp, height = 40.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            // Top bar — favourites
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 52.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onOpenFavourites,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Избранное",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pipette color pill
                val displayColor = pipetteColor
                val hexColor = if (displayColor != null)
                    String.format("#%06X", 0xFFFFFF and displayColor) else "—"

                Row(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (displayColor != null) Color(displayColor)
                                else Color.Gray.copy(alpha = 0.35f)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(7.dp))
                            .clickable {
                                if (displayColor != null) copyHexToClipboard(context, hexColor)
                            }
                    )
                    Text(
                        text = hexColor,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Копировать",
                        tint = Color.White.copy(alpha = if (displayColor != null) 0.7f else 0.3f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                if (displayColor != null) copyHexToClipboard(context, hexColor)
                            }
                    )
                }

                // Bottom icons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Галерея",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable {
                                takePhoto(context, imageCapture, currentZoomRatio) { uri ->
                                    onPhotoTaken(uri, colorCount)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDark)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable { showColorCountPicker = !showColorCountPicker },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = colorCount.toString(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

        } else {
            val deniedPermanently = !cameraPermission.status.shouldShowRationale

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (deniedPermanently)
                        stringResource(R.string.cam_not_permitted)
                    else
                        stringResource(R.string.camera_permission),
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (deniedPermanently) {
                    Button(onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.go_to_settings))
                    }
                } else {
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                        Text(stringResource(R.string.give_permission))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.go_to_gallery))
                }
            }
        }

        if (showColorCountPicker) {
            ColorCountPicker(
                currentCount = colorCount,
                options = viewModel.colorCountOptions,
                onSelect = { count ->
                    viewModel.setColorCount(count)
                    showColorCountPicker = false
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 110.dp)
                    .width(IntrinsicSize.Max)
            )
        }
    }
}

// Color count picker

@Composable
fun ColorCountPicker(
    currentCount: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.colour_count),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        options.forEach { count ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (count == currentCount) Color.White.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(count) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = if (count == currentCount) Color.White else Color.White.copy(alpha = 0.7f),
                    fontWeight = if (count == currentCount) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp
                )
            }
        }
    }
}
// Camera setup

private fun setupCameraWithAnalysis(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onCameraReady: (Camera, ImageCapture) -> Unit,
    onCenterColor: (Int) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(320, 240))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        var lastUpdateMs = 0L
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val now = System.currentTimeMillis()
            if (now - lastUpdateMs >= 500L) {
                val rgb = extractCenterPixelRgb(imageProxy)
                if (rgb != null) {
                    onCenterColor(rgb)
                    lastUpdateMs = now
                }
            }
            imageProxy.close()
        }

        try {
            cameraProvider.unbindAll()
            val cam = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                imageAnalysis
            )
            onCameraReady(cam, imageCapture)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun extractCenterPixelRgb(imageProxy: ImageProxy): Int? {
    return try {
        val plane = imageProxy.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val cx = imageProxy.width / 2
        val cy = imageProxy.height / 2
        val offset = cy * plane.rowStride + cx * plane.pixelStride
        if (offset + 3 >= buffer.limit()) return null
        val r = buffer[offset].toInt() and 0xFF
        val g = buffer[offset + 1].toInt() and 0xFF
        val b = buffer[offset + 2].toInt() and 0xFF
        android.graphics.Color.rgb(r, g, b)
    } catch (_: Exception) {
        null
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    zoomRatio: Float,
    onSuccess: (Uri) -> Unit
) {
    imageCapture ?: return
    val photoFile = File(context.cacheDir, "pipette_photo_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val croppedUri = cropBitmapByZoom(photoFile, zoomRatio)
                onSuccess(croppedUri)
            }
            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Ошибка съёмки: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

/**
 * Вырезает центральный кроп из сохранённого файла согласно zoomRatio.
 * Сохраняет EXIF-ориентацию оригинала, чтобы изображение не переворачивалось.
 */
private fun cropBitmapByZoom(file: File, zoomRatio: Float): Uri {
    if (zoomRatio <= 1f) return Uri.fromFile(file)

    return try {
        // Читаем EXIF до декодирования
        val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )

        val original = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            ?: return Uri.fromFile(file)

        val scale = 1f / zoomRatio
        val cropW = (original.width * scale).toInt()
        val cropH = (original.height * scale).toInt()
        val left = (original.width - cropW) / 2
        val top = (original.height - cropH) / 2

        val cropped = android.graphics.Bitmap.createBitmap(original, left, top, cropW, cropH)
        original.recycle()

        // Записываем кроп
        file.outputStream().use { out ->
            cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
        }
        cropped.recycle()

        // Восстанавливаем EXIF-ориентацию
        val exifOut = androidx.exifinterface.media.ExifInterface(file.absolutePath)
        exifOut.setAttribute(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            orientation.toString()
        )
        exifOut.saveAttributes()

        Uri.fromFile(file)
    } catch (_: Exception) {
        Uri.fromFile(file)
    }
}

private fun copyHexToClipboard(context: Context, hex: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("HEX Color", hex))
}