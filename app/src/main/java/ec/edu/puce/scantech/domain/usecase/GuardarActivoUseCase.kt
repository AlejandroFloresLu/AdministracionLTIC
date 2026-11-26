package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.validacion.CampoActivo
import ec.edu.puce.scantech.domain.validacion.DatosActivo
import ec.edu.puce.scantech.domain.validacion.ResultadoValidacion
import ec.edu.puce.scantech.domain.validacion.ValidadorActivo

/** Resultado de intentar guardar un activo. */
sealed interface ResultadoGuardado {
    /** El activo quedó en Room y en la cola de sincronización. */
    data class Guardado(val activo: Asset) : ResultadoGuardado

    /** Datos inválidos: un mensaje por campo para mostrar en el formulario. No se escribió nada. */
    data class Invalido(val errores: Map<CampoActivo, String>) : ResultadoGuardado

    /** Falló la lectura o la escritura en Room. No se escribió nada. */
    data class Fallo(val mensaje: String) : ResultadoGuardado
}

/**
 * [RDA-3 Tema 9: Clean Architecture - Casos de Uso y Orquestación de Negocio]
 * [RDA-3 Temas 11 y 12: Estrategia de Persistencia Offline-First]
 *
 * Valida y persiste (crea o actualiza) un activo (Estrategia de Datos §5).
 *
 * 1. Reglas puras de [ValidadorActivo]: obligatorios, units >= 0, formato del QR y longitud
 *    de observaciones.
 * 2. Unicidad del QR: solo si el formato es válido se consulta Room, excluyendo el propio activo
 *    al editar. Si la consulta falla no se asume que el QR está libre: se devuelve [ResultadoGuardado.Fallo].
 * 3. Solo si no hay ningún error se escribe en Room (`create` o `update`, que también encolan la
 *    operación de sincronización).
 */
class GuardarActivoUseCase(
    private val repository: AssetRepository
) {
    /**
     * @param asset Activo listo para persistir.
     * @param isEditMode `true` para actualizar, `false` para registrar uno nuevo.
     */
    suspend operator fun invoke(asset: Asset, isEditMode: Boolean = false): ResultadoGuardado {
        val qrYaExiste = if (ValidadorActivo.formatoQrValido(asset.qrCode)) {
            val excludeId = if (isEditMode) asset.id else null
            when (val existe = repository.existsQrCode(qrCode = asset.qrCode, excludeId = excludeId)) {
                is Result.Success -> existe.data
                is Result.Error -> return ResultadoGuardado.Fallo(
                    "No se pudo verificar si el código QR ya existe. Intenta de nuevo."
                )
            }
        } else {
            false
        }

        val datos = DatosActivo(
            qrCode = asset.qrCode,
            nombre = asset.name,
            categoria = asset.category,
            units = asset.units.toString(),
            observaciones = asset.observations ?: ""
        )
        val validacion = ValidadorActivo.validar(datos, qrYaExiste = qrYaExiste)
        if (validacion is ResultadoValidacion.Invalido) {
            return ResultadoGuardado.Invalido(validacion.errores)
        }

        val escritura = if (isEditMode) repository.update(asset) else repository.create(asset)
        return when (escritura) {
            is Result.Success -> ResultadoGuardado.Guardado(escritura.data)
            is Result.Error -> ResultadoGuardado.Fallo(escritura.message)
        }
    }
}
