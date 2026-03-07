package com.ml.android.sample

import android.app.Activity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ml.android.scanner.DocumentScannerAnalyzer
import com.ml.android.scanner.utils.DocumentScannerOverlay
import com.ml.android.ui.CaptureButton
import com.ml.android.ui.ImageSheet
import com.ml.android.ui.KnobController
import com.ml.android.ui.KnobProperties

/**
 * Refactored scanner screen using the new library API.
 * This demonstrates how to integrate the document scanner library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: ScannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ScannerScaffold(
        modifier = modifier,
        uiState = uiState,
        event = viewModel::event
    ) { paddingValues ->
        // Scanner content with camera preview and overlay
        ScannerContent(
            modifier = Modifier.padding(paddingValues = paddingValues),
            uiState = uiState,
            onEvent = viewModel::event
        )
    }

    // Image sheet (modal overlay)
    ImageSheet(
        imgList = uiState.imageList,
        isSheetVisible = uiState.shouldOpenSheet,
        hideSheet = {
            viewModel.event(ScannerUiEvent.CloseImageSheet)
        }
    )
}


@Composable
fun ScannerScaffold(
    uiState: ScannerUiState,
    event: (ScannerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.Black,
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(
                uiState = uiState,
                onEvent = event
            )
        },
        bottomBar = {
            BottomBar(
                uiState = uiState,
                onEvent = event
            )
        }
    ) { paddingValues ->
        // Scanner content with camera preview and overlay
        content(paddingValues)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    uiState: ScannerUiState,
    onEvent: (ScannerUiEvent) -> Unit
) = with(uiState) {

    val ctx = LocalContext.current

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            /*IconButton(
                onClick = {
                    (ctx as Activity)
                    ctx.finish()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }*/
        },
        title = {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)) {
                        append("Doc Edge Detection")
                    }
                    append("\n- Deepak Tiwari")
                },
                fontWeight = FontWeight.Normal,
                lineHeight = 1.5.em,
                fontSize = 8.sp
            )
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(84.dp)
            ) {
                Text(
                    text = if (autoCaptureEnabled) "Auto" else "Manual",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                )

                Switch(
                    checked = autoCaptureEnabled,
                    onCheckedChange = { enabled ->
                        onEvent(ScannerUiEvent.ToggleAutoCapture(enabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.Green,
                        uncheckedBorderColor = Color.Transparent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray
                    ),
                )


                IconButton(
                    onClick = {
                        onEvent(ScannerUiEvent.ToggleFlash)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = settingBarColor,
            titleContentColor = Color.White
        )
    )
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    uiState: ScannerUiState,
    onEvent: (ScannerUiEvent) -> Unit
) = with(uiState) {

    val ctx = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(settingBarColor)
            .navigationBarsPadding()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Captured image preview at bottom center start (left side)
            AsyncImage(
                model = savedBitmap,
                contentDescription = null,
                modifier = Modifier
                    .clickable {
                        onEvent(ScannerUiEvent.LoadImageList(ctx))
                    }
                    .size(width = 100.dp, height = 120.dp)
                    .background(color = Color.Gray.copy(alpha = 0.5f))
            )

            // Manual capture button centered in bottom bar
            // Only shown when auto-capture is disabled
            if (!scannerConfig.autoCapture) {
                CaptureButton(
                    enabled = detectedDocument != null,
                    onClick = {
                        onEvent(ScannerUiEvent.TriggerManualCapture)
                    }
                )
            } else {
                // Spacer to maintain center alignment when button is hidden
                Box(
                    modifier = Modifier
                        .size(72.dp)
                )
            }

            Box(
                modifier = Modifier.size(width = 100.dp, height = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                KnobController(
                    range = IntRange(0, 20),
                    properties = KnobProperties.Default().copy(lineColor = Color.Yellow),
                    modifier = Modifier.size(20.dp, 100.dp),
                ) {
                    onEvent(ScannerUiEvent.OnCameraZoom(it))
                }
            }
        }
    }
}

/**
 * Scanner content composable containing the camera preview and document overlay.
 */
@Composable
fun ScannerContent(
    modifier: Modifier = Modifier,
    uiState: ScannerUiState,
    onEvent: (ScannerUiEvent) -> Unit
) = with(uiState) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Camera preview with overlay using smart wrapper
    DocumentScannerOverlay(
        modifier = modifier,
        detectedDocument = detectedDocument,
        config = scannerConfig,
        camera = {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        /*setRenderEffect(
                            android.graphics.RenderEffect.createBlurEffect(
                                26f, 26f,
                                android.graphics.Shader.TileMode.CLAMP
                            )
                        )*/
                        scaleType = PreviewView.ScaleType.FIT_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val executor = ContextCompat.getMainExecutor(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        // Preview
                        val preview = androidx.camera.core.Preview.Builder()
                            .build()
                            .also { it.surfaceProvider = previewView.surfaceProvider }

                        // Image Analyzer with new library
                        val analyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val documentAnalyzer = DocumentScannerAnalyzer(
                            config = scannerConfig,
                            onDocumentDetected = { doc, _ ->
                                onEvent(ScannerUiEvent.OnDocumentDetected(doc))
                            },
                            onDocumentCaptured = { bitmap ->
                                onEvent(ScannerUiEvent.OnDocumentCaptured(bitmap, context))
                            }
                        )

                        // Set analyzer and update state reference
                        analyzer.setAnalyzer(executor, documentAnalyzer)
                        onEvent(ScannerUiEvent.OnAnalyzerReady(documentAnalyzer))

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                analyzer
                            )
                            onEvent(ScannerUiEvent.OnCameraReady(camera))
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, executor)

                    previewView
                },
            )
        }
    )
}


@Preview(showSystemUi = true, device = "spec:parent=pixel_5,navigation=buttons")
@Composable
fun ScannerScreenPreview() {
    ScannerScaffold(
        modifier = Modifier.fillMaxSize(),
        uiState = ScannerUiState(),
        event = { }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .aspectRatio(3f / 4f)
                    .background(Color.Red)
            )
        }
    }
}