package ec.edu.puce.scantech.ui.scanner

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.edu.puce.scantech.R
import java.util.concurrent.Executors

/**
 * [RDA-1 Tema 5: UI Declarativa - State Hoisting (Stateful vs Stateless)]
 * [RDA-2 Temas 7 y 8: Aislamiento Cromático en Material Design 3 y Visión Artificial]
 * [RDA-3 Temas 9 y 10: Arquitectura MVVM, UDF y Procesamiento No Bloqueante]
 *
 * Pantalla de Escáner Óptico Inteligente de Activos LTIC (Tareas #4, #5 y #27).
 *
 * Cumplimiento de Criterios de Aceptación:
 * 1. Vista previa a pantalla completa con CameraX enlazada al ciclo de vida (#4).
 * 2. Marco/overlay guía animado que cambia reactivamente de color (#4 y #5).
 * 3. Botón de linterna sincronizado con [CameraControl.enableTorch] (#4).
 * 4. Vibración háptica en la detección y animación cromática (#5).
 * 5. Verificación inmediata en base local (Room): si existe navega a detalle; si no,
 *    ofrece diálogo modal para registrar con código prellenado (#5).
 * 6. Edge-to-Edge con WindowInsets para evitar solapamientos con barras del sistema (#59).
 */
@Composable
fun ScannerScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreateWithQr: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // Feedback háptico y navegación inmediata al detectar activo existente en Room (Tarea #5)
    LaunchedEffect(uiState.navigateToAssetId) {
        uiState.navigateToAssetId?.let { assetId ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onNavigateToDetail(assetId)
            viewModel.onNavigationHandled()
        }
    }

    // Feedback háptico al detectar código no registrado
    LaunchedEffect(uiState.unregisteredQrCode) {
        if (uiState.unregisteredQrCode != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    ScannerContent(
        uiState = uiState,
        onToggleFlashlight = viewModel::toggleFlashlight,
        onCodeDetected = viewModel::onCodeScanned,
        onSimulateDetection = viewModel::onCodeScanned,
        onNavigateBack = onNavigateBack,
        onDismissUnregisteredDialog = viewModel::dismissUnregisteredDialog,
        onConfirmRegisterNew = { qrCode ->
            viewModel.dismissUnregisteredDialog()
            onNavigateToCreateWithQr(qrCode)
        },
        modifier = modifier
    )
}

/**
 * Componente de presentación pura (Stateless) con elevación de estado (State Hoisting).
 */
@Composable
fun ScannerContent(
    uiState: ScanUiState,
    onToggleFlashlight: () -> Unit,
    onCodeDetected: (String) -> Unit,
    onSimulateDetection: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onDismissUnregisteredDialog: () -> Unit,
    onConfirmRegisterNew: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Animación cromática fluida del marco reticular (Tarea #5)
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            uiState.navigateToAssetId != null -> MaterialTheme.colorScheme.primary
            uiState.unregisteredQrCode != null -> MaterialTheme.colorScheme.error
            uiState.isAnalyzing -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(durationMillis = 350),
        label = "reticle_border_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
    ) {
        // Vista Previa de Cámara CameraX a pantalla completa (Tarea #4 y #27)
        CameraPreview(
            isScanningPaused = uiState.isScanningPaused,
            isFlashlightOn = uiState.isFlashlightOn,
            onQrDetected = onCodeDetected,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay oscuro translúcido superior e inferior para resaltar el marco central
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
        )

        // Barra Superior de Acciones (Retorno, Título y Linterna) con Insets Edge-to-Edge (#59)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = statusBarPadding.calculateTopPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp
                )
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }

            Text(
                text = "Escáner Óptico Edge AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f)
            ) {
                IconButton(onClick = onToggleFlashlight) {
                    Icon(
                        imageVector = if (uiState.isFlashlightOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = stringResource(R.string.flashlight),
                        tint = if (uiState.isFlashlightOn) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.inverseOnSurface
                        }
                    )
                }
            }
        }

        // Zona Central de Escaneo: Marco Reticular con Animación Reactiva (Tareas #4 y #5)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(
                        width = 3.dp,
                        color = animatedBorderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(
                        color = animatedBorderColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isAnalyzing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(140.dp),
                        tint = animatedBorderColor.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
            ) {
                Text(
                    text = when {
                        uiState.isAnalyzing -> "Verificando en base de datos local..."
                        uiState.isScanningPaused -> "Escaneo en pausa"
                        else -> "Alinee el código QR del activo dentro del marco"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Barra inferior: Estado reactivo y botón de simulación rápida para laboratorio (Insets Edge-to-Edge #59)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = navBarPadding.calculateBottomPadding() + 16.dp,
                    top = 8.dp
                )
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (uiState.isScanningPaused) {
                    "Procesamiento completado"
                } else {
                    "Escaneando continuo activo... (<30ms Edge AI)"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Simulación interactiva de detección de código QR en laboratorio
            Button(
                onClick = { onSimulateDetection("LTIC-000123") },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simular Detección QR (#LTIC-000123)",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Diálogo modal cuando el QR no existe en Room (Tarea #5 - Decisión de Diseño del Usuario)
        if (uiState.unregisteredQrCode != null) {
            AlertDialog(
                onDismissRequest = onDismissUnregisteredDialog,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Activo No Encontrado",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "El código '${uiState.unregisteredQrCode}' no se encuentra registrado en el inventario local de Room.\n\n¿Desea registrarlo ahora como un nuevo activo?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onConfirmRegisterNew(uiState.unregisteredQrCode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Registrar Activo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUnregisteredDialog) {
                        Text("Reintentar Escaneo")
                    }
                }
            )
        }
    }
}

/**
 * Componente que enlaza CameraX [PreviewView] y [ImageAnalysis] con el ciclo de vida de Compose.
 */
@Composable
fun CameraPreview(
    isScanningPaused: Boolean,
    isFlashlightOn: Boolean,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Sincronización del control de linterna de hardware (Tarea #4)
    LaunchedEffect(camera, isFlashlightOn) {
        try {
            camera?.cameraControl?.enableTorch(isFlashlightOn)
        } catch (e: Exception) {
            Log.w("CameraPreview", "No se pudo cambiar estado de linterna: ${e.message}")
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        QrCodeAnalyzer(
                            isPaused = { isScanningPaused },
                            onQrCodeDetected = onQrDetected
                        )
                    )

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    camera?.cameraControl?.enableTorch(isFlashlightOn)
                } catch (e: Exception) {
                    Log.w("CameraPreview", "Aviso: Entorno sin cámara física disponible: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
