package com.nexora.player

import android.app.Application
import com.nexora.player.data.local.NexoraDatabase

class NexoraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NexoraDatabase.get(this)
    }
}
