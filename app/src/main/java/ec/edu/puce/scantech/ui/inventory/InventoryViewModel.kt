package ec.edu.puce.scantech.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ec.edu.puce.scantech.appContainer
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.usecase.ObtenerActivosPendientesUseCase
import ec.edu.puce.scantech.domain.usecase.ObtenerActivosUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel del Catálogo de Inventario: búsqueda y filtros reactivos sobre Room
 * (Estrategia de Datos §3.c).
 *
 * Flujo:
 * 1. [searchQuery] y [selectedCategory] viven en [SavedStateHandle], así sobreviven a la rotación
 *    y a la muerte del proceso.
 * 2. El texto pasa por `debounce` ([DEBOUNCE_BUSQUEDA_MS]) para no consultar Room en cada tecla;
 *    borrar la búsqueda se aplica al instante. La categoría no lleva debounce (es un clic).
 * 3. `combine` une texto y categoría; `flatMapLatest` cancela la consulta anterior y se suscribe
 *    a la nueva. El filtrado lo hace SQLite (`AssetDao.observeAssets`), no la memoria.
 * 4. Como Room devuelve un `Flow`, la lista se actualiza sola cuando cambia la base
 *    (alta, edición, baja o sincronización), sin recargar a mano.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val obtenerActivosUseCase: ObtenerActivosUseCase,
    private val obtenerActivosPendientesUseCase: ObtenerActivosPendientesUseCase
) : ViewModel() {

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")
    val selectedCategory: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_SELECTED_CATEGORY, CATEGORIA_TODAS)

    /**
     * [Caso de práctica 1] Estado del Switch "Solo pendientes". Vive en SavedStateHandle
     * para sobrevivir a la rotación y a la muerte del proceso, igual que la búsqueda.
     */
    val soloPendientes: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_SOLO_PENDIENTES, false)

    /** Se incrementa en [reintentar] para volver a suscribirse a Room tras un error. */
    private val reintentos = MutableStateFlow(0)

    private val textoConDebounce = searchQuery
        .debounce { texto -> if (texto.isBlank()) 0L else DEBOUNCE_BUSQUEDA_MS }
        .map { it.trim() }
        .distinctUntilChanged()

    val uiState: StateFlow<InventoryUiState> =
        combine(textoConDebounce, selectedCategory, soloPendientes, reintentos) { texto, categoria, pendientes, _ ->
            Filtro(
                texto = texto,
                categoria = categoria.takeUnless { it.equals(CATEGORIA_TODAS, ignoreCase = true) },
                soloPendientes = pendientes
            )
        }
            .flatMapLatest { filtro ->
                // flatMapLatest cancela la consulta anterior y se suscribe al flujo que corresponda
                val fuente = if (filtro.soloPendientes) {
                    obtenerActivosPendientesUseCase(query = filtro.texto, category = filtro.categoria)
                } else {
                    obtenerActivosUseCase(query = filtro.texto, category = filtro.categoria)
                }
                fuente
                    .map<List<Asset>, InventoryUiState> { InventoryUiState.Success(it) }
                    .catch { e ->
                        emit(InventoryUiState.Error(e.message ?: "No se pudo leer el inventario local"))
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InventoryUiState.Loading
            )

    fun onSearchQueryChange(newQuery: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = newQuery
    }

    fun onCategorySelect(newCategory: String) {
        savedStateHandle[KEY_SELECTED_CATEGORY] = newCategory
    }

    fun clearSearchQuery() {
        savedStateHandle[KEY_SEARCH_QUERY] = ""
    }

    /** Vuelve a mostrar todo el catálogo: sin texto y con la categoría "Todos". */
    fun limpiarFiltros() {
        clearSearchQuery()
        onCategorySelect(CATEGORIA_TODAS)
    }

    /** Reintenta la consulta después de un error de lectura. */
    /** [Caso de práctica 1] Evento del Switch: la UI solo avisa, el ViewModel decide. */
    fun onSoloPendientesChange(activo: Boolean) {
        savedStateHandle[KEY_SOLO_PENDIENTES] = activo
    }

    fun reintentar() {
        reintentos.value++
    }

    private data class Filtro(
        val texto: String,
        val categoria: String?,
        val soloPendientes: Boolean
    )

    companion object {
        private const val KEY_SEARCH_QUERY = "key_inventory_search_query"
        private const val KEY_SELECTED_CATEGORY = "key_inventory_selected_category"
        private const val KEY_SOLO_PENDIENTES = "key_inventory_solo_pendientes"

        /** Espera tras la última tecla antes de consultar Room. */
        const val DEBOUNCE_BUSQUEDA_MS = 300L

        /** Valor del chip que desactiva el filtro por categoría (coincide con `R.string.category_all`). */
        const val CATEGORIA_TODAS = "Todos"

        /** Factory que toma el repositorio compartido del [ec.edu.puce.scantech.di.AppContainer]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = appContainer().assetRepository
                InventoryViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    obtenerActivosUseCase = ObtenerActivosUseCase(repository),
                    obtenerActivosPendientesUseCase = ObtenerActivosPendientesUseCase(repository)
                )
            }
        }
    }
}
