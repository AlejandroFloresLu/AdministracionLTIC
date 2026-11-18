package ec.edu.puce.scantech.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ec.edu.puce.scantech.ui.detail.AssetDetailScreen
import ec.edu.puce.scantech.ui.form.AssetFormScreen
import ec.edu.puce.scantech.ui.inventory.InventoryDashboardScreen
import ec.edu.puce.scantech.ui.inventory.InventoryViewModel
import ec.edu.puce.scantech.ui.permissions.CameraPermissionScreen
import ec.edu.puce.scantech.ui.scanner.ScannerScreen

/**
 * [RDA-2 Temas 7 y 8: Navegación Progresiva y Fuertemente Tipada (Type-Safe Navigation)]
 * [RDA-3 Temas 9 y 10: Desacoplamiento de Pantallas y Coordinación de Flujos]
 *
 * Grafo de Navegación Declarativo y Fuertemente Tipado para ScanTech LTIC en Android 16 (API 36).
 *
 * Conceptos Teóricos Aplicados:
 * 1. Single NavHost (Escenario Central): Existe un único contenedor dinámico que intercambia los
 *    destinos visuales. Evita comportamientos anómalos o fugas en el Back Stack.
 * 2. Navegación Fuertemente Tipada con @Serializable: Cada destino es una clase u objeto Kotlin serializable.
 *    La deserialización automática con `backStackEntry.toRoute<T>()` valida los argumentos en compilación,
 *    eliminando de raíz los errores por cadenas mágicas obsoletas ("asset/{id}").
 * 3. Desacoplamiento de ViewModels (SoC): Cada pantalla (`ScannerScreen`, `AssetDetailScreen`,
 *    `AssetFormScreen`) gestiona su propio ViewModel especializado con ciclo de vida independiente.
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
                },
                onNavigateToCreate = {
                    navController.navigate(NavRoute.AssetFormRoute(assetId = null))
                }
            )
        }

        // Pantalla de Solicitud de Permiso de Cámara
        composable<NavRoute.CameraPermissionRoute> {
            CameraPermissionScreen(
                onRequestPermission = {
                    // Tras conceder el permiso, transita al escáner óptico reemplazando la pantalla de permisos
                    navController.navigate(NavRoute.ScannerRoute) {
                        popUpTo<NavRoute.CameraPermissionRoute> { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de Escáner Óptico de Códigos QR (Edge AI)
        composable<NavRoute.ScannerRoute> {
            ScannerScreen(
                onBarcodeDetected = { detectedId ->
                    // Al decodificar el código QR, transita directamente a la Ficha Técnica del hardware
                    navController.navigate(NavRoute.AssetDetailRoute(assetId = detectedId)) {
                        popUpTo<NavRoute.ScannerRoute> { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de Ficha Técnica del Activo (Módulo de Detalle)
        composable<NavRoute.AssetDetailRoute> { backStackEntry ->
            val routeArgs = backStackEntry.toRoute<NavRoute.AssetDetailRoute>()
            AssetDetailScreen(
                assetId = routeArgs.assetId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { editAssetId ->
                    navController.navigate(NavRoute.AssetFormRoute(assetId = editAssetId))
                }
            )
        }

        // Módulo de Registro / Formulario Ultra-Resiliente (Tarea #55 y Enunciado §3)
        composable<NavRoute.AssetFormRoute> { backStackEntry ->
            val routeArgs = backStackEntry.toRoute<NavRoute.AssetFormRoute>()
            AssetFormScreen(
                assetId = routeArgs.assetId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    viewModel.loadActivos() // Refresca reactivamente el catálogo
                    navController.popBackStack()
                }
            )
        }
    }
}
