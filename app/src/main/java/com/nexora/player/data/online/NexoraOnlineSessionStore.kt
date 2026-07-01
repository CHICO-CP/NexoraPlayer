package com.nexora.player.data.online

import android.content.Context
import org.json.JSONObject

class NexoraOnlineSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nexora_online_session", Context.MODE_PRIVATE)

    fun load(): OnlineUserSession? {
        val raw = prefs.getString(KEY_SESSION, null).orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            OnlineUserSession(
                accessToken = json.optString("accessToken"),
                refreshToken = json.optString("refreshToken").takeIf { it.isNotBlank() },
                expiresAtEpochSeconds = json.optLong("expiresAtEpochSeconds", 0L),
                email = json.optString("email").takeIf { it.isNotBlank() },
                userId = json.optString("userId").takeIf { it.isNotBlank() }
            ).takeIf { it.accessToken.isNotBlank() }
        }.getOrNull()
    }

    fun save(session: OnlineUserSession) {
        val json = JSONObject()
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken.orEmpty())
            .put("expiresAtEpochSeconds", session.expiresAtEpochSeconds)
            .put("email", session.email.orEmpty())
            .put("userId", session.userId.orEmpty())
        prefs.edit().putString(KEY_SESSION, json.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    companion object {
        private const val KEY_SESSION = "session"
    }
}
