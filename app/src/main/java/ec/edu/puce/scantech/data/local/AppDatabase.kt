package ec.edu.puce.scantech.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import ec.edu.puce.scantech.data.local.dao.CategoryDao
import ec.edu.puce.scantech.data.local.dao.SyncQueueDao
import ec.edu.puce.scantech.data.local.entity.CategoryEntity
import ec.edu.puce.scantech.data.local.entity.SyncOperationEntity

/**
 * Base de datos local de ScanTech LTIC (Single Source of Truth para Offline-First).
 *
 * El esquema está versionado y se exporta a la carpeta `schemas/` del repo para poder
 * escribir migraciones desde la versión 1.
 */
@Database(
    entities = [
        CategoryEntity::class,
        SyncOperationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(SyncStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val NOMBRE_ARCHIVO = "scantech_ltic.db"

        @Volatile
        private var instancia: AppDatabase? = null

        /** Devuelve la instancia única de la base de datos en disco. */
        fun obtener(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE_ARCHIVO
                )
                    .addCallback(SembradoInicial)
                    .build()
                    .also { instancia = it }
            }
        }

        /** Base en memoria para pruebas de DAOs; no toca el almacenamiento del dispositivo. */
        fun enMemoria(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .addCallback(SembradoInicial)
                .build()
        }
    }

    /** Carga las categorías de periféricos la primera vez que se crea la base. */
    private object SembradoInicial : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CategoryEntity.INICIALES.forEach { nombre ->
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (clave, nombre) VALUES (?, ?)",
                    arrayOf(CategoryEntity.claveDe(nombre), nombre)
                )
            }
        }
    }
}
