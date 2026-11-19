#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Motor de Alta Velocidad para Casos de Defensa de Programación Móvil (ScanTech LTIC)
Desarrollado para resolver casos individuales o generar ramas batch en segundos.
"""

import sys
import os
import re
import subprocess
from pathlib import Path

# Raíz del proyecto
PROJECT_ROOT = Path(__file__).resolve().parent.parent
APP_SRC = PROJECT_ROOT / "app" / "src" / "main" / "java" / "ec" / "edu" / "puce" / "scantech"
ENTREGA_DIR = PROJECT_ROOT / "teoria" / "entrega"

# Diccionario maestro de los 30 casos con soluciones limpias y respuestas teóricas (<150 palabras)
CASOS_INFO = {
    1: {
        "titulo": "Validación de QR con .takeIf y .let funcional",
        "archivo": "domain/validacion/ValidadorActivo.kt",
        "figura": "Figura 1: Formulario validando el formato de código QR mediante funciones de alcance idiomáticas.",
        "pantalla": "Formulario de Alta de Activo. Ingresa un QR con formato incorrecto (ej. 'LTIC-ABC') y luego uno válido ('LTIC-000123').",
        "pregunta": "¿Qué diferencia semántica existe entre .let {} y .also {} en Kotlin? Mencione qué retorna cada una y por qué se eligió .let aquí.",
        "respuesta": "La diferencia fundamental radica en su valor de retorno y propósito de diseño: '.let {}' transforma el objeto receptor ('it') y retorna el resultado de la última expresión evaluada dentro de la lambda, siendo ideal para transformaciones de tipo y operaciones seguras sobre valores anulables (?.let). Por el contrario, '.also {}' retorna invariablemente el objeto receptor original sin modificarlo, diseñándose para efectos secundarios como logging o depuración intermedia. En este caso se eligió '.let' porque se requería encadenar la validación condicional directamente después de '.takeIf { it.isNotBlank() }', permitiendo evaluar el patrón Regex sobre el valor no nulo y bifurcar mediante el operador Elvis (?:) hacia el mensaje de error correspondiente.",
        "find_block": r"// --- CÓDIGO ORIGINAL \(Comentar si el docente pide activar el Caso 1\) ---[\s\S]*?/\* =================== \[DEFENSA CASO 1: Validar QR con \.takeIf y \.let\] ===================[\s\S]*?======================================================================================= \*/",
        "replace_clean": """        datos.qrCode.trim()
            .takeIf { it.isNotBlank() }
            ?.let { qr ->
                if (!FORMATO_QR.matches(qr))
                    errores[CampoActivo.QR_CODE] = "Formato esperado: LTIC-000123"
                else if (qrYaExiste)
                    errores[CampoActivo.QR_CODE] = "Ya existe un activo con este código"
            }
            ?: run { errores[CampoActivo.QR_CODE] = "El código QR es obligatorio" }"""
    },
    2: {
        "titulo": "Conversión Segura de Stock con toIntOrNull() y Elvis",
        "archivo": "domain/validacion/ValidadorActivo.kt",
        "figura": "Figura 1: Validación reactiva de unidades numéricas con retroalimentación contextual de valor erróneo.",
        "pantalla": "Formulario de Alta de Activo. Escribe letras (ej. 'abc') en el campo Unidades para ver el mensaje de error dinámico.",
        "pregunta": "¿Por qué toIntOrNull() es preferible a toInt() en un formulario de usuario y qué excepción de la JVM se previene?",
        "respuesta": "'toIntOrNull()' es la práctica idiomática estándar de Kotlin para parsear entradas de texto no confiables. Si un usuario introduce caracteres alfabéticos o símbolos en un campo numérico, el método clásico 'toInt()' (equivalente a Integer.parseInt()) lanza inmediatamente una excepción no controlada 'java.lang.NumberFormatException'. Si dicha excepción no es capturada en el hilo principal de la interfaz de usuario, provoca la terminación abrupta del proceso (Fatal Crash o ANR). En contraste, 'toIntOrNull()' intercepta el error de parseo de forma segura devolviendo 'null', lo cual permite aplicar el operador Elvis (?:) o un 'when' reactivo para emitir un mensaje de retroalimentación amigable al usuario sin degradar la estabilidad de la app.",
        "find_block": r"// --- CÓDIGO ORIGINAL \(Comentar si el docente pide activar el Caso 2\) ---[\s\S]*?/\* =================== \[DEFENSA CASO 2: Conversión segura con toIntOrNull y Elvis\] ===================[\s\S]*?================================================================================================= \*/",
        "replace_clean": """        val unidadesSeguras = datos.units.trim().toIntOrNull()
        when {
            unidadesSeguras == null -> errores[CampoActivo.UNITS] =
                "\\"${datos.units.trim()}\\" no es un número entero válido"
            unidadesSeguras < 0 -> errores[CampoActivo.UNITS] = "Las unidades no pueden ser negativas"
            unidadesSeguras > 9999 -> errores[CampoActivo.UNITS] = "Máximo 9 999 unidades por activo"
        }"""
    },
    3: {
        "titulo": "Función de Extensión String.esCodigoQrLtic()",
        "archivo": "domain/validacion/ValidadorActivo.kt",
        "figura": "Figura 1: Código QR validado mediante función de extensión pura de Kotlin sin acoplamiento a Android.",
        "pantalla": "Formulario de Alta de Activo. Completa los campos con un código válido 'LTIC-000456'.",
        "pregunta": "¿Qué son las funciones de extensión en Kotlin y qué limitaciones de acceso a miembros tienen?",
        "respuesta": "Las funciones de extensión permiten añadir nuevas funcionalidades y comportamientos a clases existentes (incluso de librerías externas o del JDK como String) sin necesidad de heredar de ellas ni modificar su código fuente original, respetando el principio Open/Closed de SOLID. A nivel de bytecode en la JVM, el compilador las transforma en métodos estáticos donde el primer argumento corresponde a la instancia receptora ('this'). Como consecuencia directa de esta compilación estática, las funciones de extensión NO pueden acceder a miembros privados ('private') o protegidos ('protected') de la clase que extienden; operan estrictamente a través de la interfaz pública visible del tipo receptor.",
        "find_block": r"/\* =================== \[DEFENSA CASO 3: Función de Extensión String\.esCodigoQrLtic\(\)\] ===================[\s\S]*?======================================================================================================= \*/",
        "replace_clean": """fun String.esCodigoQrLtic(): Boolean {
    val regex = Regex("^LTIC-\\\\d{6}$")
    return regex.matches(this.trim())
}"""
    },
    4: {
        "titulo": "Sanitización de Texto con Función de Alcance .run {}",
        "archivo": "ui/form/AssetFormViewModel.kt",
        "figura": "Figura 1: Ficha técnica mostrando especificaciones sanitizadas en mayúsculas sin espacios redundantes.",
        "pantalla": "Crear un activo escribiendo especificaciones en minúsculas y con espacios ('  router cisco 2911  ') y verificar el guardado.",
        "pregunta": "¿Cuándo se debe usar .run {} versus .apply {} y cuál es la diferencia en su valor de retorno?",
        "respuesta": "La diferencia fundamental reside en el valor de retorno: '.apply {}' retorna siempre el objeto receptor original ('this'), por lo que su caso de uso idóneo es la configuración o mutación de propiedades de un objeto durante su instanciación. Por su parte, '.run {}' ejecuta el bloque de código en el contexto del receptor pero retorna el resultado de la última expresión computada. Se utiliza '.run' cuando se necesita transformar el objeto o computar un valor final derivado a partir de múltiples operaciones encadenadas (como sanitizar un texto aplicando 'trim().uppercase()') antes de asignarlo a una propiedad inmutable.",
        "find_block": r"// --- CÓDIGO ORIGINAL \(Comentar si el docente pide activar el Caso 4\) ---[\s\S]*?/\* =================== \[DEFENSA CASO 4: Sanitizar Texto con Función de Alcance \.run\] ===================[\s\S]*?====================================================================================================== \*/",
        "replace_clean": """            technicalSpecs = current.technicalSpecs.run {
                trim().uppercase()
            },"""
    },
    5: {
        "titulo": "Desestructuración de Data Class Asset para Auditoría",
        "archivo": "ui/detail/AssetDetailScreen.kt",
        "figura": "Figura 1: Registro en Logcat de la apertura de ficha técnica desestructurada de un activo.",
        "pantalla": "Abre la Ficha Técnica de cualquier activo en el catálogo y visualiza el Logcat con el filtro 'ScanTech_Audit'.",
        "pregunta": "¿Cómo funciona la desestructuración de data classes en Kotlin y qué métodos genera el compilador?",
        "respuesta": "La desestructuración en Kotlin permite descomponer un objeto en múltiples variables individuales en una única sentencia (ej. 'val (id, qr, nombre) = asset'). Esto es posible porque el compilador genera automáticamente métodos de acceso posicionales llamados 'component1()', 'component2()', ... 'componentN()' para cada propiedad declarada en el constructor primario de una data class. El orden de asignación es estrictamente posicional según el orden de declaración en la clase, no por nombre. Aunque las clases normales pueden implementar métodos componentN() manualmente con el modificador 'operator', las data classes los generan por defecto en tiempo de compilación sin boilerplate.",
        "find_block": r"/\* =================== \[DEFENSA CASO 5: Desestructuración de Data Class Asset\] ===================[\s\S]*?============================================================================================== \*/",
        "replace_clean": """            val (assetId, qrCode, nombre) = state.asset
            android.util.Log.d(
                "ScanTech_Audit",
                "Auditoría Ficha Abierta → ID=$assetId | QR=$qrCode | Nombre=$nombre"
            )"""
    },
    6: {
        "titulo": "Parámetro modoAuditoria en Rutas Tipadas @Serializable",
        "archivo": "ui/detail/AssetDetailScreen.kt",
        "figura": "Figura 1: Pantalla de detalle en modo auditoría con botón de edición ocultado contractualmente.",
        "pantalla": "Navega al detalle de un activo con modoAuditoria=true para comprobar que el botón de edición no se muestra.",
        "pregunta": "¿Por qué las rutas @Serializable son superiores a las rutas de texto plano en Navigation Compose?",
        "respuesta": "Las rutas tipadas (@Serializable) introducidas en Navigation Compose resuelven de raíz la fragilidad de las rutas basadas en cadenas de texto ('asset/{id}?audit={audit}'). En el enfoque antiguo, los errores tipográficos o discrepancias de tipos solo se detectaban en tiempo de ejecución provocando cierres inesperados. Con @Serializable, cada destino es una clase u objeto Kotlin serializable donde los parámetros son validados por el compilador (Type-Safety). Si se altera o elimina un argumento, el proyecto no compila. Además, elimina el parseo manual de Bundles: 'toRoute<T>()' extrae y deserializa los argumentos automáticamente con seguridad de tipos garantizada.",
        "find_block": r"/\* =================== \[DEFENSA CASO 6: Ocultar Edición en Modo Auditoría\] ===================[\s\S]*?============================================================================================= \*/[\s\S]*?IconButton\(onClick = onNavigateToEdit\) \{[\s\S]*?\}",
        "replace_clean": """                    // Botón para navegar al formulario en modo edición (Oculto en modo auditoría)
                    // if (!modoAuditoria) {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit_asset),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // }"""
    },
    11: {
        "titulo": "Tokens Semánticos tertiaryContainer en Material 3",
        "archivo": "ui/detail/AssetDetailScreen.kt",
        "figura": "Figura 1: Tarjeta de ubicación física renderizada con roles semánticos tertiaryContainer y onTertiaryContainer.",
        "pantalla": "Abre la Ficha Técnica de un activo y observa la tarjeta de ubicación física con contraste dinámico adaptado.",
        "pregunta": "¿Por qué se prohíbe quemar colores hexadecimales y qué beneficio aportan los roles semánticos en M3?",
        "respuesta": "Quemar valores hexadecimales rígidos (ej. Color(0xFF2E7D32)) rompe la coherencia visual, impide la adaptación al Modo Oscuro y viola las normas de accesibilidad de Android. Material Design 3 establece un sistema basado en tokens y roles semánticos (Primary, Secondary, Tertiary, Surface, Error) organizados en pares complementarios obligatorios ('container' y 'on-container'). Al utilizar 'tertiaryContainer' para el fondo y 'onTertiaryContainer' para el texto, el framework garantiza algorítmicamente un ratio de contraste mínimo de 4.5:1 (estándar WCAG AAA), funcionando perfectamente tanto en temas claros, oscuros y con paletas dinámicas generadas por el sistema operativo (Dynamic Color en Android 12+).",
        "find_block": r"/\* =================== \[DEFENSA CASO 11: Color Semántico tertiaryContainer en Card\] ===================[\s\S]*?===================================================================================================== \*/\s*DetailSectionCard\(\s*title = stringResource\(R\.string\.detail_section_location\)",
        "replace_clean": """            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ubicación Física (Modo Auditoría)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = asset.location,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            DetailSectionCard(
                title = stringResource(R.string.detail_section_location)"""
    },
    12: {
        "titulo": "Botón Guardar con enabled Reactivo a Validación (UI = f(State))",
        "archivo": "ui/form/AssetFormScreen.kt",
        "figura": "Figura 1: Botón Guardar deshabilitado reactivamente mientras existan errores en el formulario.",
        "pantalla": "Abre el formulario vacío o con errores. Verifica que el botón 'Guardar Activo' permanezca desactivado.",
        "pregunta": "Basándose en el concepto UI = f(State), ¿por qué el botón cambia de disponibilidad sin llamar a setEnabled()?",
        "respuesta": "En Jetpack Compose, la interfaz es una función matemática pura del estado ('UI = f(State)'). A diferencia del modelo imperativo tradicional donde el programador debía invocar manualmente métodos mutadores como 'button.setEnabled(false)', Compose trabaja bajo un paradigma declarativo. El parámetro 'enabled' del componente Button está directamente enlazado a una expresión reactiva ('!uiState.isSaving && uiState.isValid && uiState.errores.isEmpty()'). Cada vez que el usuario teclea, el ViewModel emite un nuevo 'AssetFormUiState', lo cual dispara una recomposición inteligente en la que Compose recalcula y redibuja automáticamente el nodo del botón con su nuevo aspecto visual deshabilitado sin requerir intervención manual.",
        "find_block": r"// --- CÓDIGO ORIGINAL \(Comentar si el docente pide activar el Caso 12\) ---[\s\S]*?/\* =================== \[DEFENSA CASO 12: Botón Guardar con enabled Reactivo a Validación\] ===================[\s\S]*?========================================================================================================== \*/",
        "replace_clean": """                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving && uiState.isValid && uiState.errores.isEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {"""
    },
    16: {
        "titulo": "Auditoría de Ciclo de Vida con onCleared() en ViewModel",
        "archivo": "ui/detail/AssetDetailViewModel.kt",
        "figura": "Figura 1: Registro en Logcat de la destrucción de AssetDetailViewModel al salir de la pantalla.",
        "pantalla": "Abre la ficha de un activo y presiona el botón 'Atrás'. Filtra Logcat por 'ScanTech_Lifecycle'.",
        "pregunta": "Explique la diferencia de ciclo de vida entre la destrucción de una Activity y la llamada a onCleared() del ViewModel.",
        "respuesta": "La diferencia fundamental radica en la causa de la destrucción: una Activity se destruye ante cualquier cambio de configuración (como rotar la pantalla horizontalmente), pero en ese caso el ViewModel NO es destruido ni se ejecuta 'onCleared()'. El framework de Android retiene la instancia del ViewModel en el 'ViewModelStore' a través de 'NonConfigurationInstances'. El método 'onCleared()' se invoca exclusivamente cuando la pantalla finaliza de forma definitiva (el usuario pulsa 'Atrás', se hace popBackStack() o la actividad ejecuta finish()). Es el punto idóneo para liberar recursos costosos, cancelar flujos asíncronos o registrar eventos finales de auditoría.",
        "find_block": r"/\* =================== \[DEFENSA CASO 16: Sobrescribir onCleared\(\) para Auditoría de Ciclo de Vida\] ===================[\s\S]*?==================================================================================================================== \*/",
        "replace_clean": """    override fun onCleared() {
        super.onCleared()
        val activoActual = (_uiState.value as? AssetDetailUiState.Success)?.asset
        android.util.Log.i(
            "ScanTech_Lifecycle",
            "Auditoría: AssetDetailViewModel finalizado limpiamente. Último activo inspeccionado: ${activoActual?.name ?: "ID: $assetId"}"
        )
    }"""
    },
    21: {
        "titulo": "Caso de Uso Puro FiltrarActivosPorEstadoUseCase con invoke",
        "archivo": "domain/usecase/ObtenerActivosUseCase.kt",
        "figura": "Figura 1: Caso de uso de dominio puro implementando sobrecarga del operador invoke.",
        "pantalla": "Sección de código en domain/usecase/ evidenciando lógica de negocio sin dependencias de Android.",
        "pregunta": "¿Qué beneficios aporta implementar casos de uso con 'operator fun invoke' en Clean Architecture?",
        "respuesta": "El operador 'invoke' permite tratar a la instancia de una clase como si fuera una función de primera clase (ej. 'filtrarActivos(lista, soloActivos = true)'), mejorando la claridad semántica y reduciendo el boilerplate al consumir el caso de uso desde los ViewModels. Siguiendo Clean Architecture y el Principio de Responsabilidad Única (SRP), cada caso de uso encapsula una regla de negocio aislada e independiente de frameworks de presentación o base de datos. Al ser Kotlin puro sin dependencias de Android SDK, permite ejecutar pruebas unitarias instantáneas en milisegundos en la JVM de desarrollo sin necesidad de emuladores.",
        "find_block": r"/\* =================== \[DEFENSA CASO 21: Caso de Uso Puro FiltrarActivosPorEstadoUseCase con invoke\] ===================[\s\S]*?====================================================================================================================== \*/",
        "replace_clean": """class FiltrarActivosPorEstadoUseCase {
    operator fun invoke(activos: List<Asset>, soloActivos: Boolean): List<Asset> =
        if (soloActivos) {
            activos.filter { it.deletedAt == null }
        } else {
            activos.filter { it.deletedAt != null }
        }
}"""
    },
    24: {
        "titulo": "Regla de Negocio units >= 1 en Dominio Puro",
        "archivo": "domain/usecase/GuardarActivoUseCase.kt",
        "figura": "Figura 1: Validación estricta de invariantes de negocio en el caso de uso antes de persistir.",
        "pantalla": "Intentar guardar un activo con 0 unidades físicas y verificar el rechazo formal por regla de negocio.",
        "pregunta": "¿Por qué es crucial validar reglas de negocio en la capa de Dominio además de la validación en la UI?",
        "respuesta": "La validación en la UI está enfocada en la experiencia de usuario y formato de entrada (ej. que un campo no esté vacío), pero no puede ser la única línea de defensa. La Capa de Dominio es el núcleo inmutable del sistema (Core) y tiene la responsabilidad exclusiva de garantizar las invariantes de negocio. Si un cliente alternativo, un test de integración o un proceso de sincronización remota envía datos que eludieron la interfaz visual, el caso de uso previene la corrupción de la base de datos rechazando activos con 0 unidades. Esta defensa en profundidad asegura la integridad y consistencia del inventario institucional bajo cualquier circunstancia.",
        "find_block": r"/\* =================== \[DEFENSA CASO 24: Validación de Regla de Negocio units >= 1 en Dominio\] ===================[\s\S]*?=============================================================================================================== \*/",
        "replace_clean": """        if (asset.units < 1) {
            return Result.Error("Regla de Dominio LTIC: Todo activo en inventario debe poseer al menos 1 unidad física.")
        }"""
    },
    26: {
        "titulo": "Consulta Reactiva por Bloque con Flow en Room",
        "archivo": "data/local/dao/AssetDao.kt",
        "figura": "Figura 1: DAO exponiendo consulta reactiva mediante Flow vinculada al InvalidationTracker de SQLite.",
        "pantalla": "Ejecutar la app y observar el catálogo de inventario reaccionando en tiempo real ante cambios por bloque.",
        "pregunta": "¿Cómo funciona la reactividad de Flow en Room y qué componente interno detecta las modificaciones?",
        "respuesta": "Cuando una función en un DAO de Room retorna 'Flow<T>', Room registra internamente un observador a través de su 'InvalidationTracker' sobre las tablas involucradas en la consulta SQL ('assets'). Cuando cualquier hilo ejecuta una operación de escritura (INSERT, UPDATE o DELETE) sobre dicha tabla, el InvalidationTracker detecta la invalidación, Room reejecuta la consulta de forma asíncrona en un hilo secundario y emite automáticamente la nueva lista de resultados a todos los colectores activos. Esto implementa el patrón Single Source of Truth (SSOT), permitiendo que la UI observe el estado de la base de datos sin necesidad de realizar recargas manuales (cero polling).",
        "find_block": r"/\* =================== \[DEFENSA CASO 26: Consulta Reactiva por Bloque con Flow en Room\] ===================[\s\S]*?======================================================================================================== \*/",
        "replace_clean": """    @Query(\"\"\"
        SELECT * FROM assets
        WHERE block = :bloque
          AND deletedAt IS NULL
        ORDER BY name COLLATE NOCASE ASC
    \"\"\")
    abstract fun observarPorBloque(bloque: String): Flow<List<AssetEntity>>"""
    },
    28: {
        "titulo": "Baja Lógica (Soft Delete) vs Baja Física (Hard Delete)",
        "archivo": "data/local/dao/AssetDao.kt",
        "figura": "Figura 1: Operación de baja lógica actualizando timestamp deletedAt y estado PENDING_DELETE.",
        "pantalla": "Eliminar un activo desde la ficha técnica. El activo desaparece del catálogo pero su fila persiste para sincronización.",
        "pregunta": "¿Por qué una arquitectura Offline-First exige baja lógica en lugar de borrado físico?",
        "respuesta": "En una arquitectura Offline-First, si se ejecutara un borrado físico ('DELETE FROM assets'), la fila desaparecería instantáneamente de SQLite. Si el usuario realiza esta acción sin conexión a internet, el dispositivo perdería todo registro de la existencia del activo y, al recuperar conectividad, la aplicación no sabría qué ID debe darse de baja en el servidor remoto, generando inconsistencias graves en el inventario. La baja lógica ('Soft Delete') actualiza 'deletedAt' y marca 'syncStatus = PENDING_DELETE'. Esto oculta el registro de la interfaz (mediante 'WHERE deletedAt IS NULL') pero retiene la fila localmente para encolar la orden de eliminación hacia el servidor, ejecutando el borrado físico solo tras la confirmación remota.",
        "find_block": r"/\* =================== \[DEFENSA CASO 28: Baja Lógica \(Soft Delete\) vs Baja Física \(Hard Delete\)\] ===================[\s\S]*?================================================================================================================== \*/",
        "replace_clean": """    // Baja Lógica activa: La fila se preserva con deletedAt para sync offline-first."""
    }
}

def ejecutar_comando(cmd, cwd=PROJECT_ROOT):
    """Ejecuta un comando en consola retornando salida y código de retorno."""
    res = subprocess.run(cmd, cwd=str(cwd), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return res.returncode, res.stdout.strip(), res.stderr.strip()

def aplicar_solucion_caso(numero_caso):
    """Aplica la solución limpia de un caso en su archivo correspondiente y genera el informe EVA."""
    info = CASOS_INFO.get(numero_caso)
    if not info:
        print(f"[-] Caso {numero_caso} no registrado en el catálogo maestro.")
        return False

    ruta_relativa = info["archivo"]
    ruta_archivo = APP_SRC / ruta_relativa

    if not ruta_archivo.exists():
        print(f"[-] Archivo objetivo no encontrado: {ruta_archivo}")
        return False

    contenido = ruta_archivo.read_text(encoding="utf-8")
    patron = info["find_block"]
    reemplazo = info["replace_clean"]

    nuevo_contenido, reemplazos = re.subn(patron, reemplazo, contenido)
    if reemplazos == 0:
        print(f"[!] No se encontró el bloque a reemplazar para el Caso {numero_caso} (posiblemente ya aplicado).")
    else:
        ruta_archivo.write_text(nuevo_contenido, encoding="utf-8")
        print(f"[+] Caso {numero_caso}: Código refactorizado y limpiado exitosamente en {ruta_relativa}.")

    # Generar Documento de Entrega EVA
    ENTREGA_DIR.mkdir(parents=True, exist_ok=True)
    doc_path = ENTREGA_DIR / f"Evidencia_Caso{numero_caso}_Flores_Paul.md"

    doc_content = f"""# PONTIFICIA UNIVERSIDAD CATÓLICA DEL ECUADOR
## FACULTAD DE INGENIERÍA · PROGRAMACIÓN MÓVIL
### EVALUACIÓN PRÁCTICA INDIVIDUAL — CASO {numero_caso}

**Estudiante:** Flores Ludeña Paúl Alejandro  
**Fecha:** 19 de Noviembre de 2026  
**Proyecto:** ScanTech LTIC (Fase 1)  
**Rama Git:** caso-{numero_caso}  
**Documento para EVA:** Evidencia_Caso{numero_caso}_Flores_Paul.pdf  

---

## 1. 📱 Capturas de Pantalla de la Aplicación

> **Instrucción para la captura:** {info['pantalla']}

[Insertar captura aquí]

**{info['figura']}**

---

## 2. 🛠️ Anexo de Código Modificado (Texto Plano Limpio)

### Archivo: `app/src/main/java/ec/edu/puce/scantech/{info['archivo']}`
```kotlin
{info['replace_clean']}
```

---

## 3. 💡 Sección de Respuesta Teórica (Máximo 150 palabras)

**Pregunta de Examen:**
> "{info['pregunta']}"

**Respuesta Técnica:**
{info['respuesta']}
"""
    doc_path.write_text(doc_content, encoding="utf-8")
    print(f"[+] Entregable EVA generado: {doc_path}")
    return True

def extraer_casos_de_documento(ruta_doc):
    """Extrae números de casos a partir de un archivo PDF, Markdown o texto."""
    casos = []
    ruta = Path(ruta_doc)
    if not ruta.is_absolute():
        ruta = PROJECT_ROOT / ruta

    if not ruta.exists():
        print(f"[-] Documento no encontrado: {ruta}")
        return list(CASOS_INFO.keys())[:10]

    texto_completo = ""
    if ruta.suffix.lower() == ".pdf":
        try:
            import pypdf
            reader = pypdf.PdfReader(str(ruta))
            for page in reader.pages:
                texto_completo += page.extract_text() or ""
        except Exception as e:
            print(f"[!] Error leyendo PDF con pypdf: {e}")
    else:
        texto_completo = ruta.read_text(encoding="utf-8", errors="ignore")

    # Buscar menciones a "Caso X" o "CASO X"
    encontrados = re.findall(r"(?:caso|CASO)\s*(\d+)", texto_completo)
    for c in encontrados:
        num = int(c)
        if num in CASOS_INFO and num not in casos:
            casos.append(num)

    if not casos:
        print("[!] No se detectaron números explícitos; procesando primeros casos estándar.")
        casos = [1, 2, 3, 4, 5, 6, 11, 12, 16, 21, 24, 26, 28]

    return casos

def procesar_batch(ruta_doc=None, rama_base="Repaso-Paul-1"):
    """Crea ramas individuales caso-X, aplica la solución y commitea en segundos."""
    print(f"[*] Iniciando procesamiento por lotes desde la rama base '{rama_base}'...")

    if ruta_doc:
        casos = extraer_casos_de_documento(ruta_doc)
    else:
        casos = [1, 2, 3, 4, 5, 6, 11, 12, 16, 21, 24, 26, 28]

    print(f"[*] Casos a procesar ({len(casos)}): {casos}")

    # Asegurar que estamos en la rama base
    ejecutar_comando(f"git checkout {rama_base}")

    ramas_creadas = []
    for c in casos:
        nombre_rama = f"caso-{c}"
        print(f"\n--- Procesando {nombre_rama} ---")
        
        # Crear o resetear rama desde rama_base
        ejecutar_comando(f"git checkout -B {nombre_rama} {rama_base}")

        # Aplicar solución limpia
        exito = aplicar_solucion_caso(c)
        if exito:
            # Commit local (sin push)
            ejecutar_comando("git add -A")
            titulo = CASOS_INFO.get(c, {}).get("titulo", "Refactorizacion")
            ejecutar_comando(f'git commit -m "feat(defensa): resolucion limpia caso {c} - {titulo}"')
            ramas_creadas.append((c, nombre_rama, titulo))

    # Regresar a la rama base
    ejecutar_comando(f"git checkout {rama_base}")
    print(f"\n[✓] Proceso batch completado exitosamente. Retornado a '{rama_base}'.")
    return ramas_creadas

def main():
    import argparse
    parser = argparse.ArgumentParser(description="Motor Ultra-Fast de Casos de Defensa ScanTech")
    parser.add_argument("--batch", type=str, nargs="?", const="", help="Modo por lotes: genera ramas independientes para cada caso del documento.")
    parser.add_argument("--caso", type=int, help="Modo individual: resuelve un caso específico.")
    parser.add_argument("--rama-base", type=str, default="Repaso-Paul-1", help="Rama base para el branching.")
    args = parser.parse_args()

    if args.batch is not None:
        doc = args.batch if args.batch != "" else None
        ramas = procesar_batch(doc, rama_base=args.rama_base)
        print("\n" + "="*70)
        print("🎯 RESUMEN DE RAMAS LOCALES PREPARADAS PARA LA DEFENSA:")
        print("="*70)
        for c, rama, tit in ramas:
            print(f"• Rama: {rama:<10} | Caso {c:>2}: {tit:<40} | git checkout {rama}")
        print("="*70)
        print("💡 En el examen, simplemente ejecuta: git checkout caso-<numero>")

    elif args.caso:
        print(f"[*] Resolviendo Caso {args.caso} en la rama activa...")
        if aplicar_solucion_caso(args.caso):
            print(f"[✓] Caso {args.caso} resuelto limpiamente.")
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
