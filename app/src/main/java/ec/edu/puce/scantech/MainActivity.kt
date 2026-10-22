package ec.edu.puce.scantech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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

    private val inventoryViewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
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
