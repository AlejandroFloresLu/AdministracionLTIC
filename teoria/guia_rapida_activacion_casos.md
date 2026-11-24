# 🎯 Guía Rápida de Activación — 30 Casos de Defensa Examen Final Fase 1
## ScanTech LTIC · Programación Móvil · PUCE

> **Cómo usar esta guía en el examen:**
> Cuando tu profesor te asigne o mencione uno de los casos, busca el número en esta tabla, abre el archivo indicado y verás el bloque `/* =================== [DEFENSA CASO X] ===================`.
> 1. Comenta la línea o bloque original indicado en el comentario.
> 2. Descomenta el código del caso.
> 3. Lee la **💡 RESPUESTA TEÓRICA RÁPIDA** incluida en el mismo bloque para defender oralmente ante el docente.

---

## 🗺️ Mapa de Búsqueda Rápida (Caso ➔ Archivo ➔ Línea Aprox.)

| Caso | Título / Tema Principal | Archivo a Abrir | Ubicación / Método |
|:---:|---|---|---|
| **1** | Validar QR con `.takeIf` y `.let` | `domain/validacion/ValidadorActivo.kt` | Dentro de `validar()` en `when` de `datos.qrCode` |
| **2** | `toIntOrNull()` con mensaje dinámico y Elvis | `domain/validacion/ValidadorActivo.kt` | Dentro de `validar()` en `unidades` |
| **3** | Función de extensión `String.esCodigoQrLtic()` | `domain/validacion/ValidadorActivo.kt` | Al final del archivo |
| **4** | Sanitizar texto de specs con `.run { trim().uppercase() }` | `ui/form/AssetFormViewModel.kt` | En `saveAsset()`, creación de `assetToSave` |
| **5** | Desestructuración de data class `Asset` para auditoría | `ui/detail/AssetDetailScreen.kt` | En `when (uiState)` rama `is AssetDetailUiState.Success` |
| **6** | Parámetro `modoAuditoria: Boolean` en ruta tipada | `ui/navigation/NavRoutes.kt`<br>`ui/navigation/NavGraph.kt`<br>`ui/detail/AssetDetailScreen.kt` | En `AssetDetailRoute`<br>En llamada de `AssetDetailScreen`<br>En TopAppBar `actions` |
| **7** | `popUpTo` al guardar en formulario | `ui/navigation/NavGraph.kt` | En `composable<NavRoute.AssetFormRoute>`, en `onSaveSuccess` |
| **8** | Nueva ruta tipada `HistorialBajasRoute` | `ui/navigation/NavRoutes.kt`<br>`ui/navigation/NavGraph.kt`<br>`ui/inventory/InventoryDashboardScreen.kt` | En `sealed interface NavRoute`<br>En `NavHost` composable<br>En TopAppBar `actions` |
| **9** | Función de extensión inline `route<T>()` con `reified` | `ui/navigation/NavGraph.kt` | Al final del archivo y en `toRoute` |
| **10** | Justificación teórica de `popUpTo` en permisos | `ui/navigation/NavGraph.kt` | En `composable<NavRoute.CameraPermissionRoute>` |
| **11** | Tokens semánticos `tertiaryContainer` en Card | `ui/detail/AssetDetailScreen.kt` | En tarjeta de Ubicación Física |
| **12** | Botón Guardar `enabled` reactivo a validación | `ui/form/AssetFormScreen.kt` | En `Button(onClick = onSave, enabled = ...)` |
| **13** | Tipografía descargable `RobotoMonoFontFamily` | `ui/detail/AssetDetailScreen.kt` | En `AssetHeaderCard` dentro de `Text(asset.qrCode)` |
| **14** | Extraer diálogo de baja a composable Stateless | `ui/detail/AssetDetailScreen.kt` | En `if (showDeleteDialog)` y al final del archivo |
| **15** | `rememberSaveable(asset.id)` ante rotación | `ui/detail/AssetDetailScreen.kt` | Al final del archivo (documentación y ejemplo) |
| **16** | Sobrescribir `onCleared()` en ViewModel | `ui/detail/AssetDetailViewModel.kt` | Al final de la clase antes de `companion object` |
| **17** | Auditoría y persistencia en `SavedStateHandle` | `ui/form/AssetFormViewModel.kt` | En `onQrChange(value: String)` |
| **18** | Máquina de estados finita con `Sealed Interface` | `ui/form/AssetFormUiState.kt` | Al final del archivo |
| **19** | Hilos de fondo con `Dispatchers.IO` en ViewModel | `ui/detail/AssetDetailViewModel.kt` | En `guardarObservaciones()` |
| **20** | Encapsulación `_uiState` privado vs `uiState` público | `ui/scanner/ScanViewModel.kt` | Declaración de `_lastDetectedQr` y `uiState` |
| **21** | Caso de uso puro `FiltrarActivosPorEstadoUseCase` con `invoke` | `domain/usecase/ObtenerActivosUseCase.kt` | Al final del archivo |
| **22** | Propiedad calculada `estaActivo` en modelo de dominio | `domain/model/Asset.kt` | Dentro de `data class Asset` después de `location` |
| **23** | Verificación de pureza de dominio con comando `grep` | `domain/model/Asset.kt` | Al final del archivo |
| **24** | Validación de regla de negocio `units >= 1` en dominio | `domain/usecase/GuardarActivoUseCase.kt` | Al inicio de `invoke()` |
| **25** | Manejo exhaustivo de `Result.Success` y `Result.Error` | `ui/form/AssetFormViewModel.kt` | En `saveAsset()` dentro de `when (result)` |
| **26** | Consulta reactiva con `Flow` por bloque físico | `data/local/dao/AssetDao.kt` | En sección de lecturas de UI |
| **27** | Atomicidad transaccional con `@Transaction` | `data/local/dao/AssetDao.kt` | En `insertWithQueue()` |
| **28** | Baja lógica (*Soft Delete*) vs Baja física (*Hard Delete*) | `data/local/dao/AssetDao.kt` | En `markDeleted()` |
| **29** | Escape de comodines `%` y `_` en `escaparLike()` | `data/local/dao/AssetDao.kt` | En `companion object` |
| **30** | Trazabilidad completa del flujo SSOT (Room ➔ UI) | `data/local/dao/AssetDao.kt` | Al inicio de la clase `AssetDao` |

---

## 💡 Consejos de Oro para la Defensa Oral

1. **Si te preguntan por qué no usamos DTOs ni Room en `domain/`:**
   > *"Por la Regla de Dependencia de Clean Architecture. El dominio es el núcleo (Core) del negocio y debe ser Kotlin puro (JVM). Esto permite probar todas las reglas de negocio con tests unitarios instantáneos sin depender de emuladores ni del SDK de Android."*

2. **Si te preguntan por qué usamos `StateFlow` y no `LiveData`:**
   > *"StateFlow es la solución moderna y nativa de corrutinas para Kotlin y Compose. Es inmutable, siempre tiene un valor inicial garantizado, y se recolecta eficientemente con `collectAsStateWithLifecycle()` respetando el ciclo de vida de la pantalla."*

3. **Si te preguntan por qué la baja es lógica y no física:**
   > *"En una arquitectura Offline-First, si borramos físicamente una fila de SQLite sin conexión, la app no tendría forma de recordar qué registro debe eliminarse en el servidor cuando retorne el internet. Con el soft delete (`deletedAt != null`), la fila se oculta de la UI pero permanece en la base de datos hasta que el servidor confirma la eliminación."*

4. **Si te preguntan por qué usamos rutas `@Serializable` en lugar de strings:**
   > *"Las rutas basadas en Strings (`asset/{id}`) fallan silenciosamente en tiempo de ejecución ante errores tipográficos. Con `@Serializable`, el compilador de Kotlin verifica en tiempo de compilación que los argumentos existan y tengan el tipo correcto, garantizando Type-Safety absoluto."*

