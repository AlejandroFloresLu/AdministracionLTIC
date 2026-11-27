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
import ec.edu.puce.scantech.domain.model.ResumenInventario
import ec.edu.puce.scantech.domain.usecase.ObtenerActivosUseCase
import ec.edu.puce.scantech.domain.usecase.ObtenerResumenInventarioUseCase
import ec.edu.puce.scantech.domain.usecase.SincronizarAhoraUseCase
import ec.edu.puce.scantech.domain.sync.EstadoSincronizacion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

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
    obtenerResumenInventarioUseCase: ObtenerResumenInventarioUseCase,
    private val sincronizarAhoraUseCase: SincronizarAhoraUseCase
) : ViewModel() {

    /**
     * [Caso 3] Estado de la pantalla, estructurado e inmutable: todas sus propiedades son `val`.
     * Para cambiar algo, el ViewModel emite una copia nueva con `copy()`; nadie lo modifica en sitio.
     */
    data class ScreenState(
        val titleText: String = "Módulo Inventario LTIC",
        val isProcessing: Boolean = false
    )

    /** [Caso 3] Fuente única de verdad (SSOT): solo el ViewModel puede escribir aquí. */
    private val _screenState = MutableStateFlow(ScreenState())

    /** [Caso 3] Exposición de solo lectura: la UI observa el estado pero no puede reasignar `.value`. */
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    /** Estado de la sincronización manual: el botón muestra un indicador mientras [EstadoSincronizacion.enProgreso]. */
    private val _estadoSync = MutableStateFlow<EstadoSincronizacion>(EstadoSincronizacion.Inactiva)
    val estadoSync: StateFlow<EstadoSincronizacion> = _estadoSync.asStateFlow()

    /** Resultado de una sincronización pedida con el botón, para mostrarlo una vez en un Snackbar. */
    private val _resultadoSync = MutableStateFlow<EstadoSincronizacion?>(null)
    val resultadoSync: StateFlow<EstadoSincronizacion?> = _resultadoSync.asStateFlow()

    /** `true` desde que el usuario toca el botón hasta que llega el resultado de esa ronda. */
    private var esperandoResultado = false

    /** Ronda cuyo resultado ya estaba a la vista al tocar el botón; no se vuelve a anunciar. */
    private var rondaAnterior: String? = null

    init {
        viewModelScope.launch {
            sincronizarAhoraUseCase.observarEstado().collect { estado ->
                _estadoSync.value = estado
                // Caso 3: el ViewModel actualiza el estado inmutable con una copia (UDF)
                _screenState.update { it.copy(isProcessing = estado.enProgreso) }
                val ronda = estado.ronda()
                if (esperandoResultado && ronda != null && ronda != rondaAnterior) {
                    esperandoResultado = false
                    _resultadoSync.value = estado
                }
            }
        }
    }

    /** Disparador manual. Se ignora si ya hay una sincronización en camino. */
    fun sincronizarAhora() {
        if (_estadoSync.value.enProgreso) return
        rondaAnterior = _estadoSync.value.ronda()
        esperandoResultado = true
        sincronizarAhoraUseCase()
    }

    /** La UI ya mostró [resultadoSync]. */
    fun resultadoSyncMostrado() {
        _resultadoSync.value = null
    }

    private fun EstadoSincronizacion.ronda(): String? = when (this) {
        is EstadoSincronizacion.Completada -> ronda
        is EstadoSincronizacion.Fallida -> ronda
        else -> null
    }

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")
    val selectedCategory: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_SELECTED_CATEGORY, CATEGORIA_TODAS)

    /** Se incrementa en [reintentar] para volver a suscribirse a Room tras un error. */
    private val reintentos = MutableStateFlow(0)

    /**
     * Totales del catálogo completo. El cálculo corre en Dispatchers.Default
     * (dentro del caso de uso); aquí solo se convierte en StateFlow para la UI.
     */
    val resumen: StateFlow<ResumenInventario> = obtenerResumenInventarioUseCase()
        .catch { emit(ResumenInventario()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ResumenInventario()
        )

    private val textoConDebounce = searchQuery
        .debounce { texto -> if (texto.isBlank()) 0L else DEBOUNCE_BUSQUEDA_MS }
        .map { it.trim() }
        .distinctUntilChanged()

    val uiState: StateFlow<InventoryUiState> =
        combine(textoConDebounce, selectedCategory, reintentos) { texto, categoria, _ ->
            Filtro(texto, categoria.takeUnless { it.equals(CATEGORIA_TODAS, ignoreCase = true) })
        }
            .flatMapLatest { filtro ->
                obtenerActivosUseCase(query = filtro.texto, category = filtro.categoria)
                    .map<List<Asset>, InventoryUiState> {
                        InventoryUiState.Success(activos = it, hayFiltros = filtro.activo)
                    }
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
    fun reintentar() {
        reintentos.value++
    }

    private data class Filtro(val texto: String, val categoria: String?) {
        val activo: Boolean get() = texto.isNotEmpty() || categoria != null
    }

    companion object {
        private const val KEY_SEARCH_QUERY = "key_inventory_search_query"
        private const val KEY_SELECTED_CATEGORY = "key_inventory_selected_category"

        /** Espera tras la última tecla antes de consultar Room. */
        const val DEBOUNCE_BUSQUEDA_MS = 300L

        /** Valor del chip que desactiva el filtro por categoría (coincide con `R.string.category_all`). */
        const val CATEGORIA_TODAS = "Todos"

        /** Factory que toma el repositorio compartido del [ec.edu.puce.scantech.di.AppContainer]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                val repository = container.assetRepository
                InventoryViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    obtenerActivosUseCase = ObtenerActivosUseCase(repository),
                    obtenerResumenInventarioUseCase = ObtenerResumenInventarioUseCase(repository),
                    sincronizarAhoraUseCase = SincronizarAhoraUseCase(container.programadorSync)
                )
            }
        }
    }
}
