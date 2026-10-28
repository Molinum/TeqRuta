package com.teqmed.teqruta.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teqmed.teqruta.data.local.dao.DestinoDao
import com.teqmed.teqruta.data.local.dao.JornadaDao
import com.teqmed.teqruta.data.local.dao.TarifaDao
import com.teqmed.teqruta.data.local.dao.TramoDao
import com.teqmed.teqruta.data.local.dao.TrasladoDao
import com.teqmed.teqruta.data.local.dao.UsuarioDao
import com.teqmed.teqruta.data.local.dao.VehiculoDao
import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.Traslado
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.local.entity.Vehiculo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Usuario::class,
        Vehiculo::class,
        Destino::class,
        Tramo::class,
        TarifaCombustible::class,
        Jornada::class,
        Traslado::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun vehiculoDao(): VehiculoDao
    abstract fun destinoDao(): DestinoDao
    abstract fun tramoDao(): TramoDao
    abstract fun tarifaDao(): TarifaDao
    abstract fun jornadaDao(): JornadaDao
    abstract fun trasladoDao(): TrasladoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teqruta.db"
                )
                    // Precarga los datos de ejemplo la primera vez que se crea la BD
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                SeedData.poblar(obtener(context))
                            }
                        }
                    })
                    // Durante el desarrollo: si cambia el esquema se recrea la BD
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
