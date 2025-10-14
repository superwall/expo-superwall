package com.superwall.sdk.storage.core_data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.superwall.sdk.storage.core_data.entities.ManagedEventData
import com.superwall.sdk.storage.core_data.entities.ManagedEventDataDao
import com.superwall.sdk.storage.core_data.entities.ManagedTriggerRuleOccurrence
import com.superwall.sdk.storage.core_data.entities.ManagedTriggerRuleOccurrenceDao

@TypeConverters(Converters::class)
@Database(
    entities = [ManagedEventData::class, ManagedTriggerRuleOccurrence::class],
    version = 1,
    exportSchema = false,
)
abstract class SuperwallDatabase : RoomDatabase() {
    abstract fun managedEventDataDao(): ManagedEventDataDao

    abstract fun managedTriggerRuleOccurrenceDao(): ManagedTriggerRuleOccurrenceDao

    companion object {
        @Volatile
        private var INSTANCE: SuperwallDatabase? = null

        fun getDatabase(context: Context): SuperwallDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            SuperwallDatabase::class.java,
                            "superwall_database",
                        ).build()
                INSTANCE = instance
                instance
            }
    }
}
