---
name: defensa-examen
description: >-
  Skill especializada para resolver casos de defensa práctica de Programación Móvil (ScanTech LTIC - PUCE).
  Analiza el reto asignado por el docente, activa o adapta quirúrgicamente el código en la rama de trabajo,
  verifica la compilación y genera el documento oficial de entrega (Evidencia_CasoX_Flores_Paul.md) con
  capturas rotuladas, anexo de código y respuesta teórica de máximo 150 palabras.
---

# 🎓 Skill de Defensa Práctica de Examen — ScanTech LTIC (PUCE)

Esta skill se activa cuando el estudiante recibe su **Hoja de Reto Individual** en la defensa de Programación Móvil y necesita resolver el caso de código y generar el documento de evidencia para el EVA en menos de 15 minutos.

---

## 📋 Protocolo de Ejecución Rápida (4 Pasos)

### Paso 1: Análisis del Reto del Docente
Cuando el usuario proporcione el caso (por número, texto copiado del PDF o imagen transcrita):
1. Identificar el **Caso Asignado** y contrastarlo con el banco de 30 casos en `teoria/banco_30_casos_defensa_examen_fase1.md` y `teoria/guia_rapida_activacion_casos.md`.
2. Detectar **variaciones específicas** introducidas por el docente (nombres de variables, parámetros nuevos, restricciones cromáticas o de arquitectura).
3. Identificar la **Pregunta Teórica de Examen** y su límite estricto de palabras (**máximo 150 palabras**).

---

### Paso 2: Aplicación Quirúrgica en Código
1. **Localizar los archivos objetivo** (según la tabla de `teoria/guia_rapida_activacion_casos.md`).
2. **Activar el cambio:**
   - Si el caso ya tiene un bloque `/* =================== [DEFENSA CASO X] =================== */`, comentar el código original y descomentar el código del caso.
   - Ajustar nombres de parámetros o rutas si el docente pidió una variante personalizada.
   - Si requiere una rama separada (ej. si el profesor lo exige explícitamente), crear la rama `git checkout -b caso-<numero>` antes de editar. Si no lo exige, trabajar directamente sobre la rama actual.
3. **Limpieza:** Dejar el código limpio, legible y sin bloques residuales que confundan al docente.

---

### Paso 3: Verificación Rápida de Compilación
1. Ejecutar verificación de build en la terminal:
   ```bash
   cd "AdministracionLTIC"
   .\gradlew assembleDebug
   ```
2. Garantizar que compile con `BUILD SUCCESSFUL` antes de dar por terminado el cambio.

---

### Paso 4: Generación del Documento de Entrega EVA
Generar inmediatamente el archivo Markdown en `teoria/entrega/Evidencia_Caso<Numero>_Flores_Paul.md` estructurado de acuerdo al **Protocolo Oficial de Evaluación del Docente**:

```markdown
# PONTIFICIA UNIVERSIDAD CATÓLICA DEL ECUADOR
## FACULTAD DE INGENIERÍA · PROGRAMACIÓN MÓVIL
### EVALUACIÓN PRÁCTICA INDIVIDUAL — CASO <X>

**Estudiante:** Flores Ludeña Paúl Alejandro  
**Fecha:** [Fecha actual]  
**Proyecto:** ScanTech LTIC (Fase 1)  
**Documento:** Evidencia_Caso<X>_Flores_Paul.pdf  

---

## 1. 📱 Capturas de Pantalla de la Aplicación

> **Instrucción para el estudiante:** Abre el emulador en la pantalla [Nombre de Pantalla], realiza la acción [Acción] y toma la captura.

[Pegar aquí la Captura de Pantalla del Emulador]

**Figura 1:** [Rótulo formal obligatorio descriptivo de la acción y el parámetro renderizado].

---

## 2. 🛠️ Anexo de Código Modificado (Texto Plano Limpio)

### Archivo: `app/src/main/java/ec/edu/puce/scantech/.../[NombreArchivo].kt`
```kotlin
// Fragmento exacto del código refactorizado
```

---

## 3. 💡 Sección de Respuesta Teórica (Máximo 150 palabras)

**Pregunta de Examen:**
> "[Pregunta textual formulada en el reto]"

**Respuesta Técnica:**
[Respuesta rigurosa, conceptual, directa y concisa basada en las diapositivas de teoría RDA-1, RDA-2 o RDA-3, con conteo estricto de menos de 150 palabras].
```

---

## 🧠 Banco de Conceptos Clave para Respuestas Teóricas

- **Rutas Tipadas (@Serializable):** Seguridad de tipos en compilación (Type-Safety), elimina cadenas mágicas ("asset/{id}"), compatibilidad con KotlinX Serialization y deserialización directa con `toRoute<T>()`.
- **Material Design 3 (Tokens Semánticos):** `tertiaryContainer` / `onTertiaryContainer` garantizan contraste WCAG AAA automático y accesibilidad tanto en modo claro como oscuro y Dynamic Color. Prohíbe colores hexadecimales quemados (`Color(0x...)`).
- **UDF & Compose (UI = f(State)):** El estado fluye hacia abajo (StateFlow) y los eventos hacia arriba (callbacks). Compose recompone automáticamente los nodos que leen estados cambiados sin llamadas imperativas (`setEnabled()`).
- **ViewModel Lifecycle vs Activity:** `onCleared()` se llama únicamente cuando el ViewModel es destruido definitivamente (navegación atrás o finish()). No se destruye en rotaciones porque el ViewModelStore sobrevive a través de `NonConfigurationInstances`.
- **Null Safety Idiomático:** `toIntOrNull() ?: default` previene `NumberFormatException` y Fatal Crashes de la JVM en tiempo de ejecución, permitiendo manejar valores corruptos con valores de rescate limpios.
- **Room SSOT & Soft Delete:** Room es la única fuente de verdad. El borrado lógico (`deletedAt != null`) oculta el registro de la UI pero preserva la fila en SQLite para poder encolar y sincronizar la orden de baja cuando haya red.

