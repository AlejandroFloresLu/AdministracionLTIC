package ec.edu.puce.scantech.data.local

import androidx.test.platform.app.InstrumentationRegistry
import ec.edu.puce.scantech.data.local.dao.CategoryDao
import ec.edu.puce.scantech.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun crearBaseEnMemoria() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.enMemoria(contexto)
        dao = db.categoryDao()
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    @Test
    fun alCrearLaBaseSeCargaronLasCategoriasDePerifericos() = runBlocking {
        val categorias = dao.observarCategorias().first()

        assertEquals(CategoryEntity.INICIALES.size, categorias.size)
        assertTrue(categorias.any { it.nombre == "Mouse" })
    }

    @Test
    fun variantesDeMayusculasYTildesNoDuplicanCategorias() = runBlocking {
        val antes = dao.observarCategorias().first().size

        dao.insertar(CategoryEntity(CategoryEntity.claveDe("mouse"), "mouse"))
        dao.insertar(CategoryEntity(CategoryEntity.claveDe("Ratón"), "Ratón"))
        dao.insertar(CategoryEntity(CategoryEntity.claveDe("Raton"), "Raton"))

        // "Mouse" ya existe; "Ratón" y "Raton" comparten clave, así que solo se agrega una.
        val despues = dao.observarCategorias().first()
        assertEquals(antes + 1, despues.size)
    }

    @Test
    fun claveDeNormalizaTildesMayusculasYEspacios() {
        assertEquals("raton", CategoryEntity.claveDe("  Ratón "))
        assertEquals("audifonos", CategoryEntity.claveDe("Audífonos"))
    }
}
