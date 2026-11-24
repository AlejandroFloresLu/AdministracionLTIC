package ec.edu.puce.scantech

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ec.edu.puce.scantech.ui.inventory.InventoryUiState
import ec.edu.puce.scantech.ui.inventory.InventoryViewModel
import ec.edu.puce.scantech.ui.navigation.ScanTechNavGraph
import ec.edu.puce.scantech.ui.theme.ScanTechTheme

/**
 * Actividad Principal de ScanTech LTIC.
 *
 * Implementa las directrices del Tema 4 (Ciclo de Vida y Componentes Fundamentales) y Tema 7 (Material 3):
 * - Activación obligatoria de [enableEdgeToEdge] para que el contenido se dibuje fluidamente
 *   detrás de las barras de estado y navegación del sistema.
 * - Instanciación de [InventoryViewModel] respaldado por ViewModelProvider y SavedStateHandle para
 *   sobrevivir a cambios de configuración (rotación) y muerte de proceso.
 * - Envoltorio composable [ScanTechTheme] con soporte de Material You (Dynamic Color) y Surface M3.
 */
class MainActivity : ComponentActivity() {

    private val inventoryViewModel: InventoryViewModel by viewModels { InventoryViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Caso de práctica 10: se instala antes de super.onCreate(). La splash se mantiene
        // mientras el catálogo siga en Loading (Room aún no emitió), así no se ve un parpadeo.
        // El tope de tiempo evita que quede fija si la app se restaura en otra pantalla.
        val splash = installSplashScreen()
        val inicio = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            inventoryViewModel.uiState.value is InventoryUiState.Loading &&
                SystemClock.uptimeMillis() - inicio < MAX_SPLASH_MS
        }

        // Habilitación de Edge-to-Edge nativo antes del setContent
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ScanTechTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScanTechNavGraph(viewModel = inventoryViewModel)
                }
            }
        }
    }
}

/** Tiempo máximo que la splash espera al primer dato de Room. */
private const val MAX_SPLASH_MS = 1_500L
