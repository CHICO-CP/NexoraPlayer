package com.nexora.player.data.online

import android.content.Context
import android.provider.Settings
import com.nexora.player.BuildConfig
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class NexoraOnlineApiClient(private val context: Context) {
    val apiBaseUrl: String = BuildConfig.NEXORA_ONLINE_API_BASE_URL.trimEnd('/').ifBlank {
        "https://nexoraplayerapi.vercel.app"
    }
    private val apiPrefix: String = if (apiBaseUrl.endsWith("/api/v1")) apiBaseUrl else "$apiBaseUrl/api/v1"
    private val supabaseUrl: String = BuildConfig.NEXORA_SUPABASE_URL.trimEnd('/')
    private val supabaseAnonKey: String = BuildConfig.NEXORA_SUPABASE_ANON_KEY
    private val appClientId: String = BuildConfig.NEXORA_API_APP_ID.ifBlank { "music-mobile-app" }
    private val appSecret: String = BuildConfig.NEXORA_API_APP_SHARED_SECRET
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty().ifBlank { "anonymous-device" }
    }

    suspend fun login(email: String, password: String): OnlineUserSession = withContext(Dispatchers.IO) {
        ensureSupabaseConfigured()
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .toString()
            .toByteArray()
        val json = requestJson(
            url = "$supabaseUrl/auth/v1/token?grant_type=password",
            method = "POST",
            body = body,
            extraHeaders = supabaseHeaders(json = true),
            requiresAuthEnvelope = false
        )
        json.toSession()
    }

    suspend fun register(email: String, password: String, username: String): OnlineUserSession = withContext(Dispatchers.IO) {
        ensureSupabaseConfigured()
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("data", JSONObject().put("username", username.trim()))
            .toString()
            .toByteArray()
        val json = requestJson(
            url = "$supabaseUrl/auth/v1/signup",
            method = "POST",
            body = body,
            extraHeaders = supabaseHeaders(json = true),
            requiresAuthEnvelope = false
        )
        if (json.optString("access_token").isBlank()) {
            throw OnlineApiException(context.getString(R.string.online_error_confirm_email))
        }
        json.toSession()
    }

    suspend fun refreshSession(session: OnlineUserSession): OnlineUserSession = withContext(Dispatchers.IO) {
        ensureSupabaseConfigured()
        val refreshToken = session.refreshToken ?: throw OnlineApiException(context.getString(R.string.online_error_missing_refresh_token))
        val body = JSONObject().put("refresh_token", refreshToken).toString().toByteArray()
        val json = requestJson(
            url = "$supabaseUrl/auth/v1/token?grant_type=refresh_token",
            method = "POST",
            body = body,
            extraHeaders = supabaseHeaders(json = true),
            requiresAuthEnvelope = false
        )
        json.toSession(fallbackEmail = session.email, fallbackUserId = session.userId)
    }

    suspend fun validateSession(session: OnlineUserSession): OnlineUserSession = withContext(Dispatchers.IO) {
        val active = if (session.isExpired) refreshSession(session) else session
        try {
            requestJson(
                url = "$apiPrefix/auth/me",
                method = "GET",
                bearerToken = active.accessToken,
                appSignature = false
            )
            active
        } catch (throwable: Throwable) {
            if (active.refreshToken != null) refreshSession(active) else throw throwable
        }
    }

    suspend fun getSongs(session: OnlineUserSession, limit: Int = 30, offset: Int = 0): OnlineSongsResponse = withContext(Dispatchers.IO) {
        val page = (offset / limit.coerceAtLeast(1)) + 1
        val json = requestJson(
            url = "$apiPrefix/songs?limit=${limit.coerceIn(1, 100)}&page=$page",
            method = "GET",
            bearerToken = session.accessToken,
            appSignature = false
        )
        parseSongsResponse(json, limit, offset)
    }

    suspend fun searchSongs(session: OnlineUserSession, query: String, limit: Int = 30, offset: Int = 0): OnlineSongsResponse = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val page = (offset / limit.coerceAtLeast(1)) + 1
        val catalogUrl = "$apiPrefix/catalog/search?q=$encoded&limit=${limit.coerceIn(1, 50)}&page=$page"
        val json = runCatching {
            requestJson(
                url = catalogUrl,
                method = "GET",
                bearerToken = session.accessToken,
                appSignature = appSecret.isNotBlank()
            )
        }.getOrElse {
            requestJson(
                url = "$apiPrefix/songs?q=$encoded&limit=${limit.coerceIn(1, 100)}&page=$page",
                method = "GET",
                bearerToken = session.accessToken,
                appSignature = false
            )
        }
        parseSongsResponse(json, limit, offset)
    }

    suspend fun getSongDetail(session: OnlineUserSession, id: String): OnlineSongDto = withContext(Dispatchers.IO) {
        val json = requestJson(
            url = "$apiPrefix/songs/$id",
            method = "GET",
            bearerToken = session.accessToken,
            appSignature = false
        )
        val data = json.optJSONObject("data") ?: json
        parseSong(data)
    }

    suspend fun getLyrics(session: OnlineUserSession, id: String): OnlineLyricsDto = withContext(Dispatchers.IO) {
        val song = getSongDetail(session, id)
        OnlineLyricsDto(songId = id, lyrics = song.lyrics, synchronized = false, source = song.source)
    }

    suspend fun uploadSong(session: OnlineUserSession, entry: MediaEntry): OnlineSongDto = withContext(Dispatchers.IO) {
        val file = copyContentUriToTempFile(entry)
        try {
            val boundary = "----NexoraBoundary${System.currentTimeMillis()}"
            val body = buildMultipartBody(boundary, file, entry)
            val json = requestJson(
                url = "$apiPrefix/songs/upload",
                method = "POST",
                body = body,
                contentType = "multipart/form-data; boundary=$boundary",
                bearerToken = session.accessToken,
                appSignature = appSecret.isNotBlank(),
                requiresAuthEnvelope = true
            )
            val data = json.optJSONObject("data") ?: json
            parseSong(data)
        } finally {
            runCatching { file.delete() }
        }
    }

    fun streamingHeaders(session: OnlineUserSession): Map<String, String> {
        return buildMap {
            put("Authorization", "Bearer ${session.accessToken}")
            put("x-client-id", "nexora-player-android")
            put("x-client-version", BuildConfig.VERSION_NAME)
            put("x-app-platform", "android")
            put("x-app-id", appClientId)
            put("x-device-id", deviceId)
        }
    }

    private fun ensureSupabaseConfigured() {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            throw OnlineApiException(context.getString(R.string.online_error_missing_supabase_config))
        }
    }

    private fun supabaseHeaders(json: Boolean): Map<String, String> = buildMap {
        put("apikey", supabaseAnonKey)
        put("Authorization", "Bearer $supabaseAnonKey")
        if (json) put("Content-Type", "application/json")
    }

    private fun requestJson(
        url: String,
        method: String,
        body: ByteArray? = null,
        contentType: String = "application/json",
        bearerToken: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        appSignature: Boolean = appSecret.isNotBlank(),
        requiresAuthEnvelope: Boolean = true
    ): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-client-id", "nexora-player-android")
            setRequestProperty("x-client-version", BuildConfig.VERSION_NAME)
            setRequestProperty("x-app-platform", "android")
            setRequestProperty("x-app-id", appClientId)
            setRequestProperty("x-device-id", deviceId)
            bearerToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            extraHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType)
                setRequestProperty("Content-Length", body.size.toString())
            }
        }

        if (appSignature) {
            val uri = URL(url)
            val pathAndQuery = uri.file
            val bodyHash = sha256Hex(body ?: ByteArray(0))
            val timestamp = System.currentTimeMillis().toString()
            val canonical = "${method.uppercase()}\n$pathAndQuery\n$timestamp\n$deviceId\n$bodyHash"
            connection.setRequestProperty("x-app-timestamp", timestamp)
            connection.setRequestProperty("x-body-sha256", bodyHash)
            connection.setRequestProperty("x-app-signature", hmacSha256Hex(appSecret, canonical))
        }

        body?.let { bytes -> connection.outputStream.use { it.write(bytes) } }

        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        val json = runCatching { JSONObject(response) }.getOrElse { JSONObject().put("message", response) }
        if (code !in 200..299) {
            val error = json.optJSONObject("error")
            val message = error?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
                ?: context.getString(R.string.online_error_http, code)
            throw OnlineApiException(message)
        }
        if (requiresAuthEnvelope && json.has("success") && !json.optBoolean("success", false)) {
            val message = json.optJSONObject("error")?.optString("message") ?: context.getString(R.string.online_error_server_generic)
            throw OnlineApiException(message)
        }
        return json
    }

    private fun parseSongsResponse(json: JSONObject, requestedLimit: Int, requestedOffset: Int): OnlineSongsResponse {
        val data = json.opt("data")
        val itemsJson: JSONArray = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("results") ?: data.optJSONArray("items") ?: data.optJSONArray("songs") ?: JSONArray()
            else -> json.optJSONArray("items") ?: json.optJSONArray("results") ?: JSONArray()
        }
        val items = buildList {
            for (index in 0 until itemsJson.length()) {
                itemsJson.optJSONObject(index)?.let { add(parseSong(it)) }
            }
        }
        val metaPagination = json.optJSONObject("meta")?.optJSONObject("pagination")
            ?: (data as? JSONObject)?.optJSONObject("meta")?.optJSONObject("pagination")
        val total = metaPagination?.optInt("total", items.size)
            ?: (data as? JSONObject)?.optInt("total", items.size)
            ?: json.optInt("total", items.size)
        val limit = metaPagination?.optInt("limit", requestedLimit) ?: requestedLimit
        val page = metaPagination?.optInt("page", requestedOffset / requestedLimit.coerceAtLeast(1) + 1) ?: 1
        val offset = ((page - 1).coerceAtLeast(0)) * limit
        return OnlineSongsResponse(items = items, total = total, limit = limit, offset = offset)
    }

    private fun parseSong(json: JSONObject): OnlineSongDto {
        val id = json.optString("id")
        val streamUrl = json.optString("stream_url").takeIf { it.isNotBlank() }
        val audioUrl = json.optString("audioUrl").takeIf { it.isNotBlank() }
            ?: json.optString("audio_url").takeIf { it.isNotBlank() }
            ?: streamUrl
        return OnlineSongDto(
            id = id,
            title = json.optString("title", "Nexora Online"),
            artist = json.optString("artist").takeIf { it.isNotBlank() },
            album = json.optString("album").takeIf { it.isNotBlank() },
            genre = json.optString("genre").takeIf { it.isNotBlank() },
            durationSeconds = when {
                json.has("durationSeconds") -> json.optLong("durationSeconds")
                json.has("duration_seconds") -> json.optLong("duration_seconds")
                else -> null
            },
            audioUrl = audioUrl,
            coverUrl = json.optString("coverUrl").takeIf { it.isNotBlank() }
                ?: json.optString("cover_url").takeIf { it.isNotBlank() },
            lyricsUrl = json.optString("lyricsUrl").takeIf { it.isNotBlank() }
                ?: json.optString("lyrics_url").takeIf { it.isNotBlank() },
            lyrics = json.optString("lyrics").takeIf { it.isNotBlank() },
            source = json.optString("source").takeIf { it.isNotBlank() }
                ?: json.optString("source_type").takeIf { it.isNotBlank() },
            canDownload = json.optBoolean("canDownload", json.optBoolean("can_download", false)),
            createdAt = json.optString("createdAt").takeIf { it.isNotBlank() }
                ?: json.optString("created_at").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.toSession(fallbackEmail: String? = null, fallbackUserId: String? = null): OnlineUserSession {
        val expiresIn = optLong("expires_in", 3600L).coerceAtLeast(60L)
        val user = optJSONObject("user")
        return OnlineUserSession(
            accessToken = optString("access_token"),
            refreshToken = optString("refresh_token").takeIf { it.isNotBlank() },
            expiresAtEpochSeconds = System.currentTimeMillis() / 1000L + expiresIn,
            email = user?.optString("email")?.takeIf { it.isNotBlank() } ?: fallbackEmail,
            userId = user?.optString("id")?.takeIf { it.isNotBlank() } ?: fallbackUserId
        )
    }

    private fun copyContentUriToTempFile(entry: MediaEntry): File {
        val suffix = when {
            entry.mimeType?.contains("mpeg", ignoreCase = true) == true -> ".mp3"
            entry.mimeType?.contains("mp4", ignoreCase = true) == true -> ".m4a"
            entry.mimeType?.contains("flac", ignoreCase = true) == true -> ".flac"
            entry.mimeType?.contains("wav", ignoreCase = true) == true -> ".wav"
            else -> ".audio"
        }
        val file = File.createTempFile("nexora-upload-", suffix, context.cacheDir)
        context.contentResolver.openInputStream(entry.uri)?.use { input ->
            BufferedInputStream(input).use { buffered ->
                FileOutputStream(file).use { output -> buffered.copyTo(output) }
            }
        } ?: throw OnlineApiException(context.getString(R.string.online_error_read_local_file, entry.title))
        return file
    }

    private fun buildMultipartBody(boundary: String, file: File, entry: MediaEntry): ByteArray {
        val out = ByteArrayOutputStream()
        fun write(value: String) = out.write(value.toByteArray())
        fun field(name: String, value: String) {
            write("--$boundary\r\n")
            write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
            write(value)
            write("\r\n")
        }
        field("title", entry.title)
        field("artist", entry.artist.ifBlank { context.getString(R.string.online_unknown_artist) })
        if (entry.album.isNotBlank()) field("album", entry.album)
        if (entry.durationMs > 0L) field("duration", (entry.durationMs / 1000L).toString())
        write("--$boundary\r\n")
        write("Content-Disposition: form-data; name=\"file\"; filename=\"${entry.title.safeFilename()}\"\r\n")
        write("Content-Type: ${entry.mimeType ?: "audio/mpeg"}\r\n\r\n")
        out.write(file.readBytes())
        write("\r\n--$boundary--\r\n")
        return out.toByteArray()
    }

    private fun String.safeFilename(): String = replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "audio.mp3" }

    private fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun hmacSha256Hex(secret: String, value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

class OnlineApiException(message: String) : Exception(message)
