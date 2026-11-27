package ec.edu.puce.scantech.ui.detail

import ec.edu.puce.scantech.domain.model.Asset

/**
 * [RDA-1 Tema 5: UI Declarativa - Máquinas de Estado Finitas (FSM)]
 * [RDA-3 Temas 9 y 10: Clean Architecture y Patrón UiState con Jerarquías Selladas]
 *
 * Jerarquía sellada exhaustiva que modela el estado de la Ficha Técnica de un activo en LTIC.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Jerarquía Cerrada (Sealed Interface): Impide estados inconsistentes y obliga al composable
 *    a evaluar todas las ramas de forma exhaustiva mediante `when(state)` sin requerir cláusula `else`.
 * 2. Blindaje contra Nulos: Elimina el operador inseguro `!!` y previene cierres forzados por nulos.
 */
sealed interface AssetDetailUiState {

    /**
     * Estado inicial mientras se recuperan los datos desde Room / SQLite.
     */
    data object Loading : AssetDetailUiState

    /**
     * Estado exitoso con el activo inmutable listo para inspección y edición.
     *
     * [observacionesDraft] es el texto que el usuario está editando (sobrevive a rotación y a
     * muerte del proceso vía SavedStateHandle); [isSavingObservaciones] indica un guardado en curso.
     */
    data class Success(
        val asset: Asset,
        val isDeleting: Boolean = false,
        val observacionesDraft: String = asset.observations.orEmpty(),
        val isSavingObservaciones: Boolean = false
    ) : AssetDetailUiState {
        /** El borrador difiere de lo guardado en Room (se compara sin espacios en los extremos). */
        val hayCambiosObservaciones: Boolean
            get() = observacionesDraft.trim() != asset.observations.orEmpty().trim()
    }

    /**
     * Estado de error si el activo no existe o si falló la lectura local.
     */
    data class Error(
        val mensaje: String
    ) : AssetDetailUiState
}

/** Eventos de un solo uso del guardado de observaciones (se muestran como Snackbar). */
sealed interface ObservacionesEvento {
    data object Guardadas : ObservacionesEvento
    data class Error(val mensaje: String) : ObservacionesEvento
}

/** Eventos de un solo uso de la baja lógica. */
sealed interface BajaEvento {
    /** El activo quedó PENDING_DELETE, oculto y con la baja encolada: volver atrás. */
    data object Completada : BajaEvento

    /** No se pudo dar de baja; la ficha sigue visible y se muestra el motivo. */
    data class Error(val mensaje: String) : BajaEvento
}

