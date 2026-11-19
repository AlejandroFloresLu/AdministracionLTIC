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
            /* =================== [DEFENSA CASO 10: Limpieza de Back Stack con popUpTo] ===================
            // 💡 EXPLICACIÓN TEÓRICA RÁPIDA:
            // - 'popUpTo<T> { inclusive = true }' elimina el destino especificado del Back Stack antes de navegar.
            // - Si omitiéramos 'inclusive = true', al presionar el botón físico "Atrás" desde el escáner,
            //   el usuario volvería a ver la pantalla de permisos (comportamiento molesto y redundante).
            // - Con esto garantizamos un flujo natural: Dashboard -> Permiso -> Escáner (BackStack: Dashboard -> Escáner).
            ============================================================================================= */
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

            /* =================== [DEFENSA CASO 9: Extracción Simplificada con inline reified] ===================
            // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
            // Descomenta la función inline al final del archivo y reemplaza la línea de arriba por:
            // val routeArgs = backStackEntry.route<NavRoute.AssetDetailRoute>()

            // 💡 RESPUESTA TEÓRICA RÁPIDA:
            // - En la JVM los tipos genéricos se eliminan en compilación (Type Erasure), por lo que normalmente
            //   no se puede hacer 'T::class'. Con 'inline' y 'reified T', el compilador copia el código en el punto
            //   de llamada y preserva la información de tipo real en tiempo de ejecución.
            ==================================================================================================== */

            AssetDetailScreen(
                assetId = routeArgs.assetId,
                /* =================== [DEFENSA CASO 6: Propagación de modoAuditoria] ===================
                // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
                // Descomenta esta línea para propagar el modo auditoría a la pantalla de detalle:
                // modoAuditoria = routeArgs.modoAuditoria,
                ====================================================================================== */
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
                    // --- CÓDIGO ORIGINAL (Comentar si el docente pide activar el Caso 7) ---
                    // El catálogo observa Room: se actualiza solo, no hace falta recargarlo.
                    navController.popBackStack()

                    /* =================== [DEFENSA CASO 7: popUpTo al Guardar en Formulario] ===================
                    // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
                    // 1. Comenta 'navController.popBackStack()' de arriba.
                    // 2. Descomenta esta navegación con limpieza de formulario del back stack:
                    // navController.navigate(NavRoute.DashboardRoute) {
                    //     popUpTo<NavRoute.DashboardRoute> { inclusive = false }
                    // }

                    // 💡 RESPUESTA TEÓRICA RÁPIDA:
                    // - Al guardar un registro, el formulario ya cumplió su propósito. Si solo usamos 'popBackStack()',
                    //   el usuario podría regresar por accidente a un estado obsoleto si navegó por enlaces anidados.
                    // - 'popUpTo<DashboardRoute> { inclusive = false }' vacía todo el historial intermedio hasta el Dashboard,
                    //   asegurando que el botón Atrás cierre la aplicación y no reabra el formulario.
                    =========================================================================================== */
                }
            )
        }

        /* =================== [DEFENSA CASO 8: Registro del Destino HistorialBajas en Grafo] ===================
        // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
        // Descomenta este bloque composable para registrar la ruta en el grafo:
        // composable<NavRoute.HistorialBajasRoute> {
        //     androidx.compose.foundation.layout.Box(
        //         modifier = Modifier.fillMaxSize(),
        //         contentAlignment = androidx.compose.ui.Alignment.Center
        //     ) {
        //         androidx.compose.material3.Text(
        //             text = "Historial de Activos Dados de Baja (En construcción)",
        //             style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        //         )
        //     }
        // }
        ====================================================================================================== */
    }
}

/* =================== [DEFENSA CASO 9: Función Inline de Extensión route<T>()] ===================
// 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
// Descomenta esta función de extensión genérica al final del archivo:
// inline fun <reified T : NavRoute> androidx.navigation.NavBackStackEntry.route(): T = toRoute<T>()
================================================================================================ */
