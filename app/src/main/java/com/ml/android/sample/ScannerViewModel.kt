package com.ml.android.sample

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.Camera
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.ml.android.scanner.DocumentScannerAnalyzer
import com.ml.android.scanner.DocumentScannerConfig
import com.ml.android.scanner.models.DetectedDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * UI State for the Scanner Screen
 */
data class ScannerUiState(
    val detectedDocument: DetectedDocument? = null,
    val savedBitmap: Bitmap? = null,
    val isFlashOn: Boolean = false,
    val autoCaptureEnabled: Boolean = true,
    val shouldOpenSheet: Boolean = false,
    val settingBarColor: Color = Color.Black.copy(alpha = .5f),
    val imageList: List<File>? = null,
    val scannerConfig: DocumentScannerConfig = DocumentScannerConfig(
        autoCapture = true,
        strokeColor = android.graphics.Color.YELLOW,
        fillAlpha = 0.2f,
        detectionMode = 1,
        smoothingAlpha = 0.15f
    )
)

/**
 * UI Events for the Scanner Screen
 */
sealed class ScannerUiEvent {
    data object ToggleFlash : ScannerUiEvent()
    data class ToggleAutoCapture(val enabled: Boolean) : ScannerUiEvent()
    data object OpenImageSheet : ScannerUiEvent()
    data object CloseImageSheet : ScannerUiEvent()
    data object TriggerManualCapture : ScannerUiEvent()
    data class OnDocumentDetected(val document: DetectedDocument?) : ScannerUiEvent()
    data class OnDocumentCaptured(val bitmap: Bitmap, val context: Context) : ScannerUiEvent()
    data class OnCameraReady(val camera: Camera) : ScannerUiEvent()
    data class OnCameraZoom(val zoom: Float) : ScannerUiEvent()
    data class OnAnalyzerReady(val analyzer: DocumentScannerAnalyzer) : ScannerUiEvent()
    data class LoadImageList(val context: Context) : ScannerUiEvent()
}

/**
 * ViewModel for managing scanner screen state and events
 */
class ScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var camera: Camera? = null
    private var scannerAnalyzer: DocumentScannerAnalyzer? = null

    init {
        // Initialize scanner config based on initial auto capture state
        updateScannerConfig(_uiState.value.autoCaptureEnabled)
    }

    /**
     * Handle UI events
     */
    fun event(event: ScannerUiEvent) {
        when (event) {
            is ScannerUiEvent.ToggleFlash -> {
                toggleFlash()
            }

            is ScannerUiEvent.ToggleAutoCapture -> {
                updateAutoCapture(event.enabled)
            }

            is ScannerUiEvent.OpenImageSheet -> {
                _uiState.update { it.copy(shouldOpenSheet = true) }
            }

            is ScannerUiEvent.CloseImageSheet -> {
                _uiState.update { it.copy(shouldOpenSheet = false) }
            }

            is ScannerUiEvent.TriggerManualCapture -> {
                scannerAnalyzer?.triggerManualCapture()
            }

            is ScannerUiEvent.OnDocumentDetected -> {
                _uiState.update { it.copy(detectedDocument = event.document) }
            }

            is ScannerUiEvent.OnDocumentCaptured -> {
                saveCapturedDocument(event.context, event.bitmap)
                _uiState.update { it.copy(savedBitmap = event.bitmap) }
            }

            is ScannerUiEvent.OnCameraReady -> {
                camera = event.camera
            }

            is ScannerUiEvent.OnAnalyzerReady -> {
                scannerAnalyzer = event.analyzer
                // Update analyzer config when it's ready
                scannerAnalyzer?.updateConfig(_uiState.value.scannerConfig)
            }

            is ScannerUiEvent.LoadImageList -> {
                val imageList = fetchCacheFiles(event.context)
                _uiState.update {
                    it.copy(
                        imageList = imageList,
                        shouldOpenSheet = true
                    )
                }
            }

            is ScannerUiEvent.OnCameraZoom -> {
                camera?.cameraControl?.setLinearZoom(event.zoom / 20f)
            }

        }
    }

    /**
     * Get current scanner config
     */
    fun getScannerConfig(): DocumentScannerConfig {
        return _uiState.value.scannerConfig
    }

    /**
     * Toggle flash on/off
     */
    private fun toggleFlash() {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                val newState = it.cameraInfo.torchState.value == 0
                it.cameraControl.enableTorch(newState)
                _uiState.update { state -> state.copy(isFlashOn = newState) }
            }
        }
    }

    /**
     * Update auto capture setting
     */
    private fun updateAutoCapture(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(autoCaptureEnabled = enabled)
        }
        updateScannerConfig(enabled)
    }

    /**
     * Update scanner config when auto capture changes
     */
    private fun updateScannerConfig(autoCapture: Boolean) {
        val newConfig = DocumentScannerConfig(
            autoCapture = autoCapture,
            strokeColor = android.graphics.Color.YELLOW,
            fillAlpha = 0.2f,
            detectionMode = 1,
            smoothingAlpha = 0.15f
        )
        _uiState.update { it.copy(scannerConfig = newConfig) }
        // Update analyzer if it's already ready
        scannerAnalyzer?.updateConfig(newConfig)
    }

    /**
     * Save captured document to cache
     */
    private fun saveCapturedDocument(context: Context, bitmap: Bitmap) {
        val cacheDir = context.cacheDir
        val tempFile = File(cacheDir, "edge_detection_image")

        if (!tempFile.exists()) {
            tempFile.mkdir()
        }

        val file = File(tempFile.absolutePath, "${System.currentTimeMillis()}.jpg")

        try {
            val outputStream = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fetch cached image files
     */
    private fun fetchCacheFiles(context: Context): List<File> {
        var list = emptyList<File>()
        context.cacheDir.listFiles()?.forEach {
            if (it.isDirectory) {
                list = it.listFiles()?.toList() ?: emptyList()
            }
        }
        return list
    }
}
