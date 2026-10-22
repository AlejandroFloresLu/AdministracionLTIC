# Informe Técnico de Implementación: ScanTech LTIC
**Proyecto:** ScanTech LTIC - Sistema Móvil Nativo de Auditoría y Trazabilidad  
**Cátedra:** Programación Móvil - PUCE  
**Facultad:** Hábitat, Infraestructura y Creatividad  
**Rama de Trabajo:** `Paúl`  

---

## 1. Resumen Ejecutivo y Alineación Metodológica

El presente documento detalla la resolución técnica y arquitectónica de las **8 actividades** iniciales solicitadas en el [documento_maestro_del_proyecto_scantech_ltic.md](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/documento_maestro_del_proyecto_scantech_ltic.md), integrando rigurosamente los lineamientos de la [Tarea 2.2. Bosquejo Proyecto Final (1).pdf](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/Tarea%202.2.%20Bosquejo%20Proyecto%20Final%20(1).pdf), la propuesta de equipo en [ScanTech - Presentacion.pdf](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/ScanTech%20-%20Presentacion.pdf) y las buenas prácticas pedagógicas de:
* **RDA-1 Tema 4:** Ciclo de vida efímero de componentes, `ViewModel` con `SavedStateHandle` frente a rotaciones y *Process Death*, `rememberSaveable`, ejecución asíncrona fuera del hilo principal (evitar ANRs) y adopción de Android 16 (API 36).
* **RDA-1 Tema 5:** Cambio de paradigma hacia UI Declarativa (Jetpack Compose), `UI = f(State)`, recomposición inteligente, **prohibición estricta de `Column`/`Row` con `forEach`** en listas dinámicas, y uso mandatorio de `LazyColumn`/`LazyRow` para garantizar 60 FPS y reciclaje de memoria.
* **RDA-2 Temas 7 y 8:** Material Design 3 con Color Semántico (accesibilidad mínimo 4.5:1), soporte nativo de *Material You* (`dynamicLightColorScheme` y `dynamicDarkColorScheme`), centralización en `strings.xml` (i18n y resiliencia visual), y **Navegación Fuertemente Tipada (Type-Safe Navigation)** con `@Serializable` y `toRoute<T>()`.

---

## 2. Matriz de Cumplimiento de las 8 Actividades

| # | Código y Tarea | Archivos Principales | Conceptos Clave Aplicados |
|---|----------------|----------------------|---------------------------|
| **1** | **[H1][UI] Tema Material 3** | [Color.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/theme/Color.kt)<br>[Theme.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/theme/Theme.kt)<br>[Type.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/theme/Type.kt)<br>[MainActivity.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/MainActivity.kt) | M3 Semántico, Material You dinámico (API 31+), soporte modo oscuro como ciudadano de primera clase, `enableEdgeToEdge()`. |
| **20** | **[H1][BD] Enum SyncStatus y TypeConverters** | [SyncStatus.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/data/local/SyncStatus.kt)<br>[SyncStatusConverter.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/data/local/SyncStatusConverter.kt)<br>[ActivoLTIC.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/data/model/ActivoLTIC.kt) | Estrategia Offline-First (`SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`), `@TypeConverter` con tolerancia a nulos, cero `!!` y uso de operador Elvis `?:`. |
| **29** | **[H1][LÓGICA] ViewModels y UI States** | [InventoryUiState.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/inventory/InventoryUiState.kt)<br>[InventoryViewModel.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/inventory/InventoryViewModel.kt) | UDF, `sealed interface` (`Idle`, `Loading`, `Success`, `Error`), `SavedStateHandle` para blindar contra *Process Death* y rotación, corrutinas en `Dispatchers.IO`. |
| **3** | **[H1][UI] Permiso de Cámara** | [CameraPermissionScreen.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/permissions/CameraPermissionScreen.kt) | Layout centrado en `Column`, icono M3 grande en contenedor tonal, texto explicativo claro sin strings hardcodeados, botón primario. |
| **14** | **[H1][UI] Estados Carga, Vacío y Error** | [StateComponents.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/components/StateComponents.kt) | `LoadingIndicator` en `Box(fillMaxSize)`, `EmptyState` con acción de reinicio, `ErrorState` con color semántico de error y reintento. |
| **2** | **[H1][UI] Navegación Type-Safe** | [NavRoutes.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/navigation/NavRoutes.kt)<br>[NavGraph.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/navigation/NavGraph.kt) | Contratos fuertemente tipados con `@Serializable` (`DashboardRoute`, `ScannerRoute`, `AssetDetailRoute(assetId)`), `composable<T>`, extracción con `toRoute<T>()`. |
| **9** | **[H1][UI] Búsqueda con State Hoisting** | [InventoryDashboardScreen.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/inventory/InventoryDashboardScreen.kt) | State Hoisting: `searchQuery` y `onSearchQueryChange` provistos por ViewModel/Caller. Cero `remember` mutable interno en el composable de búsqueda. |
| **10** | **[H1][UI] Filtros por Categoría con FilterChip** | [InventoryDashboardScreen.kt](file:///c:/Users/ASUS/Documents/Proyectos%20Personales/Proyecto%20Programaci%C3%B3n%20M%C3%B3vil%20Final/AdministracionLTIC/app/src/main/java/ec/edu/puce/scantech/ui/inventory/InventoryDashboardScreen.kt) | **Uso mandatorio de `LazyRow`** (prohibido `Row` con `forEach`), `FilterChip` de Material 3 con icono check reactivo, y listado general mediante `LazyColumn` a 60 FPS. |

---

## 3. Desglose Detallado por Actividad

### Actividad 1: [H1][UI] Tema Material 3 y Edge-to-Edge
* **`Color.kt`**: Implementa la paleta de identidad corporativa de ScanTech LTIC extraída de los mockups (morados profundos `#6C4AB6`, acentos magenta neón `#D81B60` / `#E040FB`, superficies oscuras `#0F0B15` para bodegas con poca luz y bajo consumo OLED, y colores semánticos para estados de auditoría: verde operativo, ámbar advertencia, rojo crítico).
* **`Theme.kt`**: Integra la función algorítmica de Material You (`dynamicDarkColorScheme` / `dynamicLightColorScheme`) con fallback estático seguro en dispositivos pre-Android 12.
* **`MainActivity.kt`**: Configura `enableEdgeToEdge()` antes de `setContent`, garantizando que la aplicación se dibuje de borde a borde detrás de la barra de estado y de navegación.

### Actividad 20: [H1][BD] Enum SyncStatus y TypeConverters
* **`SyncStatus.kt`**: Enum que modela los 4 estados de la sincronización offline: `SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`.
* **`SyncStatusConverter.kt`**: Anotado con `@TypeConverter` para Room. No utiliza `!!`. Emplea `try-catch` y fallback automático a `SyncStatus.SYNCED` si en SQLite se encontrara un valor nulo o corrupto.
* **`ActivoLTIC.kt`**: Data class inmutable con atributos completos de hardware (serie, modelo, ram, cpu, ubicación, observaciones físicas y badges de sincronización).

### Actividad 29: [H1][LÓGICA] ViewModels y estados de UI inmutables
* **`InventoryUiState.kt`**: `sealed interface` con variantes exhaustivas: `Idle`, `Loading`, `Success(activos, filteredActivos)` y `Error(mensaje)`.
* **`InventoryViewModel.kt`**:
  * Utiliza `SavedStateHandle` para conservar `searchQuery` y `selectedCategory` frente a *Process Death* y rotaciones de pantalla (Tema 4).
  * Expone `uiState: StateFlow<InventoryUiState>` de solo lectura mediante `asStateFlow()`.
  * Filtra y procesa los datos en `Dispatchers.IO` y `Dispatchers.Default` evitando bloquear el hilo principal (cero ANRs).

### Actividad 3: [H1][UI] Pantalla de Permiso de Cámara
* **`CameraPermissionScreen.kt`**:
  * Diseñado con un `Column` centrado horizontal y verticalmente (`Arrangement.Center`, `Alignment.CenterHorizontally`).
  * Icono M3 grande en contenedor tonal circular.
  * Mensaje claro y descriptivo centralizado en `strings.xml`.
  * Botón primario ("Otorgar Permiso") listo para disparar la solicitud de permisos del sistema.

### Actividad 14: [H1][UI] Estados de Carga, Vacío y Error
* **`StateComponents.kt`**:
  * `LoadingIndicator`: Centrado dentro de `Box(Modifier.fillMaxSize())` con `CircularProgressIndicator` y mensaje dinámico.
  * `EmptyState`: Contenedor centrado con icono `SearchOff`, título y descripción sin datos, más botón para restablecer filtros.
  * `ErrorState`: Usa el color semántico `MaterialTheme.colorScheme.error` y `errorContainer` con botón "Reintentar".

### Actividad 2: [H1][UI] Navegación entre Pantallas (Navigation Compose)
* **`NavRoutes.kt`**: Rutas fuertemente tipadas anotadas con `@Serializable`:
  * `DashboardRoute` (catálogo)
  * `ScannerRoute` (cámara ML Kit)
  * `AssetDetailRoute(val assetId: String)` (ficha técnica con argumento)
  * `CameraPermissionRoute` (permiso)
* **`NavGraph.kt`**: `NavHost` único que deserializa los argumentos tipados mediante `backStackEntry.toRoute<AssetDetailRoute>()`, eliminando las cadenas mágicas propensas a errores en tiempo de ejecución.

### Actividad 9: [H1][UI] Búsqueda por Texto en el Catálogo
* **`InventoryDashboardScreen.kt` (`SearchBarSection`)**:
  * Implementa **State Hoisting**: recibe `searchQuery` y el callback `onSearchQueryChange` desde el `ViewModel`.
  * No utiliza estados mutables locales dentro del componente de búsqueda.

### Actividad 10: [H1][UI] Filtros por Categoría con FilterChip
* **`InventoryDashboardScreen.kt` (`CategoryFilterSection`)**:
  * **Uso estricto de `LazyRow`**: Cumple la prohibición de usar `Row` con `forEach`, asegurando reciclaje y alto rendimiento.
  * Componentes `FilterChip` con estado `selected` enlazado reactivamente y marca de verificación `Check`.
  * Listado general de activos implementado mediante `LazyColumn` con clave única (`key = { it.id }`), cumpliendo el wireframe de la diapositiva 5.

---

## 4. Pantallas Complementarias Desarrolladas
Para garantizar un flujo funcional completo y demostrable:
* **`AssetDetailScreen.kt`**: Ficha técnica del activo con especificaciones de hardware, campo interactivo de observaciones físicas con persistencia ante rotación mediante `rememberSaveable`, y botones de "Descartar" y "Guardar".
* **`ScannerScreen.kt`**: Visor de escaneo óptico Edge AI con retícula, linterna y trigger de simulación de detección instantánea (<30ms).
