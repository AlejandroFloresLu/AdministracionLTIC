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
        startDestination = NavRoute.DashboardRoute(auditHash = AUDIT_HASH_INICIAL),
        modifier = modifier
    ) {
        // Pantalla Principal: Catálogo General de Inventario
        composable<NavRoute.DashboardRoute> { backStackEntry ->
            // Caso 1: el catálogo extrae auditHash de la ruta tipada con toRoute()
            val routeArgs = backStackEntry.toRoute<NavRoute.DashboardRoute>()
            InventoryDashboardScreen(
                viewModel = viewModel,
                auditHash = routeArgs.auditHash,
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

        // Pantalla de Escáner Óptico de Códigos QR (Edge AI - Tareas #4, #5 y #27)
        composable<NavRoute.ScannerRoute> {
            ScannerScreen(
                onNavigateToDetail = { detectedId ->
                    // Al decodificar un código existente en Room, transita a la Ficha Técnica (#5)
                    navController.navigate(NavRoute.AssetDetailRoute(assetId = detectedId)) {
                        popUpTo<NavRoute.ScannerRoute> { inclusive = true }
                    }
                },
                onNavigateToCreateWithQr = { initialQr ->
                    // Si el código no existe en Room, transita al formulario con el QR prellenado (#5)
                    navController.navigate(NavRoute.AssetFormRoute(assetId = null, initialQr = initialQr)) {
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
                initialQr = routeArgs.initialQr,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    // Caso 1: el formulario inyecta el hash de auditoría al volver al catálogo.
                    // popUpTo reemplaza el catálogo anterior para no duplicarlo en el back stack.
                    navController.navigate(NavRoute.DashboardRoute(auditHash = AUDIT_HASH_FORMULARIO)) {
                        popUpTo<NavRoute.DashboardRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

    }
}

/** [Caso 1] Hash con el que abre la app, antes de cualquier registro. */
private const val AUDIT_HASH_INICIAL = "SESION-INICIAL"

/** [Caso 1] Código estático que el formulario envía al catálogo tras guardar. */
private const val AUDIT_HASH_FORMULARIO = "HASH-2026-RDA3"
