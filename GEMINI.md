# 🛡️ Reglas y Comandos de Trabajo — ScanTech LTIC (PUCE)

## 🎯 Disparador Rápido de Examen: `@examen`

Cuando el usuario escriba un mensaje que comience o contenga `@examen` (por ejemplo: `@examen Caso 1`, `@examen Caso 14` o `@examen [enunciado del reto]`):

Se activa automáticamente el protocolo de la skill `defensa-examen`:

1. **Recepción y Análisis:**
   - Extraer el número del caso o los requerimientos del reto del docente.
   - Identificar archivos objetivo según `teoria/guia_rapida_activacion_casos.md`.

2. **Refactorización Limpia (Cero Basura):**
   - Activar el código correspondiente al caso.
   - **ELIMINAR COMPLETAMENTE** el código antiguo que ya no se usa y los bloques de comentarios de preparación (`/* === [DEFENSA CASO X] === */`), dejando el código limpio, puro y como si hubiese sido programado a mano directamente por el estudiante.
   - Cero comentarios de "DEFENSA CASO" o código muerto en los archivos afectados.

3. **Verificación de Compilación:**
   - Correr `cd AdministracionLTIC; .\gradlew assembleDebug` y verificar `BUILD SUCCESSFUL`.

4. **Generación del Entregable EVA:**
   - Crear el archivo Markdown en `teoria/entrega/Evidencia_Caso<X>_Flores_Paul.md` con:
     - Encabezado oficial: Flores Ludeña Paúl Alejandro
     - Instrucción de captura en emulador con rótulo formal obligatorio (`Figura 1: ...`).
     - Anexo de código limpio refactorizado.
     - Respuesta a la pregunta teórica (**máximo 150 palabras**).

5. **Respuesta en Chat:**
   - Confirmar archivos modificados.
   - Entregar la respuesta teórica resumida para que el estudiante la defienda oralmente.
   - Enlace al archivo de evidencia generado listo para exportar a PDF.

