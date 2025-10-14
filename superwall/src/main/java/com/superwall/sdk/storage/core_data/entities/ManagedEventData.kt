package com.superwall.sdk.storage.core_data.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.util.Date

@Entity
data class ManagedEventData(
    @PrimaryKey val id: String,
    val createdAt: Date,
    val name: String,
    val parameters: Map<String, Any>,
)

@Dao
interface ManagedEventDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventData: ManagedEventData)

    @Query(
        """
        SELECT * FROM ManagedEventData 
        WHERE name = :name 
        AND (:date IS NULL OR createdAt < :date) 
        ORDER BY createdAt DESC 
        LIMIT 1
    """,
    )
    suspend fun getLastSavedEvent(
        name: String,
        date: Date?,
    ): ManagedEventData?

    @Query("DELETE FROM ManagedEventData")
    suspend fun deleteAll()

    @Query(
        """
        SELECT COUNT(*) FROM ManagedEventData 
        WHERE name = :name 
        AND createdAt BETWEEN :startDate AND :endDate
        """,
    )
    suspend fun countEventsByNameInPeriod(
        name: String,
        startDate: Date,
        endDate: Date,
    ): Int
}
