package ec.edu.puce.scantech.data.qr

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Genera códigos QR para las etiquetas de activos (#40).
 *
 * El QR contiene solo un identificador corto con formato `LTIC-000123` y usa corrección de
 * errores nivel H, para que siga leyéndose aunque la etiqueta se dañe.
 */
object GeneradorQr {

    private const val PREFIJO = "LTIC"
    private val FORMATO = Regex("^$PREFIJO-\\d{6}$")
    private const val COLOR_NEGRO = 0xFF000000.toInt()
    private const val COLOR_BLANCO = 0xFFFFFFFF.toInt()

    /** Construye el identificador a partir de un número secuencial: 123 -> `LTIC-000123`. */
    fun formatearCodigo(numero: Int): String {
        require(numero in 0..999_999) { "El número debe estar entre 0 y 999999" }
        return "%s-%06d".format(PREFIJO, numero)
    }

    /** Indica si un texto tiene el formato de identificador de activo. */
    fun esCodigoValido(codigo: String): Boolean = FORMATO.matches(codigo)

    /**
     * Genera el [Bitmap] del QR para [codigo]. Lanza [IllegalArgumentException] si el código
     * no tiene el formato esperado.
     */
    fun generarBitmap(codigo: String, tamanoPx: Int = 512): Bitmap {
        require(esCodigoValido(codigo)) { "Código inválido: $codigo" }
        require(tamanoPx > 0) { "El tamaño debe ser positivo" }

        val pistas = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val matriz = QRCodeWriter().encode(
            codigo,
            BarcodeFormat.QR_CODE,
            tamanoPx,
            tamanoPx,
            pistas
        )

        val pixeles = IntArray(tamanoPx * tamanoPx) { indice ->
            val x = indice % tamanoPx
            val y = indice / tamanoPx
            if (matriz.get(x, y)) COLOR_NEGRO else COLOR_BLANCO
        }

        return Bitmap.createBitmap(pixeles, tamanoPx, tamanoPx, Bitmap.Config.ARGB_8888)
    }
}
