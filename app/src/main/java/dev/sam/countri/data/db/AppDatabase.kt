package dev.sam.countri.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CountryStateEntity::class, VisitEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.CitiesToTags::class),
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun countryStateDao(): CountryStateDao
    abstract fun visitDao(): VisitDao

    /** v2: cities became free-form place tags. */
    @RenameColumn(tableName = "country_state", fromColumnName = "cities", toColumnName = "tags")
    class CitiesToTags : AutoMigrationSpec

    companion object {
        /**
         * v4: wishlist becomes a flag beside status instead of a rival to it,
         * so a visited country can stay on the wishlist. Existing wishlist
         * rows carry their meaning into the new column.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE country_state ADD COLUMN wishlisted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE country_state SET wishlisted = 1 WHERE status = 'WISHLIST'")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "countri.db")
                .addMigrations(MIGRATION_3_4)
                .build()
    }
}
