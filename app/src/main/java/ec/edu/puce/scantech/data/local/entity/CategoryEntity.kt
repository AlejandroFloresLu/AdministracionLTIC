package ec.edu.puce.scantech.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.Normalizer

/**
 * Categoría controlada de activos (tabla `categories`).
 *
 * La clave primaria es una versión normalizada del nombre (minúsculas, sin tildes ni espacios
 * sobrantes), de modo que "Mouse", "mouse" y "Ratón" / "Raton" no generen duplicados.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val clave: String,
    val nombre: String
) {
    companion object {
        /** Categorías de periféricos cargadas al crear la base de datos. */
        val INICIALES: List<String> = listOf(
            "Mouse",
            "Teclado",
            "Monitor",
            "Audífonos",
            "Webcam",
            "Impresora",
            "Escáner",
            "Parlantes",
            "Hub USB"
        )

        private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")

        /** Convierte un nombre a su clave normalizada: "Ratón " -> "raton". */
        fun claveDe(nombre: String): String {
            val sinTildes = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)
                .replace(MARCAS_DIACRITICAS, "")
            return sinTildes.lowercase()
        }
    }
}
