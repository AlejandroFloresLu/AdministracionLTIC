package ec.edu.puce.scantech.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import ec.edu.puce.scantech.R

/**
 * [RDA-2 Temas 7 y 8: Estética y Flujo en Aplicaciones Modernas - Material Design 3 y Tipografía]
 *
 * Configuración centralizada de fuentes en Type.kt mediante Google Fonts descargadas de forma segura bajo demanda.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Descarga Dinámica Segura (GoogleFont.Provider): En lugar de incrustar archivos TTF/OTF binarios
 *    pesados en el APK que incrementan el tamaño de descarga, las fuentes se solicitan a través del
 *    proveedor del sistema (Google Play Services) validando los certificados criptográficos (`font_certs.xml`).
 * 2. Jerarquía Tipográfica de Material 3:
 *    - Familia Sans-Serif moderna (Inter): Utilizada para títulos, subtítulos, campos de entrada y cuerpo de texto
 *      garantizando legibilidad algorítmica y accesibilidad con una relación de contraste mínima de 4.5:1.
 *    - Familia Monoespaciada (Roboto Mono): Especializada para datos técnicos de hardware LTIC
 *      (números de serie, identificadores UUID, códigos QR y especificaciones), donde el ancho uniforme
 *      de los glifos facilita la lectura e inspección visual en bodegas.
 */

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Familia Inter: Diseñada para pantallas digitales y UI densa
private val fontInter = GoogleFont("Inter")
val InterFontFamily = FontFamily(
    Font(googleFont = fontInter, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontInter, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = fontInter, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = fontInter, fontProvider = provider, weight = FontWeight.Bold)
)

// Familia Roboto Mono: Destinada a seriales, códigos QR y hardware specs
private val fontRobotoMono = GoogleFont("Roboto Mono")
val RobotoMonoFontFamily = FontFamily(
    Font(googleFont = fontRobotoMono, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontRobotoMono, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = fontRobotoMono, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = fontRobotoMono, fontProvider = provider, weight = FontWeight.Bold)
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Monospace para badges de estado, seriales y referencias a códigos de activos
    labelSmall = TextStyle(
        fontFamily = RobotoMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
