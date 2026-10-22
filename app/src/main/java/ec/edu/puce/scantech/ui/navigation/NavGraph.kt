package ec.edu.puce.scantech.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ec.edu.puce.scantech.ui.detail.AssetDetailScreen
import ec.edu.puce.scantech.ui.inventory.InventoryDashboardScreen
import ec.edu.puce.scantech.ui.inventory.InventoryViewModel
import ec.edu.puce.scantech.ui.permissions.CameraPermissionScreen
import ec.edu.puce.scantech.ui.scanner.ScannerScreen

/**
 * Grafo de Navegación Declarativo y Fuertemente Tipado (Type-Safe Navigation).
 *
 * Implementa las directrices de la Tarea #2 y del Tema 2 de la cátedra:
 * - Un único NavHost para evitar comportamientos erráticos del Back Stack.
 * - Rutas tipadas usando la anotación `@Serializable` con el método `toRoute<T>()`.
 * - Cero uso de Strings mágicos en los destinos o paso de parámetros.
 */
@Composable
fun ScanTechNavGraph(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.DashboardRoute,
        modifier = modifier
    ) {
        // Pantalla Principal: Catálogo General de Inventario
        composable<NavRoute.DashboardRoute> {
            InventoryDashboardScreen(
                viewModel = viewModel,
                onNavigateToDetail = { assetId ->
                    navController.navigate(NavRoute.AssetDetailRoute(assetId = assetId))
                },
                onNavigateToScanner = {
                    navController.navigate(NavRoute.CameraPermissionRoute)
                }
            )
        }

        // Pantalla de Solicitud de Permiso de Cámara
        composable<NavRoute.CameraPermissionRoute> {
            CameraPermissionScreen(
                onRequestPermission = {
                    // Tras conceder el permiso, navega al escáner óptico
                    navController.navigate(NavRoute.ScannerRoute) {
                        popUpTo<NavRoute.CameraPermissionRoute> { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de Escáner Óptico de Códigos QR
        composable<NavRoute.ScannerRoute> {
            ScannerScreen(
                onBarcodeDetected = { detectedId ->
                    // Al decodificar el código QR, transita directamente a la Ficha Técnica del activo
                    navController.navigate(NavRoute.AssetDetailRoute(assetId = detectedId)) {
                        popUpTo<NavRoute.ScannerRoute> { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de Detalle y Ficha Técnica del Activo
        composable<NavRoute.AssetDetailRoute> { backStackEntry ->
            val routeArgs = backStackEntry.toRoute<NavRoute.AssetDetailRoute>()
            AssetDetailScreen(
                assetId = routeArgs.assetId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
