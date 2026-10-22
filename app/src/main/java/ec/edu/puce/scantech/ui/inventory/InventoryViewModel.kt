package ec.edu.puce.scantech.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.model.ActivoLTIC
import ec.edu.puce.scantech.data.model.EstadoOperativo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel que orquesta la lógica y el flujo de datos unidireccional (UDF) del Catálogo de Inventario.
 *
 * Buenas Prácticas Aplicadas:
 * 1. [SavedStateHandle]: Guarda y restaura de forma persistente la consulta de búsqueda y la categoría
 *    seleccionada para sobrevivir tanto a cambios de configuración (rotación) como a la muerte del
 *    proceso del sistema (Process Death - RDA-1 Tema 4).
 * 2. [Corrutinas en Dispatchers.IO]: Las operaciones de datos y filtrado pesado se delegan a hilos de E/S,
 *    garantizando 0 ms de bloqueo en el hilo principal y eliminando cualquier riesgo de ANR.
 * 3. [StateFlow inmutable]: Expone estados de UI inmutables para que la capa de presentación solo
 *    reaccione pasivamente a los cambios.
 */
class InventoryViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    companion object {
        private const val KEY_SEARCH_QUERY = "key_inventory_search_query"
        private const val KEY_SELECTED_CATEGORY = "key_inventory_selected_category"
    }

    // Estado efímero blindado contra Process Death
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")
    val selectedCategory: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SELECTED_CATEGORY, "Todos")

    // Lista maestra de activos en memoria (en producción provista por Room DAO)
    private val _masterActivos = MutableStateFlow<List<ActivoLTIC>>(emptyList())

    // Estado de UI expuesto mediante StateFlow
    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Idle)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadActivos()
        observeFilters()
    }

    /**
     * Carga inicial de activos tecnológicos simulando lectura desde Room en segundo plano.
     */
    fun loadActivos() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = InventoryUiState.Loading

            try {
                // Simulación de latencia de consulta local SQLite Room
                val activos = getInitialMockActivos()
                _masterActivos.value = activos
                applyFilters(activos, searchQuery.value, selectedCategory.value)
            } catch (e: Exception) {
                _uiState.value = InventoryUiState.Error(
                    mensaje = e.localizedMessage ?: "Error desconocido al acceder a la base local Room"
                )
            }
        }
    }

    /**
     * Reacciona reactivamente a los cambios en la búsqueda o en el filtro de categorías.
     */
    private fun observeFilters() {
        viewModelScope.launch {
            combine(_masterActivos, searchQuery, selectedCategory) { activos, query, category ->
                Triple(activos, query, category)
            }.collect { (activos, query, category) ->
                if (_uiState.value !is InventoryUiState.Loading && activos.isNotEmpty()) {
                    withContext(Dispatchers.Default) {
                        applyFilters(activos, query, category)
                    }
                }
            }
        }
    }

    /**
     * Aplica el filtrado por texto (búsqueda) y por categoría sin bloquear la UI.
     */
    private fun applyFilters(activos: List<ActivoLTIC>, query: String, category: String) {
        val trimmedQuery = query.trim().lowercase()

        val filtrados = activos.filter { activo ->
            val matchCategory = category == "Todos" || activo.categoria.equals(category, ignoreCase = true)
            val matchQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                activo.nombre.lowercase().contains(trimmedQuery) ||
                        activo.serie.lowercase().contains(trimmedQuery) ||
                        activo.id.lowercase().contains(trimmedQuery) ||
                        activo.qrCode.lowercase().contains(trimmedQuery)
            }
            matchCategory && matchQuery
        }

        _uiState.value = InventoryUiState.Success(
            activos = activos,
            filteredActivos = filtrados
        )
    }

    /**
     * Actualiza la consulta de búsqueda y la persiste en [SavedStateHandle].
     */
    fun onSearchQueryChange(newQuery: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = newQuery
    }

    /**
     * Actualiza la categoría seleccionada y la persiste en [SavedStateHandle].
     */
    fun onCategorySelect(newCategory: String) {
        savedStateHandle[KEY_SELECTED_CATEGORY] = newCategory
    }

    /**
     * Limpia la consulta de búsqueda.
     */
    fun clearSearchQuery() {
        savedStateHandle[KEY_SEARCH_QUERY] = ""
    }

    /**
     * Busca un activo por su ID para navegar a la pantalla de detalle.
     */
    fun getActivoById(assetId: String): ActivoLTIC? {
        return _masterActivos.value.find { it.id == assetId }
    }

    /**
     * Actualiza las observaciones físicas de un activo (cambio que marca el activo como PENDING_UPDATE
     * dentro de la estrategia Offline-First).
     */
    fun updateObservaciones(assetId: String, newObservaciones: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedList = _masterActivos.value.map { activo ->
                if (activo.id == assetId) {
                    activo.copy(
                        observaciones = newObservaciones,
                        syncStatus = SyncStatus.PENDING_UPDATE,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    activo
                }
            }
            _masterActivos.value = updatedList
            applyFilters(updatedList, searchQuery.value, selectedCategory.value)
        }
    }

    /**
     * Datos iniciales de activos LTIC basados en el documento maestro y las diapositivas de presentación.
     */
    private fun getInitialMockActivos(): List<ActivoLTIC> {
        return listOf(
            ActivoLTIC(
                id = "LTIC-2048",
                qrCode = "LTIC-2048",
                nombre = "Laptop Dell Latitude 5440",
                serie = "ABC123XYZ",
                categoria = "Laptops",
                marca = "Dell",
                modelo = "Latitude 5440",
                ram = "16GB DDR5",
                cpu = "Intel Core i7-1365U",
                ubicacion = "Bodega Central - Estante A1",
                observaciones = "Equipo con leve desgaste cosmético en la bisagra superior.",
                estadoOperativo = EstadoOperativo.ADVERTENCIA,
                syncStatus = SyncStatus.SYNCED
            ),
            ActivoLTIC(
                id = "LTIC-000123",
                qrCode = "LTIC-000123",
                nombre = "Servidor HP ProLiant DL360 Gen10",
                serie = "XYZ987SER",
                categoria = "Servidores",
                marca = "HP",
                modelo = "ProLiant DL360 Gen10",
                ram = "64GB ECC",
                cpu = "Dual Intel Xeon Silver 4210R",
                ubicacion = "Data Center LTIC - Rack 04",
                observaciones = "Módulo de fuente redundante descalibrado, requiere reemplazo urgente.",
                estadoOperativo = EstadoOperativo.CRITICO,
                syncStatus = SyncStatus.PENDING_UPDATE
            ),
            ActivoLTIC(
                id = "LTIC-000456",
                qrCode = "LTIC-000456",
                nombre = "Switch Cisco Catalyst 2960-X",
                serie = "SW-001CISCO",
                categoria = "Redes",
                marca = "Cisco",
                modelo = "Catalyst 2960-X 48 GigE",
                ram = "512MB",
                cpu = "APM86392 600MHz",
                ubicacion = "Laboratorio Redes Piso 2",
                observaciones = "Puertos 1-48 validados con tráfico normal.",
                estadoOperativo = EstadoOperativo.OPERATIVO,
                syncStatus = SyncStatus.SYNCED
            ),
            ActivoLTIC(
                id = "LTIC-000789",
                qrCode = "LTIC-000789",
                nombre = "Laptop Lenovo ThinkPad T14 Gen 4",
                serie = "LEN-TP-8841",
                categoria = "Laptops",
                marca = "Lenovo",
                modelo = "ThinkPad T14 Gen 4",
                ram = "32GB LPDDR5",
                cpu = "AMD Ryzen 7 PRO 7840U",
                ubicacion = "Bodega Central - Estante A2",
                observaciones = "Equipo nuevo recibido en recepción.",
                estadoOperativo = EstadoOperativo.OPERATIVO,
                syncStatus = SyncStatus.PENDING_CREATE
            ),
            ActivoLTIC(
                id = "LTIC-000312",
                qrCode = "LTIC-000312",
                nombre = "Router Cisco 4331 ISR",
                serie = "ISR-4331-K9",
                categoria = "Redes",
                marca = "Cisco",
                modelo = "4331 Integrated Services Router",
                ram = "4GB",
                cpu = "Multi-Core x86",
                ubicacion = "Armario de Comunicaciones Sótano",
                observaciones = "Ventilador secundario con ruido anómalo.",
                estadoOperativo = EstadoOperativo.ADVERTENCIA,
                syncStatus = SyncStatus.SYNCED
            ),
            ActivoLTIC(
                id = "LTIC-000990",
                qrCode = "LTIC-000990",
                nombre = "Lector de Código de Barras Honeywell Xenon 1900",
                serie = "HON-XEN-99",
                categoria = "Periféricos",
                marca = "Honeywell",
                modelo = "Xenon 1900g SR",
                ram = "N/A",
                cpu = "Embedded",
                ubicacion = "Mesa de Auditoría LTIC",
                observaciones = "Cable USB reemplazado y calibrado.",
                estadoOperativo = EstadoOperativo.OPERATIVO,
                syncStatus = SyncStatus.SYNCED
            )
        )
    }
}
