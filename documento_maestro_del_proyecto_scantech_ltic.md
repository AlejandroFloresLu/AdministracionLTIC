# Documento Maestro: ScanTech LTIC

Este documento es la guía técnica y operativa definitiva para el desarrollo del proyecto **ScanTech LTIC**. Contiene la visión general, los requerimientos, la arquitectura, el flujo de datos y el desglose de las 8 actividades seleccionadas para el desarrollo. **El propósito principal de este documento es servir como instrucción detallada (Master Prompt) para el Agente de IA**, asegurando que cada componente se implemente de acuerdo con las normativas y buenas prácticas definidas por la cátedra de Programación Móvil.

---

## 1. Visión General del Proyecto

### 1.1 El Problema
En el departamento de Logística Tecnológica (LTIC), el proceso de levantamiento físico de inventario y recepción de hardware se realiza de manera manual. Esto genera errores al transcribir números de serie extensos, dificulta la consulta inmediata de especificaciones y complica el registro del estado físico de los equipos. Además, las bodegas a menudo carecen de conectividad, lo que imposibilita el uso de sistemas en línea tradicionales.

### 1.2 La Solución: ScanTech LTIC
ScanTech LTIC es un **Sistema Móvil Nativo de Auditoría y Trazabilidad** que transforma el dispositivo móvil en un escáner inteligente operando bajo una estrategia **Offline-First**.

### 1.3 Funcionalidades Core
El proyecto se centra en tres pilares:
1. **Escaneo Óptico Inteligente (Edge AI):** Captura automática y continua de códigos QR y de barras usando `CameraX` y Google ML Kit, sin necesidad de pulsar botones y operando 100% offline en el dispositivo.
2. **Ficha Técnica Autocompletada y Observaciones:** Al detectar un código, se consultan los datos del activo en la base local (Room) y se precargan los campos. Se incluye un campo para registrar observaciones físicas.
3. **Gestión Integral de Inventario (CRUD Offline-First):** Catálogo dinámico para consultar, crear, editar y dar de baja equipos. Todos los cambios se guardan localmente en Room y se sincronizan asíncronamente con una API REST cuando hay red.

---

## 2. Arquitectura y Stack Tecnológico

El proyecto debe regirse estrictamente por las siguientes decisiones arquitectónicas y tecnológicas (basadas en los temas 4, 5 y 7-8 del curso):

### 2.1 Stack Base
*   **Lenguaje:** Kotlin 2.0.0
*   **Target:** Java 21, API 36 (Android 16)
*   **Paradigma UI:** Jetpack Compose (Declarativo)
*   **Diseño:** Material Design 3 (M3) con Edge-to-Edge nativo y modo oscuro.

### 2.2 Arquitectura (Clean Architecture / MVVM / UDF)
*   **UI Layer (Presentación):** Pantallas en Compose que reaccionan pasivamente a los estados. La navegación será **fuertemente tipada (Type-Safe)** usando `@Serializable` con Jetpack Navigation Compose, abandonando los "strings mágicos".
*   **ViewModel Layer (Orquestación):** Los ViewModels expondrán el estado a través de `StateFlow`. **Crucial:** Para sobrevivir a la rotación de pantalla y la muerte del proceso (Process Death), los estados efímeros (como el texto de la nota) DEBEN guardarse usando `SavedStateHandle`.
*   **Data & Domain Layer:**
    *   **Modelado:** `data class` inmutables.
    *   **Estados (UI State):** Uso obligatorio de `sealed interface` para crear máquinas de estado finitas (ej. `Idle`, `Loading`, `Success`, `Error`), evaluadas con un `when` exhaustivo en Compose, blindando la app contra nulos y cierres forzados.
    *   **Persistencia:** Room (Single Source of Truth) para operación Offline-First.
    *   **Red:** Retrofit + Kotlin Coroutines (en `Dispatchers.IO` para **NUNCA** bloquear el hilo principal y evitar ANRs).
    *   **Edge AI:** Google ML Kit Barcode Scanning.

### 2.3 Optimizaciones Exigidas
*   **Listas:** Prohibido el uso de `Column` con `forEach`. Toda lista debe usar `LazyColumn` o `LazyRow` para garantizar el reciclaje de memoria y rendimiento a 60 FPS.
*   **Nulos (Null Safety):** Cero uso del operador `!!`. Utilizar conversiones seguras y operador Elvis (`?:`).

---

## 3. Desglose de las 8 Actividades a Desarrollar

A continuación, se detallan las 8 tareas iniciales seleccionadas. **Instrucciones para el Agente:** Debes leer cuidadosamente la descripción técnica de cada tarea antes de generar el código.

### Tarea #1: [H1][UI] Configurar tema Material 3 de ScanTech

*   **Objetivo:** Establecer la base visual de la aplicación configurando el tema de Material Design 3 en el proyecto.
*   **Archivos implicados:** `ui/theme/Color.kt`, `ui/theme/Theme.kt`.
*   **Instrucciones de Implementación:**
    1.  Define los colores base en `Color.kt` (Primario, Secundario, Terciario, Fondos) para los esquemas claro y oscuro.
    2.  En `Theme.kt`, asegura que la función composable principal del tema (ej. `ScanTechTheme`) utilice `dynamicLightColorScheme` y `dynamicDarkColorScheme` (si `dynamicColor` es true, soportado desde API 31+). Si no, debe hacer el "fallback" a los colores estáticos definidos.
    3.  Aplica el tema a la `Activity` principal asegurando la compatibilidad con *Edge-to-Edge* (contenido que se dibuja detrás de las barras de estado y navegación).

### Tarea #20: [H1][BD] Enum SyncStatus y TypeConverters

*   **Objetivo:** Preparar la base de datos (Room) para manejar el estado de sincronización de los activos, vital para la estrategia Offline-First.
*   **Archivos implicados:** `data/local/SyncStatus.kt`, `data/local/SyncStatusConverter.kt`.
*   **Instrucciones de Implementación:**
    1.  Crea un `enum class` llamado `SyncStatus` con los estados: `SYNCED` (sincronizado), `PENDING_CREATE` (creado localmente, pendiente de subir), `PENDING_UPDATE` (editado localmente, pendiente de subir), `PENDING_DELETE` (borrado localmente, pendiente de borrar en servidor).
    2.  Crea una clase `SyncStatusConverter` con métodos anotados con `@TypeConverter`.
    3.  Implementa la lógica para convertir de `SyncStatus` a `String` (o `Int`) y viceversa, para que Room sepa cómo persistir este dato. Incluye manejo seguro ante nulos o valores desconocidos en la conversión de vuelta al enum.

### Tarea #29: [H1][LÓGICA] ViewModels y estados de UI inmutables

*   **Objetivo:** Definir el esqueleto reactivo del núcleo de la aplicación (el Inventario) usando el patrón UDF (Unidirectional Data Flow).
*   **Archivos implicados:** `ui/inventory/InventoryUiState.kt` (o similar), `ui/inventory/InventoryViewModel.kt`.
*   **Instrucciones de Implementación:**
    1.  Crea una `sealed interface` llamada `InventoryUiState`.
    2.  Define los estados finitos: `Idle` (data object), `Loading` (data object), `Success` (data class que contenga una lista de activos, ej. `val activos: List<ActivoLTIC>`), y `Error` (data class con `val mensaje: String`).
    3.  Crea la clase `InventoryViewModel` que herede de `ViewModel`. Define un `MutableStateFlow` interno (`_uiState`) inicializado en `Idle` y expónlo como un `StateFlow` público (`uiState`) de solo lectura.

### Tarea #3: [H1][UI] Pantalla de permiso de cámara

*   **Objetivo:** Crear la interfaz que solicita al usuario el permiso necesario para que ML Kit pueda operar.
*   **Archivos implicados:** `ui/permissions/CameraPermissionScreen.kt`.
*   **Instrucciones de Implementación:**
    1.  Diseña una función composable limpia y centrada.
    2.  Utiliza una `Column` con alineación central (`Arrangement.Center`, `Alignment.CenterHorizontally`).
    3.  Incluye un icono grande (ej. `Icons.Rounded.CameraAlt`), un texto explicativo claro ("ScanTech requiere acceso a la cámara para auditar los códigos de los equipos") y un `Button` principal ("Otorgar Permiso"). *Nota: La lógica real de Accompanist Permissions se añadirá luego; por ahora, el botón puede recibir un evento `onClick` vacío.*

### Tarea #14: [H1][UI] Estados de carga, vacío y error

*   **Objetivo:** Modularizar los componentes de feedback visual que reaccionarán a la máquina de estados definida en la Tarea #29.
*   **Archivos implicados:** `ui/components/StateComponents.kt`.
*   **Instrucciones de Implementación:**
    1.  Crea un composable `LoadingIndicator()` que envuelva un `CircularProgressIndicator` dentro de un `Box` centrado que ocupe todo el espacio (`fillMaxSize()`).
    2.  Crea un composable `EmptyState(mensaje: String)` que muestre una ilustración/icono y un texto indicando que no hay datos.
    3.  Crea un composable `ErrorState(mensaje: String, onRetry: () -> Unit)` que muestre el texto de error (usando colores semánticos como `MaterialTheme.colorScheme.error`) y un botón para reintentar la acción.

### Tarea #2: [H1][UI] Navegación entre pantallas (Navigation Compose)

*   **Objetivo:** Configurar el esqueleto de navegación utilizando la arquitectura de **Navegación Tipada (Type-Safe Navigation)** exigida para API 36.
*   **Archivos implicados:** `MainActivity.kt` (o `ui/navigation/NavGraph.kt`), clases de rutas.
*   **Instrucciones de Implementación:**
    1.  Define los destinos usando la anotación `@Serializable`. Crea un `object DashboardRoute` (para el catálogo) y un `object ScannerRoute` (para la cámara). Crea una `data class AssetDetailRoute(val assetId: String)` para la pantalla de detalle.
    2.  En el Composable principal (dentro del `NavHost`), configura el `navController`.
    3.  Usa la función `composable<Destino>` para cada ruta. *Por ahora, dentro de cada bloque, puedes llamar a composables provisionales (Textos) o a la pantalla de Permiso de Cámara creada en la Tarea #3.*

### Tarea #9: [H1][UI] Búsqueda por texto en el Catálogo

*   **Objetivo:** Implementar la interfaz de la barra de búsqueda en el catálogo, usando el patrón de *State Hoisting*.
*   **Archivos implicados:** `ui/inventory/InventoryDashboardScreen.kt` (o similar).
*   **Instrucciones de Implementación:**
    1.  Integra el componente `SearchBar` de Material Design 3 (o en su defecto un `OutlinedTextField` con icono de lupa) en la parte superior del `Scaffold`.
    2.  Aplica *State Hoisting*: El texto de búsqueda (query) y el evento `onQueryChange` deben ser pasados como parámetros al composable desde un nivel superior. **No** declares la variable mutable (`remember`) dentro del propio componente de búsqueda.

### Tarea #10: [H1][UI] Filtros por categoría con FilterChip

*   **Objetivo:** Añadir filtros rápidos (ej. Laptops, Servidores) debajo de la barra de búsqueda utilizando el componente optimizado correcto.
*   **Archivos implicados:** `ui/inventory/InventoryDashboardScreen.kt` (o similar).
*   **Instrucciones de Implementación:**
    1.  Implementa una lista horizontal de opciones. **Obligatorio:** Debes utilizar `LazyRow` para garantizar el reciclaje de memoria. Prohibido usar `Row` con un ciclo `forEach`.
    2.  Dentro del `items` del `LazyRow`, renderiza componentes `FilterChip` de Material 3.
    3.  Asegúrate de que el estado visual del chip (`selected`) reaccione a una variable de estado que identifique la categoría activa actual.