---
name: defensa-examen
description: >-
  Se activa con el comando '@examen' (ej. '@examen Caso 14' o '@examen [texto del reto]').
  Resuelve de forma inmediata el caso asignado por el docente, elimina código comentado y basura,
  compila el proyecto con Gradle y genera el informe Evidencia_CasoX_Flores_Paul.md para el EVA.
---

# 🎓 Skill de Defensa Práctica de Examen — ScanTech LTIC (PUCE)

Esta skill se dispara cuando el usuario envía un mensaje que contiene el comando:
```
@examen Caso <X> [detalles o texto del reto]
```

---

## ⚡ Protocolo de Ejecución de Alta Velocidad

### 1. Detección y Planificación Inmediata
- Extraer el número del caso o el texto del reto pegado por el estudiante.
- Ubicar los archivos y funciones correspondientes usando `teoria/guia_rapida_activacion_casos.md` y `teoria/banco_30_casos_defensa_examen_fase1.md`.
- Si el docente modificó nombres de variables, rutas o condiciones respecto a los 30 casos estándar, adaptar el código a la variación exacta del profesor.

### 2. Refactorización Quirúrgica Sin Basura (Regla Estricta)
- **Activar** la implementación requerida para el caso.
- **BORRAR DEFINITIVAMENTE:**
  - El código anterior obsoleto (no dejarlo comentado).
  - El bloque de envoltura `/* =================== [DEFENSA CASO X] =================== */`.
  - Instrucciones de activación o textos de guía rápida dentro del archivo `.kt`.
- **Resultado en el código:** El archivo debe quedar con la indentación perfecta, código de producción impecable y cero rastros de comentarios guía o código muerto, como si el estudiante lo hubiese redactado directamente frente al docente.

### 3. Verificación de Compilación Automática
- Ejecutar:
  ```powershell
  cd "c:\Users\ASUS\Documents\Proyectos Personales\Proyecto Programación Móvil Final\AdministracionLTIC"
  .\gradlew assembleDebug
  ```
- Confirmar `BUILD SUCCESSFUL`.

### 4. Generación del Informe Oficial EVA
Crear el archivo `teoria/entrega/Evidencia_Caso<Numero>_Flores_Paul.md`:
```markdown
# PONTIFICIA UNIVERSIDAD CATÓLICA DEL ECUADOR
## FACULTAD DE INGENIERÍA · PROGRAMACIÓN MÓVIL
### EVALUACIÓN PRÁCTICA INDIVIDUAL — CASO <X>

**Estudiante:** Flores Ludeña Paúl Alejandro  
**Fecha:** [Fecha actual]  
**Proyecto:** ScanTech LTIC (Fase 1)  
**Documento para EVA:** Evidencia_Caso<X>_Flores_Paul.pdf  

---

## 1. 📱 Capturas de Pantalla de la Aplicación

> **Instrucción para la captura:** Abre la aplicación en el emulador en la pantalla [Pantalla], realiza [Acción] y toma la captura.

[Insertar captura aquí]

**Figura 1:** [Rótulo formal obligatorio describiendo la pantalla y el parámetro renderizado].

---

## 2. 🛠️ Anexo de Código Modificado (Texto Plano Limpio)

### Archivo: `app/src/main/java/ec/edu/puce/scantech/.../[NombreArchivo].kt`
```kotlin
// Código final limpio
```

---

## 3. 💡 Sección de Respuesta Teórica (Máximo 150 palabras)

**Pregunta de Examen:**
> "[Pregunta exacta del docente]"

**Respuesta Técnica:**
[Respuesta formal de alto nivel técnico fundamentada en los RDA 1, 2 o 3, cumpliendo estrictamente con un máximo de 150 palabras].
```

### 5. Respuesta Inmediata en el Chat
1. Lista de archivos modificados y confirmación de build exitoso.
2. La respuesta teórica completa lista para responder oralmente si el profesor pregunta en vivo.
3. Enlace directo al archivo `Evidencia_Caso<Numero>_Flores_Paul.md` para convertirlo a PDF y subirlo al EVA.
