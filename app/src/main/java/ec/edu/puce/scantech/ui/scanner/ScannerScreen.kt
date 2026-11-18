package ec.edu.puce.scantech.ui.scanner

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.edu.puce.scantech.R

/**
 * [RDA-1 Tema 5: UI Declarativa - State Hoisting (Stateful vs Stateless)]
 * [RDA-2 Temas 7 y 8: Aislamiento Cromático en Material Design 3 (Cero Colores Hardcodeados)]
 * [RDA-3 Temas 9 y 10: Arquitectura MVVM y Flujo Unidireccional (UDF)]
 *
 * Pantalla de Escáner Óptico de Activos LTIC.
 *
 * Buenas Prácticas Aplicadas:
 * 1. Aislamiento Cromático (M3 Semántico): Se erradicaron todos los colores hexadecimales estáticos
 *    (`Color(0x...)`, `Color.White`, `Color.Black`). Cada componente consulta exclusivamente los roles
 *    del tema dinámico (`scrim`, `inverseOnSurface`, `surfaceContainerHighest`, `tertiary`, `primary`).
 * 2. Desacoplamiento de ViewModel: La pantalla ya no depende de `InventoryViewModel`; consume su propio
 *    [ScanViewModel] con estado reactivo [ScanUiState] respaldado por [androidx.lifecycle.SavedStateHandle].
 * 3. Composable Stateful vs Stateless:
 *    - [ScannerScreen]: Contenedor con estado que recolecta el [ScanUiState] y despacha eventos al ViewModel.
 *    - [ScannerContent]: Componente puro de presentación (stateless) óptimo para previsualizaciones y tests unitarios.
 */
@Composable
fun ScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScannerContent(
        uiState = uiState,
        onToggleFlashlight = viewModel::toggleFlashlight,
        onSimulateDetection = { qrCode ->
            viewModel.onCodeScanned(qrCode)
            onBarcodeDetected(qrCode)
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * Componente de representación visual pura (Stateless) con elevación de estado (State Hoisting).
 */
@Composable
fun ScannerContent(
    uiState: ScanUiState,
    onToggleFlashlight: () -> Unit,
    onSimulateDetection: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // Uso de rol semántico M3 scrim para representar el visor oscuro de cámara sin hardcodear colores
            .background(MaterialTheme.colorScheme.scrim)
    ) {
        // Barra Superior de Acciones (Retorno, Título y Linterna)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
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
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
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

        // Zona Central de Escaneo con Retícula Visual M3
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
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
            ) {
                Text(
                    text = "Alinee el código QR o de barras del hardware dentro del marco",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Barra inferior: Feedback de estado reactivo y simulación interactiva para laboratorio
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (uiState.isScanningPaused) {
                    "Escaneo en pausa (código detectado)"
                } else {
                    "Escaneando continuo activo... (<30ms Edge AI)"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
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
    }
}
