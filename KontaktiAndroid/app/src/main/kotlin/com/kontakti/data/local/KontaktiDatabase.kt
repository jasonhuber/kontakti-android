package com.kontakti.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PersonEntity::class,
        CompanyEntity::class,
        DiscussionEntity::class,
        PendingSyncEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KontaktiDatabase : RoomDatabase() {
    abstract fun dao(): KontaktiDao

    companion object {
        private const val DB_NAME = "kontakti.db"

        fun create(context: Context): KontaktiDatabase =
            Room.databaseBuilder(context, KontaktiDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
