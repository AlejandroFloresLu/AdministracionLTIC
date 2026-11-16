package ec.edu.puce.scantech.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ec.edu.puce.scantech.data.local.dao.AssetDao
import ec.edu.puce.scantech.data.local.dao.CategoryDao
import ec.edu.puce.scantech.data.local.dao.SyncQueueDao
import ec.edu.puce.scantech.data.local.entity.AssetEntity
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
        AssetEntity::class,
        CategoryEntity::class,
        SyncOperationEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(SyncStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun assetDao(): AssetDao

    abstract fun categoryDao(): CategoryDao

    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val NOMBRE_ARCHIVO = "scantech_ltic.db"

        /** v1 -> v2: agrega la tabla `assets` con sus índices; no toca los datos existentes. */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `assets` (" +
                        "`id` TEXT NOT NULL, `qrCode` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `technicalSpecs` TEXT NOT NULL, " +
                        "`color` TEXT, `dimensions` TEXT, `block` TEXT NOT NULL, " +
                        "`shelf` TEXT NOT NULL, `locker` TEXT NOT NULL, `units` INTEGER NOT NULL, " +
                        "`observations` TEXT, `syncStatus` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assets_qrCode` ON `assets` (`qrCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_category` ON `assets` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_name` ON `assets` (`name`)")
            }
        }

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
                    .addMigrations(MIGRACION_1_2)
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
