package ec.edu.puce.scantech.ui.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ec.edu.puce.scantech.appContainer
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.usecase.GuardarActivoUseCase
import ec.edu.puce.scantech.domain.usecase.ObtenerActivoPorIdUseCase
import ec.edu.puce.scantech.domain.usecase.ResultadoGuardado
import ec.edu.puce.scantech.domain.validacion.DatosActivo
import ec.edu.puce.scantech.domain.validacion.ResultadoValidacion
import ec.edu.puce.scantech.domain.validacion.ValidadorActivo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * [RDA-1 Tema 4: Resiliencia del Estado con SavedStateHandle ante Process Death y Rotación]
 * [RDA-3 Temas 9 y 10: Arquitectura MVVM + Clean Architecture - Casos de Uso y UDF]
 * [RDA-3 Temas 11 y 12: Estrategia de Persistencia Offline-First con Room]
 *
 * ViewModel que orquesta el Formulario Ultra-Resiliente de Registro y Edición de Activos.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Resiliencia Total de Formulario (SavedStateHandle): Cada pulsación de tecla y campo ingresado
 *    se almacena de inmediato en `SavedStateHandle`. Si el sistema operativo destruye la Activity por rotación
 *    o liquida el proceso por falta de memoria RAM (*Process Death* con `am kill`), ningún carácter se pierde.
 * 2. Validación Estricta en Tiempo Real: En cada modificación, se invoca [ValidadorActivo], evaluando
 *    el formato regex del QR (`LTIC-000123`), nulabilidad, longitud máxima y conversión segura a entero
 *    mediante `toIntOrNull()` sobre las unidades físicas.
 * 3. Hilos de Fondo No Bloqueantes: Las operaciones de guardado y lectura transaccional se despachan
 *    en `Dispatchers.IO`, garantizando cero bloqueos del UI Thread y eliminando el riesgo de ANR (>5s).
 * 4. UDF (Slide 17 RDA-3 Tema 9): `_uiState` privado y `uiState` público de solo lectura vía `asStateFlow()`.
 */
class AssetFormViewModel(
    val assetId: String?,
    val initialQr: String? = null,
    private val guardarActivoUseCase: GuardarActivoUseCase,
    private val obtenerActivoPorIdUseCase: ObtenerActivoPorIdUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    companion object {
        /** [Caso 5] Valor seguro que asigna el operador Elvis si el texto no es un entero. */
        const val UNIDADES_POR_DEFECTO = 0

        /**
         * [Caso 5] Parseo blindado: `toIntOrNull()` devuelve `null` en lugar de lanzar
         * NumberFormatException ("abc", "", "5 días"), y Elvis (?:) lo reemplaza por 0.
         */
        fun procesarUnidades(texto: String): Int =
            texto.trim().toIntOrNull() ?: UNIDADES_POR_DEFECTO

        const val MENSAJE_CORREGIR_CAMPOS = "Por favor corrige los campos señalados antes de guardar."

        private const val KEY_QR = "form_qr_code"
        private const val KEY_NAME = "form_name"
        private const val KEY_CATEGORY = "form_category"
        private const val KEY_SPECS = "form_specs"
        private const val KEY_COLOR = "form_color"
        private const val KEY_DIMENSIONS = "form_dimensions"
        private const val KEY_BLOCK = "form_block"
        private const val KEY_SHELF = "form_shelf"
        private const val KEY_LOCKER = "form_locker"
        private const val KEY_UNITS = "form_units"
        private const val KEY_OBSERVATIONS = "form_observations"
        private const val KEY_INITIALIZED = "form_initialized_from_db"

        /**
         * Factory que arma los casos de uso con el repositorio compartido del
         * [ec.edu.puce.scantech.di.AppContainer]. El [SavedStateHandle] viene de
         * `createSavedStateHandle()`, así el borrador sobrevive a la muerte del proceso.
         */
        fun provideFactory(assetId: String?, initialQr: String? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = appContainer().assetRepository
                AssetFormViewModel(
                    assetId = assetId,
                    initialQr = initialQr,
                    guardarActivoUseCase = GuardarActivoUseCase(repository),
                    obtenerActivoPorIdUseCase = ObtenerActivoPorIdUseCase(repository),
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }

    val isEditMode: Boolean = assetId != null

    // Inicialización del estado desde SavedStateHandle
    private val _uiState = MutableStateFlow(
        AssetFormUiState(
            qrCode = savedStateHandle[KEY_QR] ?: initialQr ?: "",
            name = savedStateHandle[KEY_NAME] ?: "",
            category = savedStateHandle[KEY_CATEGORY] ?: "Laptops",
            technicalSpecs = savedStateHandle[KEY_SPECS] ?: "",
            color = savedStateHandle[KEY_COLOR] ?: "",
            dimensions = savedStateHandle[KEY_DIMENSIONS] ?: "",
            block = savedStateHandle[KEY_BLOCK] ?: "A",
            shelf = savedStateHandle[KEY_SHELF] ?: "1",
            locker = savedStateHandle[KEY_LOCKER] ?: "1",
            units = savedStateHandle[KEY_UNITS] ?: "1",
            observations = savedStateHandle[KEY_OBSERVATIONS] ?: "",
            isEditMode = isEditMode,
            isLoading = isEditMode && !(savedStateHandle.get<Boolean>(KEY_INITIALIZED) ?: false)
        )
    )
    val uiState: StateFlow<AssetFormUiState> = _uiState.asStateFlow()

    init {
        // Si se recibe un código QR desde el escáner y no existía en el SavedStateHandle, persistirlo
        if (savedStateHandle.get<String>(KEY_QR) == null && initialQr != null) {
            savedStateHandle[KEY_QR] = initialQr
        }
        if (isEditMode && !(savedStateHandle.get<Boolean>(KEY_INITIALIZED) ?: false)) {
            cargarActivoParaEdicion(assetId!!)
        } else {
            revalidar()
        }
    }

    /**
     * Carga los datos del activo desde la base de datos Room para poblar el formulario de edición.
     */
    private fun cargarActivoParaEdicion(id: String) {
        viewModelScope.launch(ioDispatcher) {
            val asset = obtenerActivoPorIdUseCase(id).firstOrNull()
            withContext(Dispatchers.Main) {
                if (asset != null) {
                    savedStateHandle[KEY_QR] = asset.qrCode
                    savedStateHandle[KEY_NAME] = asset.name
                    savedStateHandle[KEY_CATEGORY] = asset.category
                    savedStateHandle[KEY_SPECS] = asset.technicalSpecs
                    savedStateHandle[KEY_COLOR] = asset.color ?: ""
                    savedStateHandle[KEY_DIMENSIONS] = asset.dimensions ?: ""
                    savedStateHandle[KEY_BLOCK] = asset.block
                    savedStateHandle[KEY_SHELF] = asset.shelf
                    savedStateHandle[KEY_LOCKER] = asset.locker
                    savedStateHandle[KEY_UNITS] = asset.units.toString()
                    savedStateHandle[KEY_OBSERVATIONS] = asset.observations ?: ""
                    savedStateHandle[KEY_INITIALIZED] = true

                    _uiState.update { current ->
                        current.copy(
                            qrCode = asset.qrCode,
                            name = asset.name,
                            category = asset.category,
                            technicalSpecs = asset.technicalSpecs,
                            color = asset.color ?: "",
                            dimensions = asset.dimensions ?: "",
                            block = asset.block,
                            shelf = asset.shelf,
                            locker = asset.locker,
                            units = asset.units.toString(),
                            observations = asset.observations ?: "",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorGeneral = "No se encontró el activo a editar.") }
                }
                revalidar()
            }
        }
    }

    // Actualizaciones de campos con persistencia en SavedStateHandle y revalidación en tiempo real
    fun onQrChange(value: String) {
        val uppercaseVal = value.uppercase()
        savedStateHandle[KEY_QR] = uppercaseVal
        _uiState.update { it.copy(qrCode = uppercaseVal) }
        revalidar()
    }

    fun onNameChange(value: String) {
        savedStateHandle[KEY_NAME] = value
        _uiState.update { it.copy(name = value) }
        revalidar()
    }

    fun onCategoryChange(value: String) {
        savedStateHandle[KEY_CATEGORY] = value
        _uiState.update { it.copy(category = value) }
        revalidar()
    }

    fun onSpecsChange(value: String) {
        savedStateHandle[KEY_SPECS] = value
        _uiState.update { it.copy(technicalSpecs = value) }
        revalidar()
    }

    fun onColorChange(value: String) {
        savedStateHandle[KEY_COLOR] = value
        _uiState.update { it.copy(color = value) }
    }

    fun onDimensionsChange(value: String) {
        savedStateHandle[KEY_DIMENSIONS] = value
        _uiState.update { it.copy(dimensions = value) }
    }

    fun onBlockChange(value: String) {
        savedStateHandle[KEY_BLOCK] = value
        _uiState.update { it.copy(block = value) }
    }

    fun onShelfChange(value: String) {
        savedStateHandle[KEY_SHELF] = value
        _uiState.update { it.copy(shelf = value) }
    }

    fun onLockerChange(value: String) {
        savedStateHandle[KEY_LOCKER] = value
        _uiState.update { it.copy(locker = value) }
    }

    fun onUnitsChange(value: String) {
        savedStateHandle[KEY_UNITS] = value
        // Caso 5: el resultado mostrado es temporal; se limpia en cuanto el usuario vuelve a escribir
        _uiState.update { it.copy(units = value, unidadesProcesadas = null) }
        revalidar()
    }

    fun onObservationsChange(value: String) {
        savedStateHandle[KEY_OBSERVATIONS] = value
        _uiState.update { it.copy(observations = value) }
        revalidar()
    }

    /**
     * Valida los datos en tiempo real mediante [ValidadorActivo].
     */
    private fun revalidar(): Boolean {
        val current = _uiState.value
        val datos = DatosActivo(
            qrCode = current.qrCode,
            nombre = current.name,
            categoria = current.category,
            units = current.units,
            observaciones = current.observations
        )
        val resultado = ValidadorActivo.validar(datos, qrYaExiste = false)
        return when (resultado) {
            is ResultadoValidacion.Valido -> {
                _uiState.update { it.copy(errores = emptyMap(), isValid = current.qrCode.isNotBlank() && current.name.isNotBlank()) }
                true
            }
            is ResultadoValidacion.Invalido -> {
                _uiState.update { it.copy(errores = resultado.errores, isValid = false) }
                false
            }
        }
    }

    /**
     * Valida y procesa el guardado del activo en el repositorio.
     * Criterio crítico del enunciado: Si existen errores de validación, el botón Guardar NO persiste nada.
     */
    fun saveAsset() {
        // Un guardado en curso: ignorar toques repetidos para no escribir dos veces
        if (_uiState.value.isSaving) return

        // Caso 5: se procesa la entrada numérica antes de validar; con letras o vacío no hay crash
        val unidadesProcesadas = procesarUnidades(_uiState.value.units)
        _uiState.update { it.copy(unidadesProcesadas = unidadesProcesadas) }

        // Primer filtro sin tocar Room: si un campo es inválido no se llama al caso de uso
        if (!revalidar()) {
            _uiState.update { it.copy(errorGeneral = MENSAJE_CORREGIR_CAMPOS) }
            return
        }

        val current = _uiState.value
        val assetToSave = Asset(
            id = assetId ?: UUID.randomUUID().toString(),
            qrCode = current.qrCode.trim(),
            name = current.name.trim(),
            category = current.category.trim(),
            technicalSpecs = current.technicalSpecs.trim(),
            color = current.color.trim().ifBlank { null },
            dimensions = current.dimensions.trim().ifBlank { null },
            block = current.block.trim().ifBlank { "A" },
            shelf = current.shelf.trim().ifBlank { "1" },
            locker = current.locker.trim().ifBlank { "1" },
            units = unidadesProcesadas,
            observations = current.observations.trim().ifBlank { null }
        )

        // isSaving se marca antes de lanzar la corrutina para que un segundo toque ya lo vea
        _uiState.update { it.copy(isSaving = true, errorGeneral = null) }

        viewModelScope.launch {
            // El caso de uso valida otra vez, ahora con la unicidad del QR consultada en Room
            val result = withContext(ioDispatcher) {
                guardarActivoUseCase(asset = assetToSave, isEditMode = isEditMode)
            }
            when (result) {
                is ResultadoGuardado.Guardado -> {
                    _uiState.update { it.copy(isSaving = false, isSavedSuccess = true) }
                }
                is ResultadoGuardado.Invalido -> {
                    // Errores por campo (p. ej. QR duplicado) para que la UI los muestre junto al input
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errores = result.errores,
                            isValid = false,
                            errorGeneral = MENSAJE_CORREGIR_CAMPOS
                        )
                    }
                }
                is ResultadoGuardado.Fallo -> {
                    _uiState.update { it.copy(isSaving = false, errorGeneral = result.mensaje) }
                }
            }
        }
    }
}
