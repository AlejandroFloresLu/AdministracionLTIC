package ec.edu.puce.scantech.domain.usecase

import ec.edu.puce.scantech.domain.sync.EstadoSincronizacion
import ec.edu.puce.scantech.domain.sync.ProgramadorSync
import kotlinx.coroutines.flow.Flow

/** Disparador manual de la sincronización (botón del inventario) y su estado. */
class SincronizarAhoraUseCase(
    private val programador: ProgramadorSync
) {
    operator fun invoke() = programador.sincronizarAhora()

    fun observarEstado(): Flow<EstadoSincronizacion> = programador.observarSincronizacionManual()
}
