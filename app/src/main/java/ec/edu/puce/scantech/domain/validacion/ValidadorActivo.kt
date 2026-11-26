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
    const val MENSAJE_QR_DUPLICADO = "Ya existe un activo con este código"

    /** `true` si el QR tiene el formato `LTIC-000123`; solo así vale la pena consultar su unicidad. */
    fun formatoQrValido(qrCode: String): Boolean = FORMATO_QR.matches(qrCode.trim())

    fun validar(datos: DatosActivo, qrYaExiste: Boolean): ResultadoValidacion {
        val errores = mutableMapOf<CampoActivo, String>()

        when {
            datos.qrCode.isBlank() -> errores[CampoActivo.QR_CODE] = "El código QR es obligatorio"
            !formatoQrValido(datos.qrCode) ->
                errores[CampoActivo.QR_CODE] = "Formato esperado: LTIC-000123"
            qrYaExiste -> errores[CampoActivo.QR_CODE] = MENSAJE_QR_DUPLICADO
        }

        if (datos.nombre.isBlank()) {
            errores[CampoActivo.NOMBRE] = "El nombre es obligatorio"
        }

        if (datos.categoria.isBlank()) {
            errores[CampoActivo.CATEGORIA] = "Selecciona una categoría"
        }

        // Validación estricta: toIntOrNull() devuelve null ante letras o vacío (sin excepción).
        // El valor seguro con Elvis para mostrar en pantalla vive en AssetFormViewModel.procesarUnidades().
        val unidades = datos.units.trim().toIntOrNull()
        when {
            unidades == null -> errores[CampoActivo.UNITS] = "Ingresa un número entero"
            unidades < 0 -> errores[CampoActivo.UNITS] = "Las unidades no pueden ser negativas"
        }

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
