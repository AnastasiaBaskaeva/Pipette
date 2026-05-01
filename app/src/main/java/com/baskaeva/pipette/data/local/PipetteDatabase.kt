package com.baskaeva.pipette.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavouriteColorEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PipetteDatabase : RoomDatabase() {
    abstract fun favouriteColorDao(): FavouriteColorDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM favourites
            WHERE id NOT IN (
                SELECT MIN(id) FROM favourites GROUP BY hex
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_favourites_hex ON favourites(hex)"
        )
    }
}