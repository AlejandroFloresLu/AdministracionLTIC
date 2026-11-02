package ec.edu.puce.scantech.domain.sync

/** Versión de un activo tal como se compara durante la sincronización. */
data class ActivoSincronizable(
    val id: String,
    val updatedAt: Long,
    val units: Int
)

/** Resultado de comparar la copia local con la del servidor. */
sealed interface ResolucionConflicto {

    /** Se conserva la versión del servidor. */
    data class GanaServidor(val activo: ActivoSincronizable) : ResolucionConflicto

    /** Se conserva la versión local (pendiente de subir). */
    data class GanaLocal(val activo: ActivoSincronizable) : ResolucionConflicto

    /** El conflicto se marca para revisión humana; no se resuelve en silencio. */
    data class RevisionManual(
        val local: ActivoSincronizable,
        val servidor: ActivoSincronizable,
        val motivo: String
    ) : ResolucionConflicto
}

/**
 * Política de conflictos de sincronización (Estrategia de Datos §4):
 *
 * 1. Regla general: "última escritura gana" según `updatedAt`.
 * 2. Excepción para `units`: si el servidor cambió `units` respecto de lo que vio el usuario
 *    y además difiere de la copia local, se avisa y se marca para revisión.
 */
object PoliticaConflictos {

    /**
     * @param local versión del dispositivo.
     * @param servidor versión recibida del servidor.
     * @param unitsVisto valor de `units` que el usuario tenía en pantalla al editar, o `null`
     *        si no se conoce.
     */
    fun resolver(
        local: ActivoSincronizable,
        servidor: ActivoSincronizable,
        unitsVisto: Int?
    ): ResolucionConflicto {
        require(local.id == servidor.id) { "Las dos versiones deben ser del mismo activo" }

        val serverCambioUnits = unitsVisto != null && servidor.units != unitsVisto
        val localDifiere = local.units != servidor.units

        if (serverCambioUnits && localDifiere) {
            return ResolucionConflicto.RevisionManual(
                local = local,
                servidor = servidor,
                motivo = "El servidor cambió 'units' de ${unitsVisto} a ${servidor.units}"
            )
        }

        return if (servidor.updatedAt > local.updatedAt) {
            ResolucionConflicto.GanaServidor(servidor)
        } else {
            ResolucionConflicto.GanaLocal(local)
        }
    }
}
