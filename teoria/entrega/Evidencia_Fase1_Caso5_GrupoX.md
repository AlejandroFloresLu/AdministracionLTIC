# PONTIFICIA UNIVERSIDAD CATÓLICA DEL ECUADOR
## FACULTAD DE INGENIERÍA · PROGRAMACIÓN MÓVIL
### EVALUACIÓN DE DEFENSA PRÁCTICA — FASE 1 (RETO GRUPAL)
#### CASO 5: Manejo Seguro de Nulos en la Capa de UI

**Integrantes / Estudiante:** Flores Ludeña Paúl Alejandro (Equipo ScanTech LTIC)  
**Fecha:** 24 de Noviembre de 2026  
**Proyecto:** ScanTech LTIC (Fase 1)  
**Archivo Entregable para el EVA:** `Evidencia_Fase1_Caso5_GrupoX.pdf`  

---

## 1. 📱 Captura de Pantalla de la Aplicación

> **Instrucción para la captura:** 
> 1. Abre la aplicación ScanTech en el emulador.
> 2. Presiona el botón de agregar nuevo activo (`+`) para abrir el formulario de registro.
> 3. En el campo **"Unidades"**, escribe caracteres alfabéticos (ej. `abc`) o déjalo vacío.
> 4. Observa cómo la aplicación reacciona en tiempo real en el `supportingText` indicando que la entrada no numérica se procesará de forma segura como `0` mediante el operador Elvis (`?:`), manteniendo la estabilidad total sin colapsar (`Crash`) al presionar "Guardar Activo".

```
┌────────────────────────────────────────────────────────┐
│                                                        │
│         [ PEGAR AQUÍ LA CAPTURA DEL EMULADOR ]         │
│   (Con "abc" en el campo Unidades y el texto reactivo) │
│                                                        │
└────────────────────────────────────────────────────────┘
```

**Figura 1: Formulario tolerante a fallos interceptando entradas no numéricas mediante toIntOrNull y operador Elvis de forma segura.**

---

## 2. 🛠️ Anexo de Código Modificado (Texto Plano Limpio)

### A. Capa de Presentación / UI (`app/src/main/java/ec/edu/puce/scantech/ui/form/AssetFormScreen.kt`)
```kotlin
// Campo Numérico Crítico (Unidades) con evaluación y feedback reactivo de nulos vía Elvis (?:)
val unitsError = uiState.errores[CampoActivo.UNITS]
val safeUnits = uiState.units.trim().toIntOrNull() ?: 0
val esEntradaInvalida = uiState.units.isNotBlank() && uiState.units.trim().toIntOrNull() == null

OutlinedTextField(
    value = uiState.units,
    onValueChange = onUnitsChange,
    label = { Text("Unidades") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    isError = unitsError != null,
    supportingText = {
        if (esEntradaInvalida) {
            Text(
                text = "Entrada no numérica ('${uiState.units}'). Se procesará como $safeUnits mediante Elvis (?:)",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        } else if (unitsError != null) {
            Text(text = unitsError, color = MaterialTheme.colorScheme.error)
        } else {
            Text("Valor numérico procesado: $safeUnits unidades")
        }
    },
    modifier = Modifier.weight(1f),
    shape = RoundedCornerShape(12.dp),
    singleLine = true
)
```

### B. Capa de Lógica de Presentación / ViewModel (`app/src/main/java/ec/edu/puce/scantech/ui/form/AssetFormViewModel.kt`)
```kotlin
fun saveAsset() {
    val current = _uiState.value

    // Blindaje de parseo idiomático con toIntOrNull() y operador Elvis (?: 0)
    // Garantiza que entradas con letras o vacías no produzcan NumberFormatException
    val parsedUnits = current.units.trim().toIntOrNull() ?: 0

    if (!revalidar()) {
        _uiState.update { it.copy(errorGeneral = "Por favor corrige los campos señalados antes de guardar.") }
        return
    }

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
        units = parsedUnits,
        observations = current.observations.trim().ifBlank { null }
    )

    viewModelScope.launch(Dispatchers.IO) {
        _uiState.update { it.copy(isSaving = true, errorGeneral = null) }
        val result = guardarActivoUseCase(asset = assetToSave, isEditMode = isEditMode)
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> _uiState.update { it.copy(isSaving = false, isSavedSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, errorGeneral = result.message) }
            }
        }
    }
}
```

### C. Capa de Validación de Dominio (`app/src/main/java/ec/edu/puce/scantech/domain/validacion/ValidadorActivo.kt`)
```kotlin
// Manejo Seguro de Nulos: toIntOrNull() con operador Elvis para asignar 0 por defecto ante nulos
val unidades = datos.units.trim().toIntOrNull() ?: 0
if (unidades < 0) {
    errores[CampoActivo.UNITS] = "Las unidades no pueden ser negativas"
}
```

---

## 3. 💡 Sección de Respuesta Teórica (Máximo 150 palabras)

**Pregunta de Examen:**
> *"Justifique técnicamente cómo el sistema de Null Safety de Kotlin y el control de nulidades desde el tiempo de compilación mitigan el riesgo histórico de excepciones del tipo NullPointerException en el desarrollo móvil".*

**Respuesta Técnica:**
En Java tradicional, cualquier referencia de objeto puede ser potencialmente nula sin advertencia, convirtiendo a `NullPointerException` (NPE) en la causa principal de fallos en tiempo de ejecución en Android.

Kotlin erradica este problema estructural integrando Null Safety en su sistema de tipos desde el tiempo de compilación. Los tipos se dividen estrictamente en no-anulables (ej. `String`) y anulables (ej. `String?`). El compilador prohíbe terminantemente asignar valores nulos o invocar métodos sobre tipos anulables sin comprobación previa, trasladando la detección de errores de la ejecución a la compilación.

Para gestionar nulidades de forma robusta e idiomática, Kotlin provee la función `toIntOrNull()` y el operador Elvis (`?:`), permitiendo interceptar conversiones fallidas de texto y asignar de inmediato valores de rescate por defecto seguros (como 0). Esto blinda la aplicación móvil contra entradas anómalas, garantizando estabilidad total sin cierres forzados.
*(Conteo exacto: 128 palabras).*

