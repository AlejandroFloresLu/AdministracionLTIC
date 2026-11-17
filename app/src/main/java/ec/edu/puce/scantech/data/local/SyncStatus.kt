package ec.edu.puce.scantech.data.local

/**
 * [RDA-3 Tema 9: Clean Architecture - Inversión de Dependencias]
 * Redirecciona al modelo puro del dominio [ec.edu.puce.scantech.domain.model.SyncStatus].
 * La capa de datos depende del dominio, garantizando que las reglas de negocio sean independientes
 * de la infraestructura técnica (Room / SQLite).
 */
typealias SyncStatus = ec.edu.puce.scantech.domain.model.SyncStatus
