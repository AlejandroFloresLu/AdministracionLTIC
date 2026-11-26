# ScanTech LTIC

Aplicación Android nativa para auditar y administrar el inventario de hardware del departamento de Logística Tecnológica (LTIC) de la PUCE. Funciona **offline-first**: los datos viven en el teléfono (Room) y se sincronizan con una API REST cuando hay conexión.

Proyecto final de **Programación Móvil**, PUCE.
Estado: **Fase 2, "Integración, resiliencia y defensa final"** (en curso).

## Funcionalidades

| Pantalla | Qué hace |
|---|---|
| Catálogo | Lista de activos con búsqueda en tiempo real (con *debounce*), filtro por categoría y estados de carga, error y lista vacía |
| Formulario | Alta y edición de activos, con validación en tiempo real y estado que sobrevive a la rotación y a la muerte del proceso (`SavedStateHandle`) |
| Ficha técnica | Datos completos del activo, observaciones de auditoría, edición y baja lógica con confirmación |
| Permiso de cámara | Pantalla que explica para qué se usa la cámara antes de pedir el permiso |
| Escáner | Lectura de códigos QR con la cámara (CameraX + ML Kit, en el dispositivo y sin internet), linterna y vibración al detectar. Si el QR existe abre la ficha; si no, abre el formulario con el código ya escrito |

## Stack

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.0.0, JDK 21 |
| SDK | `minSdk` 26, `compileSdk` y `targetSdk` 36 (Android 16) |
| Build | Gradle 8.10.2, AGP 8.7.1, catálogo de versiones en `gradle/libs.versions.toml`, KSP |
| UI | Jetpack Compose (BOM 2024.10.00), Material 3 con color dinámico |
| Navegación | Navigation Compose 2.8.3 con rutas tipadas `@Serializable` |
| Persistencia | Room 2.6.1 (esquema exportado y migraciones) |
| Red | Retrofit 2.11.0, OkHttp 4.12.0, kotlinx.serialization 1.7.3 |
| Concurrencia | Corrutinas 1.9.0, `StateFlow`, `viewModelScope` |
| Cámara e IA local | CameraX 1.4.0 y ML Kit Barcode 18.3.1 (escaneo de QR); ZXing 3.5.3 para generar QR |

Todas las dependencias se declaran en el catálogo de versiones. No hay versiones escritas a mano en los `build.gradle.kts`.

## Arquitectura

MVVM con Clean Architecture y flujo unidireccional de datos (UDF). Las dependencias apuntan hacia el dominio:

```
ui (Compose + ViewModel)  ──►  domain (casos de uso, modelos, contratos)  ◄──  data (Room, Retrofit, repositorio)
```

```
app/src/main/java/ec/edu/puce/scantech/
├── ui/            Pantallas Compose, ViewModels y estados de UI (sealed interface)
│   ├── inventory/   Catálogo
│   ├── form/        Formulario de alta/edición
│   ├── detail/      Ficha técnica
│   ├── scanner/     Escáner QR con CameraX + ML Kit
│   ├── permissions/ Permiso de cámara
│   ├── navigation/  NavRoutes (@Serializable) y NavGraph
│   └── theme/       Tema Material 3
├── domain/        Kotlin puro: modelos, AssetRepository (interfaz), casos de uso y ValidadorActivo
├── data/          Room (entidades, DAOs, AppDatabase), Retrofit (AssetApi, DTOs), mappers y AssetRepositoryImpl
└── di/            AppContainer: inyección de dependencias manual
```

- **Fuente única de verdad:** la UI observa Room con `Flow`. Retrofit solo actualiza Room y nunca habla directo con la pantalla.
- **Estado:** cada ViewModel expone un `StateFlow` de solo lectura y la pantalla lo dibuja (UI = f(estado)). Los eventos suben al ViewModel.
- **Cola de sincronización:** cada alta, edición o baja se guarda junto con una operación en `sync_queue` dentro de una `@Transaction`.

## Base de datos (Room)

- Versión actual: **2**. Esquemas exportados en `app/schemas/`.
- Tablas: `assets` (PK UUID, índice único en `qrCode`), `categories` y `sync_queue`.
- Migración `MIGRACION_1_2` registrada en `AppDatabase`.
- Baja lógica con `deletedAt`: el activo no se borra hasta que el servidor confirma.

## Red (Retrofit)

`AssetApi` define tres endpoints: `POST assets/sync`, `GET assets?updatedSince=` y `GET categories`. La URL base está en `di/AppContainer.kt`. **Todavía no hay un servidor real**, así que cualquier intento de sincronizar termina en un error de conexión controlado y la app sigue funcionando con los datos locales.

## Cómo ejecutar

1. Abrir el proyecto en Android Studio (Ladybug o superior) con JDK 21.
2. Esperar la sincronización de Gradle.
3. Ejecutar en un emulador o teléfono con Android 8.0 (API 26) o superior. Se recomienda un emulador con API 36.

Desde la terminal:

```bash
./gradlew assembleDebug        # compila el APK de depuración
./gradlew testDebugUnitTest    # pruebas unitarias (JVM)
./gradlew lintDebug            # análisis estático
```

## Pruebas

Pruebas unitarias en `app/src/test/`: validador de activos, política de conflictos de sincronización, generador de QR, mappers y `InventoryViewModel` (*debounce*, filtros y errores). Las pruebas de Room y del repositorio están en `app/src/androidTest/` y necesitan un emulador.

## Ramas de práctica para la defensa (RDA 3)

Estas ramas **no se fusionan a `main`**. Cada una parte de `main` y resuelve un caso de refactorización probable, para comparar después de intentarlo. Cada rama compila y pasa las pruebas unitarias.

| Rama | Caso | Cómo comprobarlo |
|---|---|---|
| `practica/caso-01-filtro-pendientes` | Switch "Solo pendientes de sincronizar": query nueva en el DAO con `Flow`, caso de uso y `flatMapLatest` | Activar el switch: solo quedan los activos con insignia "Pendiente" |
| `practica/caso-02-regla-negocio-dominio` | Reglas nuevas en `ValidadorActivo`: nombre de 4 o más caracteres y unidades entre 1 y 500 | Escribir "Ana" o 501 unidades: error bajo el campo y Guardar deshabilitado |
| `practica/caso-03-parametro-ruta-tipada` | Parámetro obligatorio `origen` en `AssetDetailRoute` | La ficha muestra "Abierto desde: Catálogo" o "Escáner" |
| `practica/caso-04-estado-sellado-sin-resultados` | Estado nuevo `SinResultados(query)` en `InventoryUiState` | Buscar un texto que no existe: "No hay activos para ..." |
| `practica/caso-05-ciclo-vida-viewmodel` | `onCleared()` con `Log.d` en `AssetDetailViewModel` | Logcat con `tag:ScanTech_Lifecycle`: aparece al volver al catálogo y no al rotar |
| `practica/caso-06-columna-room-migracion` | Columna `responsable` con `@ColumnInfo`, versión 3 y `MIGRACION_2_3` | La ficha muestra "Responsable: Bodega LTIC" sin perder datos |
| `practica/caso-07-sincronizacion-estados-red` | `safeApiCall`, `SyncUiState` sellado y botón Sincronizar | El botón se desactiva mientras carga; sin servidor sale "Sin conexión..." en un Snackbar |
| `practica/caso-08-resumen-dispatchers` | Caso de uso que calcula totales con `flowOn(Dispatchers.Default)` | Bajo los filtros: "N activos · N unidades · N pendientes" |
| `practica/caso-09-colores-material3` | Insignia de sincronización con roles `secondaryContainer`, `tertiaryContainer` y `errorContainer` | Cambiar el fondo de pantalla: la insignia cambia de tono |
| `practica/caso-10-catalogo-splashscreen` | `core-splashscreen` agregado solo por el catálogo de versiones | Al abrir la app aparece la pantalla de inicio azul con el ícono |

Para practicar un caso: crear una rama propia desde `main`, resolverlo con el tiempo de la prueba (10 a 15 minutos) y comparar con `git diff main practica/caso-XX-...`.

## Flujo de trabajo

- `main` siempre compila. Los cambios entran por *pull request* desde ramas `feat/`, `fix/`, `chore/` o `docs/`.
- Mensajes de commit con prefijo convencional: `feat(ficha): ...`, `fix(...)`, `chore(...)`, `docs(...)`.
- Antes de abrir un PR: `./gradlew assembleDebug testDebugUnitTest`.

## Equipo

- Alejandro Flores
- Paúl Alexander Rosero Carrión
- Gabriel Aguinaga
- Michael Molina
- Joselyn Cadena

Documentación del proyecto (documento maestro e informe de actividades) en [`docs/`](docs/).
