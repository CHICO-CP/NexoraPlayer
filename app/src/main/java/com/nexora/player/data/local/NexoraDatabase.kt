package com.nexora.player.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteMediaEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PlaybackHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun playlistsDao(): PlaylistsDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: NexoraDatabase? = null

        fun get(context: Context): NexoraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NexoraDatabase::class.java,
                    "nexora.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
