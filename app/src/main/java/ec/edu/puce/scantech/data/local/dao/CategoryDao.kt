package ec.edu.puce.scantech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ec.edu.puce.scantech.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /** Observa todas las categorías ordenadas por nombre para los FilterChip. */
    @Query("SELECT * FROM categories ORDER BY nombre ASC")
    fun observarCategorias(): Flow<List<CategoryEntity>>

    /** Inserta una categoría; si la clave ya existe, la operación se ignora. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(categoria: CategoryEntity): Long
}
