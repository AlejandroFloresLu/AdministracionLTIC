package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.Result
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.repository.AssetRepository
import ec.edu.puce.scantech.domain.validacion.DatosActivo
import ec.edu.puce.scantech.domain.validacion.ResultadoValidacion
import ec.edu.puce.scantech.domain.validacion.ValidadorActivo

/**
 * [RDA-3 Tema 9: Clean Architecture - Casos de Uso y Orquestación de Negocio]
 * [RDA-3 Temas 11 y 12: Estrategia de Persistencia Offline-First]
 *
 * Caso de uso responsable de validar y persistir (crear o actualizar) un activo en el inventario.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Validación de Dominio Previa: Aplica las reglas de negocio de [ValidadorActivo] antes de intentar
 *    cualquier escritura en base de datos.
 * 2. Unicidad de Código QR: Comprueba mediante el repositorio si el código QR ya está registrado en
 *    otro activo, excluyendo el ID actual en caso de edición para permitir actualizar sin falsos conflictos.
 * 3. Transaccionalidad Offline-First: Si es una creación, invoca `create(asset)`. Si es una edición,
 *    invoca `update(asset)`. En ambos casos, la capa de datos actualiza Room e inserta la operación en la
 *    cola de sincronización para enviarse cuando haya red disponible.
 */
class GuardarActivoUseCase(
    private val repository: AssetRepository
) {
    /**
     * Valida y guarda el activo en el inventario.
     *
     * @param asset Activo estructurado listo para persistir.
     * @param isEditMode Indica si se trata de una actualización (`true`) o de un registro nuevo (`false`).
     * @return [Result.Success] con el activo persistido o [Result.Error] con la causa del fallo.
     */
    suspend operator fun invoke(asset: Asset, isEditMode: Boolean = false): Result<Asset> {
        /* =================== [DEFENSA CASO 24: Validación de Regla de Negocio units >= 1 en Dominio] ===================
        // 🛠️ PASO PARA ACTIVAR EN EL EXAMEN:
        // Descomenta esta regla de negocio estricta al inicio de este caso de uso:
        // if (asset.units < 1) {
        //     return Result.Error("Regla de Dominio LTIC: Todo activo en inventario debe poseer al menos 1 unidad física.")
        // }

        // 💡 RESPUESTA TEÓRICA RÁPIDA:
        // - Validación de UI vs Validación de Dominio: La UI valida formato de entrada (ej. que no esté vacío),
        //   mientras que el Caso de Uso hace cumplir las invariantes y reglas de negocio del sistema.
        // - Defensa en Profundidad: Aunque un atacante o cliente alternativo eluda la UI, el caso de uso
        //   garantiza que el estado de la base de datos nunca se corrompa con datos operativamente ilógicos.
        =============================================================================================================== */

        // 1. Verificar unicidad de QR en la base de datos
        val excludeId = if (isEditMode) asset.id else null
        val existeCheck = repository.existsQrCode(qrCode = asset.qrCode, excludeId = excludeId)

        val qrYaExiste = when (existeCheck) {
            is Result.Success -> existeCheck.data
            is Result.Error -> false
        }

        // 2. Ejecutar validación pura de reglas de negocio
        val datosValidacion = DatosActivo(
            qrCode = asset.qrCode,
            nombre = asset.name,
            categoria = asset.category,
            units = asset.units.toString(),
            observaciones = asset.observations ?: ""
        )

        val validacion = ValidadorActivo.validar(datosValidacion, qrYaExiste = qrYaExiste)
        if (validacion is ResultadoValidacion.Invalido) {
            val primerError = validacion.errores.values.firstOrNull() ?: "Datos del activo inválidos"
            return Result.Error(primerError)
        }

        // 3. Persistir en la fuente única de verdad (Room + Cola de Sync)
        return if (isEditMode) {
            repository.update(asset)
        } else {
            repository.create(asset)
        }
    }
}

