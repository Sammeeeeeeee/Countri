package dev.sam.countri.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visit ORDER BY startDay")
    fun observeAll(): Flow<List<VisitEntity>>

    @Insert
    suspend fun insert(visit: VisitEntity): Long

    @Update
    suspend fun update(visit: VisitEntity)

    @Query("DELETE FROM visit WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM visit WHERE iso2 = :iso2")
    suspend fun deleteAllFor(iso2: String)
}
