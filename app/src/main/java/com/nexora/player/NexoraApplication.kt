package com.nexora.player

import android.app.Application
import com.nexora.player.data.local.NexoraDatabase
import com.nexora.player.notifications.MediaLibraryNotifier

class NexoraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NexoraDatabase.get(this)
        MediaLibraryNotifier.ensureChannel(this)
    }
}
