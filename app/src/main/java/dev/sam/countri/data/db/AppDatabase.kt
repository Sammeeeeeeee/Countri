package dev.sam.countri.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [CountryStateEntity::class, VisitEntity::class],
    version = 3,
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
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "countri.db").build()
    }
}
