# 📱 Banco de 30 Casos de Cambio — Defensa Examen Final Fase 1
## ScanTech LTIC · PUCE · Programación Móvil

> **Uso exclusivo de preparación para defensa oral/práctica.**
> Todos los archivos referenciados son reales del repositorio `AdministracionLTIC`, rama `Paúl`.
> Tiempo estimado por caso: **10–15 minutos**.

---

## 🧩 BLOQUE 1 — Kotlin Idiomático y Null Safety (Casos 1–5)

---

### Caso 1 — Validar QR con `.takeIf` y `.let` en lugar de if-else anidado

**📝 Descripción del Problema**

El validador de activos LTIC verifica el formato del código QR con una estructura `when` que contiene tres ramas anidadas. El docente solicita refactorizar la comprobación del formato usando las funciones de alcance idiomáticas de Kotlin (`.takeIf` y `.let`) para que el flujo sea más expresivo y funcional.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/domain/validacion/ValidadorActivo.kt`

1. Localiza el bloque `when` que valida `datos.qrCode`:
   ```kotlin
   when {
       datos.qrCode.isBlank() -> errores[CampoActivo.QR_CODE] = "El código QR es obligatorio"
       !FORMATO_QR.matches(datos.qrCode.trim()) ->
           errores[CampoActivo.QR_CODE] = "Formato esperado: LTIC-000123"
       qrYaExiste -> errores[CampoActivo.QR_CODE] = "Ya existe un activo con este código"
   }
   ```
2. Reemplázalo con una cadena `.takeIf` + `.let`:
   ```kotlin
   datos.qrCode.trim()
       .takeIf { it.isNotBlank() }
       ?.let { qr ->
           if (!FORMATO_QR.matches(qr))
               errores[CampoActivo.QR_CODE] = "Formato esperado: LTIC-000123"
           else if (qrYaExiste)
               errores[CampoActivo.QR_CODE] = "Ya existe un activo con este código"
       }
       ?: run { errores[CampoActivo.QR_CODE] = "El código QR es obligatorio" }
   ```
3. Verifica que el proyecto compile: `./gradlew assembleDebug`.

**🎯 Salida Esperada en Emulador**

- El formulario de registro sigue rechazando QR vacíos con el mensaje "El código QR es obligatorio".
- Un QR con formato `LTIC-ABC` sigue mostrando "Formato esperado: LTIC-000123" bajo el campo.
- Un QR duplicado (`LTIC-000001`) sigue mostrando "Ya existe un activo con este código".

**💡 Pregunta Teórica de Examen**

> ¿Qué diferencia semántica existe entre `.let {}` y `.also {}` en Kotlin? ¿Por qué en este caso se eligió `.let` y no `.also` para transformar el código QR antes de validarlo? Menciona el tipo que retorna cada función.

---

### Caso 2 — Conversión segura de stock con `toIntOrNull()` y operador Elvis

**📝 Descripción del Problema**

El campo "Unidades" del formulario es un `String` que el usuario escribe libremente. Actualmente `ValidadorActivo.kt` usa `toIntOrNull()` para convertirlo. El docente pide que expliques este patrón y lo extiendas: si la conversión falla (`null`), el error en pantalla debe incluir el valor exacto que el usuario escribió para ayudarlo a corregirlo.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/domain/validacion/ValidadorActivo.kt`

1. Localiza el bloque de validación de unidades:
   ```kotlin
   val unidades = datos.units.trim().toIntOrNull()
   when {
       unidades == null -> errores[CampoActivo.UNITS] = "Ingresa un número entero"
       unidades < 0 -> errores[CampoActivo.UNITS] = "Las unidades no pueden ser negativas"
   }
   ```
2. Modifica el mensaje de error `null` para incluir el valor escrito:
   ```kotlin
   val unidades = datos.units.trim().toIntOrNull()
   when {
       unidades == null -> errores[CampoActivo.UNITS] =
           "\"${datos.units.trim()}\" no es un número entero válido"
       unidades < 0 -> errores[CampoActivo.UNITS] = "Las unidades no pueden ser negativas"
       unidades > 9999 -> errores[CampoActivo.UNITS] = "Máximo 9 999 unidades por activo"
   }
   ```
3. Compila y prueba escribiendo "abc" en el campo Unidades del formulario.

**🎯 Salida Esperada en Emulador**

- Al escribir "abc" en Unidades, el `supportingText` bajo el campo muestra: `"abc" no es un número entero válido`.
- Al escribir "10000", muestra: `Máximo 9 999 unidades por activo`.
- El botón Guardar permanece deshabilitado mientras exista cualquier error.

**💡 Pregunta Teórica de Examen**

> ¿Por qué `toIntOrNull()` es preferible a `toInt()` en un formulario de usuario? ¿Qué excepción lanzaría `toInt()` si el texto no es un número, y cómo afecta eso a la experiencia del usuario en Android?

---

### Caso 3 — Función de extensión `String.esCodigoQrLtic(): Boolean`

**📝 Descripción del Problema**

La regex de validación QR `^LTIC-\d{6}$` está encapsulada en `ValidadorActivo.kt` como una propiedad privada. El docente pide extraerla como una **función de extensión** pública en un nuevo archivo `QrExtensions.kt` dentro del paquete `domain/validacion/`, de modo que pueda reutilizarse desde cualquier parte del dominio sin duplicar la lógica.

**🛠️ Pasos de Cambio en el Código**

1. Crea el archivo `app/src/main/java/ec/edu/puce/scantech/domain/validacion/QrExtensions.kt`:
   ```kotlin
   package ec.edu.puce.scantech.domain.validacion

   /** Regex oficial para los activos del departamento LTIC-PUCE. */
   private val FORMATO_QR_LTIC = Regex("^LTIC-\\d{6}$")

   /**
    * Retorna `true` si este String cumple el patrón LTIC-000000.
    * Función de extensión pura: sin dependencias de Android.
    */
   fun String.esCodigoQrLtic(): Boolean = FORMATO_QR_LTIC.matches(this.trim())
   ```
2. En `ValidadorActivo.kt`, elimina `private val FORMATO_QR = Regex(...)` y reemplaza:
   ```kotlin
   !FORMATO_QR.matches(datos.qrCode.trim())
   // por:
   !datos.qrCode.esCodigoQrLtic()
   ```
3. Verifica con grep que `FORMATO_QR` ya no exista en `ValidadorActivo.kt`.

**🎯 Salida Esperada en Emulador**

- El comportamiento de validación del formulario no cambia visualmente.
- `QrExtensions.kt` aparece en el árbol del proyecto sin ningún import de `android.*`.

**💡 Pregunta Teórica de Examen**

> ¿Qué son las funciones de extensión en Kotlin y cómo se diferencian de los métodos de clase heredados? ¿Pueden las funciones de extensión acceder a los miembros privados de la clase que extienden?

---

### Caso 4 — Sanitizar texto con la función de alcance `.run {}`

**📝 Descripción del Problema**

En `AssetFormViewModel.kt`, el campo `technicalSpecs` se guarda tal como lo escribe el usuario (con espacios al inicio/final y en minúsculas). El docente pide que antes de crear el objeto `Asset`, las especificaciones técnicas se saneen usando la función de alcance `.run {}` para eliminar espacios y convertir a mayúsculas el texto.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/form/AssetFormViewModel.kt`

1. Localiza el bloque donde se construye el objeto `Asset` dentro de `fun guardar()` o similar.
2. Aplica `.run {}` sobre el campo de especificaciones técnicas:
   ```kotlin
   val asset = Asset(
       id = assetId ?: UUID.randomUUID().toString(),
       qrCode = savedStateHandle[KEY_QR] ?: "",
       name = (savedStateHandle[KEY_NAME] ?: "").trim(),
       category = savedStateHandle[KEY_CATEGORY] ?: "",
       technicalSpecs = (savedStateHandle[KEY_SPECS] ?: "").run {
           trim().uppercase()   // Sanitización antes de persistir en Room
       },
       // ... resto de campos
   )
   ```
3. Prueba escribiendo " laptop hp " en especificaciones y guarda el activo.

**🎯 Salida Esperada en Emulador**

- En la pantalla `AssetDetailScreen`, el campo "Especificaciones Técnicas" muestra `LAPTOP HP` (sin espacios y en mayúsculas).

**💡 Pregunta Teórica de Examen**

> Enumera las cinco funciones de alcance de Kotlin (`.let`, `.run`, `.with`, `.apply`, `.also`) y explica: ¿cuál retorna el resultado del lambda y cuál retorna el objeto receptor? ¿Cuándo es apropiado usar `.run` versus `.let`?

---

### Caso 5 — Desestructuración de `data class Asset` para logging

**📝 Descripción del Problema**

En `AssetDetailScreen.kt`, el `Asset` recibido se accede campo por campo para mostrar los datos. El docente pide agregar una función de log de auditoría que use **desestructuración de data class** para extraer los tres primeros campos del activo en una sola declaración y construir una cadena de log.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/detail/AssetDetailScreen.kt`

1. En el bloque `is AssetDetailUiState.Success -> { ... }`, agrega al inicio:
   ```kotlin
   // Desestructuración de data class: extrae los 3 primeros campos declarados en Asset.kt
   // Orden: id, qrCode, name (componentN() generado automáticamente por el compilador)
   val (assetUid, qr, nombre) = uiState.asset
   android.util.Log.d(
       "ScanTech_Audit",
       "Ficha abierta → ID=$assetUid | QR=$qr | Nombre=$nombre"
   )
   ```
2. Verifica que el proyecto compile y que el log aparezca en Logcat al abrir un activo.

**🎯 Salida Esperada en Emulador**

- En Logcat con filtro `ScanTech_Audit`, al abrir cualquier activo aparece: `Ficha abierta → ID=<uuid> | QR=LTIC-000001 | Nombre=Laptop HP`.

**💡 Pregunta Teórica de Examen**

> ¿Cómo funciona la desestructuración de `data class` en Kotlin a nivel de bytecode? ¿Qué funciones genera automáticamente el compilador para habilitar esta característica y cómo se llaman? ¿Es posible desestructurar una clase que no sea `data class`?

---

## 🗺️ BLOQUE 2 — Rutas Tipadas `@Serializable` y Navegación (Casos 6–10)

---

### Caso 6 — Agregar parámetro `modoAuditoria: Boolean` a `AssetDetailRoute`

**📝 Descripción del Problema**

El módulo de detalle de activos siempre se muestra en modo lectura. Se requiere un parámetro booleano `modoAuditoria` que, cuando es `true`, deshabilite la opción de editar. El docente pide agregar este parámetro a la ruta tipada y propagarlo hasta la pantalla.

**🛠️ Pasos de Cambio en el Código**

Archivo 1: `app/src/main/java/ec/edu/puce/scantech/ui/navigation/NavRoutes.kt`
```kotlin
@Serializable
data class AssetDetailRoute(
    val assetId: String,
    val modoAuditoria: Boolean = false   // <-- Nuevo parámetro con valor por defecto
) : NavRoute
```

Archivo 2: `app/src/main/java/ec/edu/puce/scantech/ui/navigation/NavGraph.kt`
```kotlin
composable<NavRoute.AssetDetailRoute> { backStackEntry ->
    val routeArgs = backStackEntry.toRoute<NavRoute.AssetDetailRoute>()
    AssetDetailScreen(
        assetId = routeArgs.assetId,
        modoAuditoria = routeArgs.modoAuditoria,  // <-- Propagar
        onNavigateBack = { navController.popBackStack() },
        onNavigateToEdit = { editId ->
            navController.navigate(NavRoute.AssetFormRoute(assetId = editId))
        }
    )
}
```

Archivo 3: `AssetDetailScreen.kt` — Agrega el parámetro y condiciona el botón Editar:
```kotlin
@Composable
fun AssetDetailScreen(
    assetId: String,
    modoAuditoria: Boolean = false,
    ...
) {
    // ...
    if (!modoAuditoria) {
        FloatingActionButton(onClick = { onNavigateToEdit(assetId) }) {
            Icon(Icons.Default.Edit, contentDescription = "Editar activo")
        }
    }
}
```

**🎯 Salida Esperada en Emulador**

- Al navegar con `modoAuditoria = false` (normal), el FAB de edición es visible.
- Al navegar con `modoAuditoria = true`, el FAB desaparece sin romper la pantalla.

**💡 Pregunta Teórica de Examen**

> ¿Por qué Navigation Compose con rutas `@Serializable` es superior a las rutas basadas en strings como `"asset/{id}"`? Menciona al menos dos ventajas concretas en términos de seguridad de tipos y mantenibilidad.

---

### Caso 7 — Implementar `popUpTo` al guardar en el formulario

**📝 Descripción del Problema**

Actualmente, después de guardar un activo en `AssetFormScreen`, se hace `navController.popBackStack()`. Esto deja el formulario en el back stack. Si el usuario navega "atrás" varias veces, podría regresar al formulario vacío. El docente pide usar `popUpTo` para limpiar el formulario del back stack al guardar exitosamente.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/navigation/NavGraph.kt`

Localiza el bloque del formulario y modifica `onSaveSuccess`:
```kotlin
composable<NavRoute.AssetFormRoute> { backStackEntry ->
    val routeArgs = backStackEntry.toRoute<NavRoute.AssetFormRoute>()
    AssetFormScreen(
        assetId = routeArgs.assetId,
        onNavigateBack = { navController.popBackStack() },
        onSaveSuccess = {
            // popUpTo elimina el formulario del back stack y retorna al Dashboard
            // inclusive = true elimina también el destino especificado (el propio formulario)
            navController.navigate(NavRoute.DashboardRoute) {
                popUpTo<NavRoute.DashboardRoute> { inclusive = false }
            }
        }
    )
}
```

**🎯 Salida Esperada en Emulador**

- Al guardar un activo nuevo, la app navega al Dashboard.
- Al presionar el botón físico "Atrás" desde el Dashboard, la app se cierra (el formulario no está en el back stack).

**💡 Pregunta Teórica de Examen**

> ¿Qué significa `inclusive = true` en `popUpTo`? Si en este caso usáramos `inclusive = true` apuntando a `DashboardRoute`, ¿qué destino quedaría visible? Explica la diferencia entre `popUpTo` y `popBackStack()`.

---

### Caso 8 — Crear ruta `HistorialBajasRoute` y registrarla en el grafo

**📝 Descripción del Problema**

El inventario LTIC requiere una pantalla de historial de bajas lógicas. El docente pide crear la ruta tipada `HistorialBajasRoute`, registrarla en el grafo de navegación y agregar un botón en el Dashboard que navegue a ella.

**🛠️ Pasos de Cambio en el Código**

Archivo 1: `NavRoutes.kt` — Añade al final de `sealed interface NavRoute`:
```kotlin
/** Pantalla de historial de activos dados de baja lógica (deletedAt != null). */
@Serializable
data object HistorialBajasRoute : NavRoute
```

Archivo 2: `NavGraph.kt` — Añade un nuevo `composable`:
```kotlin
composable<NavRoute.HistorialBajasRoute> {
    // Placeholder hasta que se implemente la pantalla completa
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Historial de Bajas — Próximamente",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
```

Archivo 3: `InventoryDashboardScreen.kt` — Agrega un `IconButton` en el `TopAppBar`:
```kotlin
actions = {
    IconButton(onClick = { onNavigateToHistorial() }) {
        Icon(Icons.Default.History, contentDescription = "Historial de bajas")
    }
}
```

**🎯 Salida Esperada en Emulador**

- El Dashboard muestra un ícono de historial en la barra superior.
- Al pulsarlo, navega a una pantalla con el texto "Historial de Bajas — Próximamente".

**💡 Pregunta Teórica de Examen**

> ¿Cuál es la diferencia entre `data object` y `data class` al definir una ruta de navegación `@Serializable`? ¿En qué casos debes usar uno u otro y por qué?

---

### Caso 9 — Extraer y reusar la lógica de `toRoute<T>()` para evitar repetición

**📝 Descripción del Problema**

En `NavGraph.kt`, la extracción de argumentos con `backStackEntry.toRoute<NavRoute.AssetDetailRoute>()` se repite en cada `composable`. El docente pide crear una **función inline de extensión** sobre `NavBackStackEntry` que simplifique la extracción y sea type-safe.

**🛠️ Pasos de Cambio en el Código**

Archivo: `NavGraph.kt` (o un nuevo `NavExtensions.kt` en el paquete `navigation`):
```kotlin
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute

/** Extrae la ruta tipada T del back stack de forma concisa y sin boilerplate. */
inline fun <reified T : NavRoute> NavBackStackEntry.route(): T = toRoute<T>()
```

Luego en `NavGraph.kt`, simplifica:
```kotlin
// Antes:
composable<NavRoute.AssetDetailRoute> { backStackEntry ->
    val routeArgs = backStackEntry.toRoute<NavRoute.AssetDetailRoute>()
    ...
}

// Después:
composable<NavRoute.AssetDetailRoute> { backStackEntry ->
    val routeArgs = backStackEntry.route<NavRoute.AssetDetailRoute>()
    ...
}
```

**🎯 Salida Esperada en Emulador**

- El comportamiento de navegación no cambia. La app compila sin errores.
- El código del grafo es más legible.

**💡 Pregunta Teórica de Examen**

> ¿Qué significa el modificador `inline` en Kotlin y por qué es necesario cuando se usa `reified T`? ¿Qué limitación tienen los genéricos en la JVM que `reified` resuelve?

---

### Caso 10 — Verificar que la navegación al escáner limpia la pantalla de permisos

**📝 Descripción del Problema**

Cuando el usuario concede el permiso de cámara en `CameraPermissionScreen`, la app navega a `ScannerRoute`. Sin embargo, si el usuario presiona "Atrás" desde el escáner, regresa a la pantalla de permisos (que ya no es necesaria). El docente pide verificar y documentar por qué el `popUpTo` actual en `NavGraph.kt` resuelve esto.

**🛠️ Pasos de Cambio en el Código**

Archivo: `NavGraph.kt` — Verifica que el bloque de permisos ya tenga:
```kotlin
onRequestPermission = {
    navController.navigate(NavRoute.ScannerRoute) {
        popUpTo<NavRoute.CameraPermissionRoute> { inclusive = true }
    }
}
```

Agrega un comentario explicativo detallado encima de ese bloque:
```kotlin
// SSOT Navigation Pattern:
// Al conceder el permiso, 'CameraPermissionRoute' se elimina del back stack (inclusive=true).
// Esto garantiza que el botón físico "Atrás" desde el escáner NO regrese a la pantalla de permisos,
// sino al Dashboard. Sin este popUpTo, el flujo sería: Scanner → Permisos → Dashboard (incorrecto).
onRequestPermission = { ... }
```

**🎯 Salida Esperada en Emulador**

- Concede permiso → navega al escáner.
- Presiona "Atrás" desde el escáner → regresa al Dashboard (no a permisos).

**💡 Pregunta Teórica de Examen**

> ¿Qué es el "Back Stack" en Android Navigation Compose y quién es responsable de gestionarlo? Describe un escenario donde un mal manejo del back stack produzca una experiencia de usuario confusa o un comportamiento inesperado.

---

## 🎨 BLOQUE 3 — UI Compose y Material Design 3 (Casos 11–15)

---

### Caso 11 — Reemplazar `ElevatedCard` con color `tertiaryContainer`

**📝 Descripción del Problema**

En `AssetDetailScreen.kt`, la tarjeta de localización del activo (Bloque, Estante, Casillero) usa el color de contenedor por defecto. El docente pide que use explícitamente `MaterialTheme.colorScheme.tertiaryContainer` para el fondo y `onTertiaryContainer` para el texto, reforzando el sistema de roles de color de Material Design 3.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/detail/AssetDetailScreen.kt`

Localiza la `ElevatedCard` o `Card` de localización y agrega:
```kotlin
ElevatedCard(
    colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ),
    modifier = Modifier.fillMaxWidth()
) {
    // Contenido: Bloque, Estante, Casillero
    Text(
        text = asset.location,  // "Bloque A · Estante 3 · Casillero 2"
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onTertiaryContainer
    )
}
```

**🎯 Salida Esperada en Emulador**

- La tarjeta de localización muestra un tono terciario distintivo (diferente al fondo general).
- En modo oscuro, el color cambia automáticamente al complemento oscuro de `tertiaryContainer`.

**💡 Pregunta Teórica de Examen**

> ¿Qué es el sistema de roles de color en Material Design 3 (`primary`, `secondary`, `tertiary`, `surface`, etc.)? ¿Por qué se recomienda usar roles semánticos en lugar de colores hexadecimales fijos (`Color(0xFF...)`) en el código Compose?

---

### Caso 12 — Deshabilitar botón "Guardar" con `enabled` reactivo

**📝 Descripción del Problema**

El botón "Guardar Activo" en `AssetFormScreen.kt` siempre aparece habilitado, incluso cuando el formulario tiene errores. El docente pide vincularlo al estado de validación del ViewModel para que se deshabilite automáticamente mientras existan errores en cualquier campo.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/form/AssetFormScreen.kt`

1. En el `AssetFormContent` (composable stateless), agrega el parámetro:
   ```kotlin
   @Composable
   fun AssetFormContent(
       uiState: AssetFormUiState,
       onGuardar: () -> Unit,
       // ...
   ) {
       val hayErrores = uiState.errores.isNotEmpty()

       Button(
           onClick = onGuardar,
           enabled = !hayErrores,   // Reactivo al estado de validación
           modifier = Modifier.fillMaxWidth()
       ) {
           Text(if (uiState.modoEdicion) "Actualizar Activo" else "Guardar Activo")
       }
   }
   ```
2. Verifica que `uiState.errores` sea del tipo `Map<CampoActivo, String>` (ya definido en el ViewModel).

**🎯 Salida Esperada en Emulador**

- Al abrir el formulario vacío, el botón Guardar está deshabilitado (gris).
- Al completar todos los campos correctamente, el botón se habilita automáticamente.

**💡 Pregunta Teórica de Examen**

> ¿Qué es el Flujo de Datos Unidireccional (UDF) en Compose? Dibuja o describe el ciclo: `Events → ViewModel → State → UI`. ¿Por qué en Compose los composables deben ser funciones puras que no guardan estado mutable directamente?

---

### Caso 13 — Aplicar `RobotoMono` al campo QR en `AssetDetailScreen`

**📝 Descripción del Problema**

El código QR de un activo (`LTIC-000001`) se muestra con la tipografía genérica. El docente pide aplicar la familia de fuente monoespaciada `RobotoMono` (ya configurada en `Type.kt`) al texto del QR para distinguirlo visualmente de los campos de texto normal.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/detail/AssetDetailScreen.kt`

1. Verifica que `Type.kt` exponga la familia `RobotoMonoFontFamily` (si no existe, créala).
2. Localiza donde se muestra `asset.qrCode` y agrega la familia:
   ```kotlin
   Text(
       text = asset.qrCode,
       style = MaterialTheme.typography.bodyLarge.copy(
           fontFamily = RobotoMonoFontFamily  // Importado desde ui/theme/Type.kt
       ),
       color = MaterialTheme.colorScheme.onSurface
   )
   ```

**🎯 Salida Esperada en Emulador**

- El texto `LTIC-000001` aparece en fuente monoespaciada, claramente distinguible del resto del texto.

**💡 Pregunta Teórica de Examen**

> ¿Cómo funciona la descarga bajo demanda de Google Fonts en Android (`ui-text-google-fonts`)? ¿Qué archivo de recursos XML es necesario para garantizar la autenticidad del proveedor de fuentes y qué contiene?

---

### Caso 14 — Extraer `AlertDialog` de baja como composable `ConfirmarBajaDialog`

**📝 Descripción del Problema**

En `AssetDetailScreen.kt`, el diálogo de confirmación de baja está incrustado directamente en el composable principal, lo que lo hace difícil de reutilizar y probar. El docente pide extraerlo como un composable **stateless** independiente llamado `ConfirmarBajaDialog`.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/detail/AssetDetailScreen.kt`

Extrae el `AlertDialog` a una función separada al final del archivo:
```kotlin
/**
 * Composable Stateless: recibe estado y eventos, no guarda nada internamente.
 * Principio de Elevación de Estado (State Hoisting) aplicado a un diálogo modal.
 */
@Composable
fun ConfirmarBajaDialog(
    nombreActivo: String,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Baja") },
        text = {
            Text("¿Seguro que deseas dar de baja \"$nombreActivo\"? Esta acción no se puede deshacer desde la app.")
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text("Dar de Baja", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
```

Luego en `AssetDetailScreen` usa: `if (mostrarDialogoBaja) { ConfirmarBajaDialog(...) }`.

**🎯 Salida Esperada en Emulador**

- El diálogo de confirmación de baja funciona exactamente igual que antes.
- La función `ConfirmarBajaDialog` es un composable independiente y reutilizable.

**💡 Pregunta Teórica de Examen**

> ¿Qué es la "Elevación de Estado" (State Hoisting) en Jetpack Compose? ¿Cuál es la diferencia entre un composable Stateful y uno Stateless, y por qué los composables stateless son más fáciles de probar y reutilizar?

---

### Caso 15 — Proteger borrador de observaciones con `rememberSaveable`

**📝 Descripción del Problema**

En `AssetDetailScreen.kt`, el usuario puede editar las observaciones de un activo desde un `TextField` local. Si el usuario rota el dispositivo antes de guardar, el texto escrito se pierde porque usa `remember` en lugar de `rememberSaveable`. El docente pide corregir esto.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/detail/AssetDetailScreen.kt`

1. Localiza la variable de estado del campo de observaciones:
   ```kotlin
   // ANTES (se pierde al rotar):
   var borradorObservaciones by remember { mutableStateOf(asset.observations ?: "") }
   ```
2. Reemplaza con `rememberSaveable` usando la `key` del activo:
   ```kotlin
   // DESPUÉS (sobrevive a rotación; la key evita mezclar borradores de distintos activos):
   var borradorObservaciones by rememberSaveable(asset.id) {
       mutableStateOf(asset.observations ?: "")
   }
   ```
3. Prueba: escribe texto en observaciones → rota el dispositivo → el texto debe persistir.

**🎯 Salida Esperada en Emulador**

- El texto escrito en observaciones sobrevive a la rotación del dispositivo.
- Al navegar a otro activo y regresar, el borrador corresponde al activo correcto.

**💡 Pregunta Teórica de Examen**

> ¿Cuál es la diferencia entre `remember` y `rememberSaveable` en Jetpack Compose? ¿Qué mecanismo usa `rememberSaveable` internamente para sobrevivir a la rotación de pantalla, y qué tipos de datos puede guardar sin configuración adicional?

---

## 🧠 BLOQUE 4 — ViewModels, UDF y Ciclo de Vida (Casos 16–20)

---

### Caso 16 — Sobrescribir `onCleared()` en `AssetDetailViewModel` para auditoría

**📝 Descripción del Problema**

No hay registro de cuándo el ViewModel de detalle es destruido (por ejemplo, cuando el usuario navega atrás). El docente pide sobrescribir `onCleared()` para emitir un log de auditoría indicando qué activo estaba siendo visualizado cuando el ViewModel fue destruido.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/detail/AssetDetailViewModel.kt`

```kotlin
class AssetDetailViewModel(...) : ViewModel() {

    // ... código existente ...

    /**
     * [RDA-1 Tema 4: Ciclo de Vida del ViewModel]
     * onCleared() se llama cuando el ViewModel es destruido definitivamente
     * (el usuario navega atrás o la Activity termina). NO se llama en rotaciones.
     * Es el lugar correcto para liberar recursos costosos.
     */
    override fun onCleared() {
        super.onCleared()
        val assetActual = (_uiState.value as? AssetDetailUiState.Success)?.asset
        android.util.Log.i(
            "ScanTech_Lifecycle",
            "AssetDetailViewModel destruido. Último activo visto: ${assetActual?.name ?: "ninguno"} (ID: ${assetActual?.id ?: "N/A"})"
        )
    }
}
```

**🎯 Salida Esperada en Emulador**

- Al presionar "Atrás" desde la pantalla de detalle, Logcat muestra el mensaje de auditoría con el nombre del activo.

**💡 Pregunta Teórica de Examen**

> ¿Cuándo se llama `onCleared()` de un ViewModel y cuándo NO se llama? ¿Por qué el ViewModel sobrevive a los cambios de configuración (rotación de pantalla) pero no a la navegación hacia atrás?

---

### Caso 17 — Verificar persistencia de `KEY_QR` en `SavedStateHandle`

**📝 Descripción del Problema**

En `AssetFormViewModel.kt`, el campo `qrCode` ya tiene la clave `KEY_QR = "form_qr_code"`. El docente pide verificar que el guardado en `SavedStateHandle` ocurre en cada cambio de campo (no solo al guardar) y agregar un log que confirme la escritura en la clave.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/form/AssetFormViewModel.kt`

1. Localiza la función `fun onQrCodeChange(value: String)` o similar.
2. Agrega un log que confirme la escritura:
   ```kotlin
   fun onQrCodeChange(value: String) {
       savedStateHandle[KEY_QR] = value
       android.util.Log.v("ScanTech_SSH", "SavedStateHandle[$KEY_QR] = '$value'")
       // La validación se dispara automáticamente al actualizar _uiState
       validarFormulario()
   }
   ```
3. Prueba: escribe `LTIC-` en el campo QR, rota el dispositivo. El campo debe recuperar `LTIC-`.

**🎯 Salida Esperada en Emulador**

- El texto del campo QR sobrevive a la rotación de pantalla (Process Death con `adb shell am kill ec.edu.puce.scantech`).
- Logcat muestra cada cambio en el campo QR con la clave `form_qr_code`.

**💡 Pregunta Teórica de Examen**

> ¿Qué es `SavedStateHandle` y en qué se diferencia del simple `ViewModel`? ¿Qué escenarios de destrucción de la UI sobrevive `SavedStateHandle` que un campo regular de ViewModel no sobreviviría?

---

### Caso 18 — Añadir estado `Guardando` a `AssetFormUiState` con `when` exhaustivo

**📝 Descripción del Problema**

El formulario no muestra ningún indicador visual mientras el activo se está guardando en Room (operación de I/O). El docente pide agregar un estado `Guardando` al `sealed interface` del formulario y asegurar que todos los `when` sobre ese estado sean exhaustivos.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/form/AssetFormUiState.kt` (o donde esté definido):
```kotlin
sealed interface AssetFormUiState {
    data object Inicial : AssetFormUiState
    data object Guardando : AssetFormUiState   // <-- Nuevo estado
    data object GuardadoExitosamente : AssetFormUiState
    data class Error(val mensaje: String) : AssetFormUiState
    data class Editando(/* campos del formulario */) : AssetFormUiState
}
```

En el ViewModel, antes de llamar al caso de uso:
```kotlin
fun guardar() {
    _uiState.update { AssetFormUiState.Guardando }  // Muestra progreso
    viewModelScope.launch(Dispatchers.IO) {
        val result = guardarActivoUseCase(/* ... */)
        // ...
    }
}
```

En el composable, maneja el nuevo estado:
```kotlin
when (val estado = uiState) {
    is AssetFormUiState.Guardando -> CircularProgressIndicator()
    is AssetFormUiState.GuardadoExitosamente -> LaunchedEffect(Unit) { onSaveSuccess() }
    // ... resto de estados
}
```

**🎯 Salida Esperada en Emulador**

- Al pulsar "Guardar", aparece un `CircularProgressIndicator` brevemente antes de navegar atrás.

**💡 Pregunta Teórica de Examen**

> ¿Por qué un `sealed interface` (o `sealed class`) garantiza exhaustividad en las expresiones `when` en Kotlin? ¿Qué error de compilación obtendrías si no manejas todos los subtipos en un `when` usado como expresión?

---

### Caso 19 — Despachar `updateObservaciones()` en `Dispatchers.IO`

**📝 Descripción del Problema**

En `AssetDetailViewModel.kt`, si la función de actualizar observaciones no está en el dispatcher correcto, podría bloquear el hilo principal. El docente pide verificar que usa `Dispatchers.IO` y agregar un comentario explicativo de por qué es necesario.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/detail/AssetDetailViewModel.kt`

```kotlin
fun guardarObservaciones(nuevasObservaciones: String) {
    viewModelScope.launch {
        // [RDA-3 Temas 11 y 12: Estrategia de Coroutines con Dispatchers]
        // Dispatchers.IO: pool de hilos optimizado para operaciones bloqueantes de E/S
        // (lectura/escritura en Room/SQLite). NUNCA bloquees el Main dispatcher con Room.
        // Si Room no se llama en un dispatcher de fondo, lanza IllegalStateException.
        withContext(Dispatchers.IO) {
            assetRepository.updateObservations(
                id = assetId,
                observations = nuevasObservaciones,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
```

**🎯 Salida Esperada en Emulador**

- Las observaciones se guardan sin que la UI se congele.
- No aparece ningún `StrictMode` warning en Logcat sobre acceso a base de datos en el hilo principal.

**💡 Pregunta Teórica de Examen**

> ¿Qué son los `Dispatchers` en Kotlin Coroutines? Explica la diferencia entre `Dispatchers.Main`, `Dispatchers.IO` y `Dispatchers.Default`. ¿Por qué Room lanza una excepción si se le accede desde el Main Thread?

---

### Caso 20 — Encapsular `_uiState` privado en `ScanViewModel`

**📝 Descripción del Problema**

En `ScanViewModel.kt`, el `MutableStateFlow` del estado de la UI podría estar accesible desde fuera del ViewModel si no tiene el modificador correcto. El docente pide verificar (y corregir si es necesario) el patrón de encapsulación `_uiState` privado / `uiState` público de solo lectura.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/scanner/ScanViewModel.kt`

```kotlin
class ScanViewModel(...) : ViewModel() {

    // [RDA-1 Tema 5: Encapsulación del Estado en ViewModel — Patrón UDF]
    // _uiState: privado, mutable. Solo el ViewModel puede modificarlo.
    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)

    // uiState: público, inmutable. La UI solo puede leer, nunca escribir.
    // asStateFlow() convierte MutableStateFlow en StateFlow (read-only).
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onQrCodeScanned(qrCode: String) {
        _uiState.update { ScanUiState.QrDetected(qrCode) }
    }
}
```

**🎯 Salida Esperada en Emulador**

- La app compila. `_uiState` no es accesible desde `ScannerScreen.kt`.
- `uiState` se puede observar con `collectAsStateWithLifecycle()` desde la UI.

**💡 Pregunta Teórica de Examen**

> ¿Por qué en el patrón MVVM con Compose se recomienda que el ViewModel exponga un `StateFlow` en lugar de un `LiveData`? ¿Qué ventajas tiene `StateFlow` sobre `LiveData` en un contexto de Jetpack Compose?

---

## 🏛️ BLOQUE 5 — Capa de Dominio y Casos de Uso (Casos 21–25)

---

### Caso 21 — Crear `FiltrarActivosPorEstadoUseCase` con `operator fun invoke`

**📝 Descripción del Problema**

No existe un caso de uso que filtre activos según si están activos (`deletedAt == null`) o dados de baja. El docente pide crear `FiltrarActivosPorEstadoUseCase` en la capa de dominio, implementando el operador `invoke` para que se pueda llamar como una función.

**🛠️ Pasos de Cambio en el Código**

Crea el archivo: `app/src/main/java/ec/edu/puce/scantech/domain/usecase/FiltrarActivosPorEstadoUseCase.kt`

```kotlin
package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.model.Asset

/**
 * [RDA-3 Temas 9 y 10: Clean Architecture — Casos de Uso (Use Cases / Interactors)]
 *
 * Caso de uso puro: lógica de negocio sin dependencias de Android ni de Room.
 * El operador `invoke` permite llamarlo como función: filtrarUseCase(lista, true)
 */
class FiltrarActivosPorEstadoUseCase {

    /**
     * @param activos Lista completa de activos del dominio.
     * @param soloActivos `true` para activos vivos, `false` para dados de baja.
     */
    operator fun invoke(activos: List<Asset>, soloActivos: Boolean): List<Asset> =
        if (soloActivos) {
            activos.filter { it.deletedAt == null }
        } else {
            activos.filter { it.deletedAt != null }
        }
}
```

Verifica con grep que no hay imports de `android.*` en el archivo.

**🎯 Salida Esperada en Emulador**

- El caso de uso compila sin errores.
- Un test unitario local (`test/`) puede ejecutarlo sin emulador.

**💡 Pregunta Teórica de Examen**

> ¿Qué es un Caso de Uso (Use Case / Interactor) en Clean Architecture? ¿Por qué los casos de uso del dominio no deben tener imports de `android.*` o `androidx.*`, y cómo esto facilita las pruebas unitarias?

---

### Caso 22 — Agregar propiedad calculada `estaActivo` a `Asset`

**📝 Descripción del Problema**

En todo el proyecto, la condición `asset.deletedAt == null` se repite para verificar si un activo está vigente. El docente pide encapsular esta lógica como una **propiedad calculada** en el modelo de dominio `Asset`.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/domain/model/Asset.kt`

Dentro de la `data class Asset { ... }`, agrega después de `val location`:
```kotlin
/**
 * Propiedad calculada (no almacenada en Room ni en la red).
 * Un activo está activo si y solo si no ha sido dado de baja lógicamente.
 * Encapsula la regla de negocio en el dominio, evitando comparaciones nulas dispersas.
 */
val estaActivo: Boolean
    get() = deletedAt == null

/** Retorna el estado legible para mostrar en la UI de auditoría. */
val estadoTexto: String
    get() = if (estaActivo) "Activo" else "Dado de Baja"
```

Reemplaza en el resto del proyecto `asset.deletedAt == null` por `asset.estaActivo`.

**🎯 Salida Esperada en Emulador**

- En `AssetDetailScreen`, el estado del activo puede mostrarse con `asset.estadoTexto`.

**💡 Pregunta Teórica de Examen**

> ¿Cuál es la diferencia entre una propiedad (`val`) calculada (con `get()`) y un campo almacenado en una `data class` de Kotlin? ¿Las propiedades calculadas se incluyen en `equals()`, `hashCode()` y `copy()` generados automáticamente?

---

### Caso 23 — Verificar pureza del dominio con grep

**📝 Descripción del Problema**

El enunciado del examen exige que la carpeta `domain/` no tenga ninguna dependencia de Android Framework. El docente pide ejecutar el comando grep de verificación y documentar su resultado en un comentario de código o en un archivo de texto.

**🛠️ Pasos de Cambio en el Código**

En la terminal del proyecto (desde la raíz del módulo `app`), ejecuta:
```bash
grep -rn "android\.\|androidx\." app/src/main/java/ec/edu/puce/scantech/domain/ --include="*.kt"
```

Si el resultado es vacío: el dominio es puro. Agrega un comentario en `Asset.kt`:
```kotlin
/*
 * VERIFICACIÓN DE PUREZA DE DOMINIO — Resultado grep (2026-11-19):
 * Comando: grep -rn "android\.|androidx\." domain/ --include="*.kt"
 * Resultado: 0 coincidencias. ✅ Dominio 100% puro (Kotlin/JVM puro).
 * Ref: Enunciado §4 — Arquitectura Obligatoria: Domain Layer sin dependencias de Android.
 */
```

**🎯 Salida Esperada en Emulador**

- El proyecto compila. La verificación de pureza pasa con cero resultados.

**💡 Pregunta Teórica de Examen**

> ¿Por qué el principio de "Separación de Capas" (Layered Architecture) exige que la capa de dominio no dependa de la capa de datos ni de Android Framework? ¿Qué beneficio concreto tiene esto para las pruebas unitarias del dominio?

---

### Caso 24 — Validar `units >= 1` en `GuardarActivoUseCase`

**📝 Descripción del Problema**

Actualmente, `ValidadorActivo.kt` solo verifica que las unidades no sean negativas (`< 0`). Sin embargo, un activo con `0` unidades no tiene sentido operativamente. El docente pide agregar esta regla de negocio en `GuardarActivoUseCase.kt` como una capa adicional de protección.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/domain/usecase/GuardarActivoUseCase.kt`

```kotlin
class GuardarActivoUseCase(private val repository: AssetRepository) {

    suspend operator fun invoke(asset: Asset, esEdicion: Boolean): Result<Unit> {
        // Regla de negocio del dominio: mínimo 1 unidad por activo registrado en inventario LTIC
        if (asset.units < 1) {
            return Result.Error(
                IllegalArgumentException("Un activo debe tener al menos 1 unidad registrada.")
            )
        }

        return try {
            if (esEdicion) repository.update(asset) else repository.create(asset)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

**🎯 Salida Esperada en Emulador**

- Si de alguna forma `units = 0` llega al caso de uso, el guardado se rechaza y el ViewModel muestra un error.

**💡 Pregunta Teórica de Examen**

> ¿Cuál es la diferencia entre la validación de UI (en el formulario/ViewModel) y la validación de dominio (en el caso de uso)? ¿Por qué es buena práctica tener ambas, y cuál es la "fuente de verdad" de las reglas de negocio?

---

### Caso 25 — Manejar `Result.Success` / `Result.Error` con `when` exhaustivo en ViewModel

**📝 Descripción del Problema**

En `AssetFormViewModel.kt`, el resultado de `GuardarActivoUseCase` podría no estar siendo manejado con un `when` exhaustivo, dejando el caso `Result.Error` sin tratamiento visible para el usuario. El docente pide asegurar el manejo exhaustivo.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/ui/form/AssetFormViewModel.kt`

```kotlin
fun guardar() {
    viewModelScope.launch {
        _uiState.update { AssetFormUiState.Guardando }
        val resultado = withContext(Dispatchers.IO) {
            guardarActivoUseCase(buildAsset(), esEdicion = assetId != null)
        }
        // when exhaustivo sobre sealed interface Result
        when (resultado) {
            is Result.Success -> {
                _uiState.update { AssetFormUiState.GuardadoExitosamente }
            }
            is Result.Error -> {
                _uiState.update {
                    AssetFormUiState.Error(
                        mensaje = resultado.exception.message ?: "Error desconocido al guardar"
                    )
                }
                android.util.Log.e("ScanTech_Form", "Error al guardar activo", resultado.exception)
            }
        }
    }
}
```

**🎯 Salida Esperada en Emulador**

- Si el guardado falla (por ejemplo, QR duplicado en Room), la UI muestra un `Snackbar` o texto de error en lugar de bloquearse silenciosamente.

**💡 Pregunta Teórica de Examen**

> ¿Qué es un `sealed interface Result<T>` y cómo se diferencia de lanzar excepciones directamente? ¿Qué ventaja ofrece el patrón `Result` para el manejo de errores en la capa de UI?

---

## 🗄️ BLOQUE 6 — Room, DAOs y Persistencia Reactiva (Casos 26–30)

---

### Caso 26 — Añadir query `observarPorBloque()` en `AssetDao`

**📝 Descripción del Problema**

El inventario LTIC está organizado por bloques físicos (A, B, C, etc.). Actualmente no existe una consulta que filtre activos por bloque. El docente pide agregar la función `observarPorBloque` en `AssetDao.kt` que retorne un `Flow` reactivo.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/data/local/dao/AssetDao.kt`

Dentro de la clase abstracta `AssetDao`, agrega:
```kotlin
/**
 * [RDA-3 Temas 11 y 12: Consultas Reactivas con Flow en Room]
 *
 * Flow reactivo: cada vez que un activo del bloque especificado se inserta, actualiza o borra,
 * Room emite automáticamente la lista actualizada al colector (ViewModel → UI).
 * Solo incluye activos no dados de baja lógica (deletedAt IS NULL).
 */
@Query("""
    SELECT * FROM assets
    WHERE block = :bloque
      AND deletedAt IS NULL
    ORDER BY name COLLATE NOCASE ASC
""")
abstract fun observarPorBloque(bloque: String): Flow<List<AssetEntity>>
```

**🎯 Salida Esperada en Emulador**

- La función compila correctamente.
- En el ViewModel, `assetDao.observarPorBloque("A").collect { ... }` emite los activos del bloque A en tiempo real.

**💡 Pregunta Teórica de Examen**

> ¿Cómo funciona la reactividad de `Flow` en Room? Cuando se inserta un nuevo activo en la base de datos, ¿qué mecanismo de Room notifica automáticamente a los colectores del `Flow` sin que el desarrollador tenga que hacer nada explícito?

---

### Caso 27 — Crear función `@Transaction` para insert atómico con cola de sincronización

**📝 Descripción del Problema**

Cuando se crea un activo nuevo, deben ocurrir dos operaciones: insertar en `assets` y encolar en `sync_queue`. Si la segunda falla, el activo quedaría sin operación de sincronización pendiente. El docente pide demostrar que la función `insertWithQueue` en `AssetDao.kt` es `@Transaction` y explicar por qué.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/data/local/dao/AssetDao.kt`

Agrega (o verifica que ya existe) la función transaccional:
```kotlin
/**
 * [RDA-3 Temas 11 y 12: Atomicidad con @Transaction en Room]
 *
 * @Transaction garantiza que el insert en 'assets' y el enqueue en 'sync_queue'
 * son atómicos: o ambos se confirman (COMMIT), o ninguno (ROLLBACK automático).
 * Sin esto, un activo podría quedar sin operación pendiente si la app se cierra
 * entre las dos escrituras.
 */
@Transaction
open suspend fun insertWithQueue(entity: AssetEntity, payload: String) {
    insert(entity)
    enqueue(
        SyncOperationEntity(
            assetId = entity.id,
            tipo = SyncStatus.PENDING_CREATE,
            payload = payload,
            createdAt = entity.updatedAt
        )
    )
}
```

**🎯 Salida Esperada en Emulador**

- Al crear un activo sin conexión, la tabla `sync_queue` contiene exactamente una fila con `tipo = PENDING_CREATE`.

**💡 Pregunta Teórica de Examen**

> ¿Qué significa que una operación de base de datos sea "atómica"? ¿Qué hace `@Transaction` de Room internamente (a nivel de SQL) para garantizar la atomicidad? ¿Qué sucedería si se omitiera `@Transaction` en este caso?

---

### Caso 28 — Implementar baja lógica con `UPDATE assets SET deletedAt`

**📝 Descripción del Problema**

El proyecto ya implementa borrado lógico. El docente pide que expliques la diferencia entre borrado lógico y borrado físico, y que modifiques la query `markDeleted` para que también registre el `updatedAt` con el mismo timestamp del `deletedAt`.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/data/local/dao/AssetDao.kt`

Verifica que la query existente ya tiene `updatedAt = :deletedAt`:
```kotlin
@Query("""
    UPDATE assets
    SET deletedAt  = :deletedAt,
        updatedAt  = :deletedAt,
        syncStatus = 'PENDING_DELETE'
    WHERE id = :id
""")
abstract suspend fun markDeleted(id: String, deletedAt: Long): Int
```

Si no tiene `updatedAt = :deletedAt`, agrégalo. Luego añade un comentario:
```kotlin
// Baja Lógica (Soft Delete): la fila NO se elimina físicamente de la tabla.
// deletedAt marca el instante de la baja; la UI la filtra (WHERE deletedAt IS NULL).
// El registro se mantiene hasta que el servidor confirme la baja (PENDING_DELETE → purgeConfirmedDeletion).
// Estrategia Offline-First: si no hay conexión, la baja queda pendiente en sync_queue.
```

**🎯 Salida Esperada en Emulador**

- Al dar de baja un activo, desaparece del catálogo (filtrado por `deletedAt IS NULL`) pero la fila sigue en SQLite (verificable con Database Inspector).

**💡 Pregunta Teórica de Examen**

> ¿Cuál es la diferencia entre borrado lógico (soft delete) y borrado físico (hard delete)? ¿Por qué la estrategia offline-first de ScanTech requiere borrado lógico en lugar de físico? ¿Qué riesgo existiría si se borrara físicamente el registro antes de sincronizar con el servidor?

---

### Caso 29 — Verificar el escape LIKE para prevenir búsquedas incorrectas

**📝 Descripción del Problema**

La función de búsqueda en `AssetDao.kt` usa `LIKE` con el texto del usuario. Si el usuario escribe `50%` como búsqueda, el `%` sería interpretado como comodín SQL y devolvería todos los activos. El docente pide demostrar que `escaparLike()` previene esto y agregar un caso de prueba como comentario.

**🛠️ Pasos de Cambio en el Código**

Archivo: `app/src/main/java/ec/edu/puce/scantech/data/local/dao/AssetDao.kt`

Verifica que el `companion object` ya tiene:
```kotlin
companion object {
    /** Escapa '%', '_' y '\' para que el texto del usuario se busque literalmente en LIKE. */
    fun escaparLike(texto: String): String =
        texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
```

Agrega un comentario de ejemplo:
```kotlin
// EJEMPLO DE USO:
// usuarioEscribe = "50%"
// escaparLike("50%") → "50\\%"
// La query generada: WHERE name LIKE '%50\%%' ESCAPE '\'
// Resultado: busca el texto literal "50%" (no "50" seguido de cualquier cosa).
//
// SIN escaparLike:
// WHERE name LIKE '%50%%' → devolvería TODOS los activos que contengan "50" (bug).
```

En `InventoryViewModel.kt`, verifica que la búsqueda usa `AssetDao.escaparLike(query)` antes de pasarla al DAO.

**🎯 Salida Esperada en Emulador**

- Buscar `50%` en el catálogo devuelve solo activos cuyo nombre/specs contengan literalmente "50%".

**💡 Pregunta Teórica de Examen**

> ¿Qué es la inyección SQL (SQL Injection) y cómo la previene Room en comparación con SQLiteDatabase directo? ¿Es posible un ataque de inyección SQL a través de Room si se usan `@Query` con parámetros? Explica el rol de `ESCAPE` en las queries `LIKE`.

---

### Caso 30 — Trazar el flujo SSOT completo de extremo a extremo

**📝 Descripción del Problema**

El docente pide que traces y documentes el flujo completo de datos reactivo (Single Source of Truth) desde la base de datos Room hasta la pantalla, mostrando cómo un cambio en Room se propaga automáticamente hasta el composable sin intervención manual.

**🛠️ Pasos de Cambio en el Código**

En `AssetDao.kt`, agrega un bloque de documentación extenso al principio de la clase:
```kotlin
/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  FLUJO SSOT (Single Source of Truth) — ScanTech LTIC
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  [1] ROOM (AssetDao)
 *      observeAssets(query, category): Flow<List<AssetEntity>>
 *      ↓  Room emite automáticamente ante cualquier INSERT/UPDATE/DELETE en 'assets'
 *
 *  [2] REPOSITORIO (AssetRepositoryImpl)
 *      observeAssets(...): Flow<List<Asset>>
 *      .map { entities -> entities.map { it.toDomain() } }
 *      ↓  Convierte AssetEntity → Asset (dominio puro) mediante mappers
 *
 *  [3] CASO DE USO (ObtenerActivosUseCase)
 *      operator fun invoke(...): Flow<List<Asset>>
 *      ↓  Orquesta el repositorio; aplica reglas de negocio si las hubiera
 *
 *  [4] VIEWMODEL (InventoryViewModel)
 *      val activos: StateFlow<List<Asset>> = obtenerActivosUseCase(...)
 *          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
 *      ↓  stateIn convierte Flow frío en StateFlow caliente; 5s de gracia ante rotación
 *
 *  [5] UI (InventoryDashboardScreen)
 *      val activos by viewModel.activos.collectAsStateWithLifecycle()
 *      LazyColumn { items(activos) { AssetListItem(it) } }
 *      ↓  collectAsStateWithLifecycle respeta el ciclo de vida (no recolecta en background)
 *
 *  RESULTADO: Un INSERT en Room llega a la LazyColumn en < 50ms sin polling ni callbacks manuales.
 * ═══════════════════════════════════════════════════════════════════════════
 */
```

**🎯 Salida Esperada en Emulador**

- Al insertar un activo desde el formulario, aparece en el catálogo del Dashboard en tiempo real, sin que el usuario tenga que refrescar manualmente.

**💡 Pregunta Teórica de Examen**

> ¿Qué significa "Single Source of Truth" (SSOT) en la arquitectura de Android? ¿Por qué Room es la SSOT en ScanTech y no el servidor remoto? Explica cómo `stateIn(SharingStarted.WhileSubscribed(5000))` optimiza el consumo de recursos del `Flow` durante los cambios de configuración.

---

## 📊 Resumen de Distribución

| Bloque | Tema | Casos | Archivos Principales |
|--------|------|-------|----------------------|
| 1 | Kotlin Idiomático y Null Safety | 1–5 | `ValidadorActivo.kt`, `Asset.kt`, `AssetFormViewModel.kt` |
| 2 | Rutas Tipadas y Navegación | 6–10 | `NavRoutes.kt`, `NavGraph.kt` |
| 3 | UI Compose y Material Design 3 | 11–15 | `AssetDetailScreen.kt`, `AssetFormScreen.kt`, `Type.kt` |
| 4 | ViewModels, UDF y Ciclo de Vida | 16–20 | `AssetDetailViewModel.kt`, `AssetFormViewModel.kt`, `ScanViewModel.kt` |
| 5 | Capa de Dominio y Casos de Uso | 21–25 | `domain/model/Asset.kt`, `domain/usecase/*`, `Result.kt` |
| 6 | Room, DAOs y Persistencia Reactiva | 26–30 | `AssetDao.kt`, `AppDatabase.kt`, `AssetRepositoryImpl.kt` |

---

> **💡 Consejo para la Defensa:** Para cada caso, prepárate para:
> 1. **Implementar** el cambio en 10–15 minutos
> 2. **Explicar** el concepto teórico de forma oral
> 3. **Mostrar** el resultado en el emulador
> 4. **Responder** la pregunta teórica con ejemplos del proyecto ScanTech

