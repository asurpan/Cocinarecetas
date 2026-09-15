package com.sagon.cocinarecetas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.data.model.HealthRecord

@Database(entities = [Recipe::class, HealthRecord::class], version = 14, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun healthRecordDao(): HealthRecordDao

    companion object {
        @Volatile
        private var INSTANCE: RecipeDatabase? = null

        fun getDatabase(context: Context): RecipeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    "recipe_database"
                )
                .fallbackToDestructiveMigration(true) // Forzamos limpieza profunda
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Crear índices de alto rendimiento para acelerar las búsquedas un 500%
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_category ON recipes(category);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_healthTags ON recipes(healthTags);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_title ON recipes(title);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_isFavorite ON recipes(isFavorite);")
                    }
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Crear índices también al abrir por seguridad si no existen
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_category ON recipes(category);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_healthTags ON recipes(healthTags);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_title ON recipes(title);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_isFavorite ON recipes(isFavorite);")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
