package dev.sam.countri.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryStateDao {
    @Query("SELECT * FROM country_state")
    fun observeAll(): Flow<List<CountryStateEntity>>

    @Upsert
    suspend fun upsert(state: CountryStateEntity)

    @Query("SELECT * FROM country_state")
    suspend fun allOnce(): List<CountryStateEntity>

    @Query("DELETE FROM country_state WHERE iso2 = :iso2")
    suspend fun delete(iso2: String)

    @Query("DELETE FROM country_state")
    suspend fun deleteAll()
}
