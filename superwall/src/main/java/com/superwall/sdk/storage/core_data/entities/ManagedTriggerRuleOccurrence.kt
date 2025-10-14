package com.superwall.sdk.storage.core_data.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import java.util.Date

@Entity(tableName = "ManagedTriggerRuleOccurrence")
data class ManagedTriggerRuleOccurrence(
    @PrimaryKey(autoGenerate = true) var id: Int? = null,
    var createdAt: Date = Date(),
    var occurrenceKey: String,
)

@Dao
interface ManagedTriggerRuleOccurrenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventData: ManagedTriggerRuleOccurrence)

    @Transaction
    @Query("SELECT * FROM ManagedTriggerRuleOccurrence WHERE occurrenceKey = :key")
    suspend fun getManagedTriggerRuleOccurrencesByKey(key: String): List<ManagedTriggerRuleOccurrence>

    @Transaction
    @Query("SELECT * FROM ManagedTriggerRuleOccurrence WHERE createdAt >= :date AND occurrenceKey = :key")
    suspend fun getManagedTriggerRuleOccurrencesSinceDate(
        date: Date,
        key: String,
    ): List<ManagedTriggerRuleOccurrence>

    @Transaction
    @Query("DELETE FROM ManagedTriggerRuleOccurrence")
    suspend fun deleteAll()
}
