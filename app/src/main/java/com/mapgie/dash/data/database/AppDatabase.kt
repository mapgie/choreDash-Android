package com.mapgie.dash.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mapgie.dash.data.database.dao.CustomColorThemeDao
import com.mapgie.dash.data.database.entities.CustomColorTheme

@Database(
    entities = [CustomColorTheme::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customColorThemeDao(): CustomColorThemeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Version 2: adds saturation and lightness columns for all three colour roles
         * so users can control full HSL rather than hue alone.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE custom_color_themes ADD COLUMN primarySaturation REAL NOT NULL DEFAULT 0.5"
                )
                database.execSQL(
                    "ALTER TABLE custom_color_themes ADD COLUMN primaryLightness REAL NOT NULL DEFAULT 0.4"
                )
                database.execSQL(
                    "ALTER TABLE custom_color_themes ADD COLUMN secondarySaturation REAL NOT NULL DEFAULT 0.4"
                )
                database.execSQL(
                    "ALTER TABLE custom_color_themes ADD COLUMN secondaryLightness REAL NOT NULL DEFAULT 0.4"
                )
                database.execSQL(
                    "ALTER TABLE custom_color_themes ADD COLUMN tertiarySaturation REAL NOT NULL DEFAULT 0.4"
                )
                database.execSQL(
                    "ALTER TABLE custom_color_themes ADD COLUMN tertiaryLightness REAL NOT NULL DEFAULT 0.4"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dash.db",
                )
                    // No fallbackToDestructiveMigration — add explicit migrations for future versions.
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
