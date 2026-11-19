package ec.edu.puce.scantech.domain.validacion

/** Campos del formulario de activo que pueden tener errores. */
enum class CampoActivo {
    QR_CODE,
    NOMBRE,
    CATEGORIA,
    UNITS,
    OBSERVACIONES
}

/** Datos crudos del formulario, tal como los escribe el usuario. */
data class DatosActivo(
    val qrCode: String,
    val nombre: String,
    val categoria: String,
    val units: String,
    val observaciones: String
)

/** Resultado de validar: sin errores o con un mensaje por campo. */
sealed interface ResultadoValidacion {
    data object Valido : ResultadoValidacion
    data class Invalido(val errores: Map<CampoActivo, String>) : ResultadoValidacion
}

/**
 * Validaciones del CRUD antes de tocar Room (Estrategia de Datos §5).
 *
 * El caller decide si el `qrCode` ya existe en la base y lo pasa como [qrYaExiste]; así este
 * validador no depende de Room y se puede probar en la JVM.
 */
object ValidadorActivo {

    const val MAX_OBSERVACIONES = 500
    private val FORMATO_QR = Regex("^LTIC-\\d{6}$")

    fun validar(datos: DatosActivo, qrYaExiste: Boolean): ResultadoValidacion {
        val errores = mutableMapOf<CampoActivo, String>()

        // --- CÓDIGO ORIGINAL (Comentar si el docente pide activar el Caso 1) ---
        when {
            datos.qrCode.isBlank() -> errores[CampoActivo.QR_CODE] = "El código QR es obligatorio"
            !FORMATO_QR.matches(datos.qrCode.trim()) ->
                errores[CampoActivo.QR_CODE] = "Formato esperado: LTIC-000123"
            qrYaExiste -> errores[CampoActivo.QR_CODE] = "Ya existe un activo con este código"
        }

        /* =================== [DEFENSA CASO 1: Validar QR con .takeIf y .let] ===================
        // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
        // 1. Comenta el bloque "when" original de arriba.
        // 2. Descomenta este bloque de código funcional con funciones de alcance idiomáticas:
        datos.qrCode.trim()
            .takeIf { it.isNotBlank() }
            ?.let { qr ->
                if (!FORMATO_QR.matches(qr))
                    errores[CampoActivo.QR_CODE] = "Formato esperado: LTIC-000123"
                else if (qrYaExiste)
                    errores[CampoActivo.QR_CODE] = "Ya existe un activo con este código"
            }
            ?: run { errores[CampoActivo.QR_CODE] = "El código QR es obligatorio" }

        // 💡 RESPUESTA TEÓRICA RÁPIDA:
        // - '.let {}' transforma el objeto receptor ('it') y retorna el resultado del lambda. Se usa para
        //   operaciones nulas seguras (?.) y cambios de tipo o formato.
        // - '.also {}' ejecuta un efecto secundario (logging, depuración) y retorna el objeto receptor sin modificarlo.
        // - '.takeIf { pred }' retorna el objeto si cumple la condición o null si no, habilitando el operador Elvis (?:).
        ======================================================================================= */

        if (datos.nombre.isBlank()) {
            errores[CampoActivo.NOMBRE] = "El nombre es obligatorio"
        }

        if (datos.categoria.isBlank()) {
            errores[CampoActivo.CATEGORIA] = "Selecciona una categoría"
        }

        // --- CÓDIGO ORIGINAL (Comentar si el docente pide activar el Caso 2) ---
        val unidades = datos.units.trim().toIntOrNull()
        when {
            unidades == null -> errores[CampoActivo.UNITS] = "Ingresa un número entero"
            unidades < 0 -> errores[CampoActivo.UNITS] = "Las unidades no pueden ser negativas"
        }

        /* =================== [DEFENSA CASO 2: Conversión segura con toIntOrNull y Elvis] ===================
        // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
        // 1. Comenta la asignación y el bloque when de "unidades" original de arriba.
        // 2. Descomenta este bloque con feedback detallado del valor erróneo:
        val unidadesSeguras = datos.units.trim().toIntOrNull()
        when {
            unidadesSeguras == null -> errores[CampoActivo.UNITS] =
                "\"${datos.units.trim()}\" no es un número entero válido"
            unidadesSeguras < 0 -> errores[CampoActivo.UNITS] = "Las unidades no pueden ser negativas"
            unidadesSeguras > 9999 -> errores[CampoActivo.UNITS] = "Máximo 9 999 unidades por activo"
        }

        // 💡 RESPUESTA TEÓRICA RÁPIDA:
        // - 'toIntOrNull()' previene 'NumberFormatException' en tiempo de ejecución al parsear texto de usuario.
        // - En Compose/Android, una excepción no controlada en el hilo principal provoca un Fatal Crash (ANR/Cierre forzoso).
        // - Retornar 'null' permite usar el operador Elvis '?:' o un 'when' para manejar el error de forma reactiva y elegante.
        ================================================================================================= */

        if (datos.observaciones.length > MAX_OBSERVACIONES) {
            errores[CampoActivo.OBSERVACIONES] =
                "Máximo $MAX_OBSERVACIONES caracteres"
        }

        return if (errores.isEmpty()) {
            ResultadoValidacion.Valido
        } else {
            ResultadoValidacion.Invalido(errores.toMap())
        }
    }
}

/* =================== [DEFENSA CASO 3: Función de Extensión String.esCodigoQrLtic()] ===================
// 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
// 1. Descomenta esta función de extensión al final de este archivo.
// 2. En la validación de QR de arriba, reemplaza:
//    !FORMATO_QR.matches(datos.qrCode.trim())
//    por:
//    !datos.qrCode.esCodigoQrLtic()

// fun String.esCodigoQrLtic(): Boolean {
//     val regex = Regex("^LTIC-\\d{6}$")
//     return regex.matches(this.trim())
// }

// 💡 RESPUESTA TEÓRICA RÁPIDA:
// - Las funciones de extensión permiten agregar funcionalidad a clases existentes (como String) sin heredar de ellas
//   ni modificar su código fuente original (principio Open/Closed de SOLID).
// - En bytecode de JVM, se compilan como métodos estáticos donde el primer parámetro es el objeto receptor ('this').
// - No pueden acceder a miembros privados ('private') de la clase que extienden, garantizando encapsulación estricta.
======================================================================================================= */
