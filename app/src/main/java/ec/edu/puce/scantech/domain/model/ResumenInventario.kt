package ec.edu.puce.scantech.domain.model

/**
 * [Caso de práctica 8] Totales del inventario que se muestran en la cabecera del catálogo.
 */
data class ResumenInventario(
    val totalActivos: Int = 0,
    val totalUnidades: Int = 0,
    val pendientesSync: Int = 0
)
