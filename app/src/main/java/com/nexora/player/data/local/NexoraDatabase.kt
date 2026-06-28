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
        PlaybackHistoryEntity::class,
        OnlineSavedTrackEntity::class,
        LyricsEntity::class,
        PlaybackStatsEntity::class,
        RemoteNoticeEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun playlistsDao(): PlaylistsDao
    abstract fun historyDao(): HistoryDao
    abstract fun onlineSavedTracksDao(): OnlineSavedTracksDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun playbackStatsDao(): PlaybackStatsDao
    abstract fun remoteNoticesDao(): RemoteNoticesDao

    companion object {
        @Volatile private var INSTANCE: NexoraDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lyrics` (
                        `mediaId` INTEGER NOT NULL,
                        `mediaUriString` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        `offsetMs` INTEGER NOT NULL,
                        `rawText` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `online_saved_tracks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `providerId` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `artworkUrl` TEXT,
                        `streamUrl` TEXT NOT NULL,
                        `downloadUrl` TEXT,
                        `durationMs` INTEGER NOT NULL,
                        `sourcePageUrl` TEXT,
                        `savedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_online_saved_tracks_providerId_sourceId` ON `online_saved_tracks` (`providerId`, `sourceId`)"
                )
            }
        }


        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playback_stats` (
                        `mediaKey` TEXT NOT NULL,
                        `mediaId` INTEGER NOT NULL,
                        `mediaKind` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `uriString` TEXT NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `playCount` INTEGER NOT NULL,
                        `lastPlayedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaKey`)
                    )
                    """.trimIndent()
                )
            }
        }


        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `remote_notices` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `readAt` INTEGER NOT NULL,
                        `versionCode` INTEGER NOT NULL,
                        `actionUrl` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): NexoraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NexoraDatabase::class.java,
                    "nexora.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
