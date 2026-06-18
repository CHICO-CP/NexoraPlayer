package com.nexora.player.data.online

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nexora.player.data.model.DownloadStorageMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.text.Normalizer

object OnlineTrackDownloader {

    suspend fun download(
        context: Context,
        track: OnlineTrack,
        mode: DownloadStorageMode
    ): Uri = withContext(Dispatchers.IO) {
        val downloadUrl = track.downloadUrl?.takeIf { it.isNotBlank() }
            ?: error("Track does not expose a download URL")

        when (mode) {
            DownloadStorageMode.ASK_FIRST_TIME -> error("Download storage mode not selected")
            DownloadStorageMode.APP_PRIVATE -> saveToPrivateStorage(context, track, downloadUrl)
            DownloadStorageMode.PUBLIC_DOWNLOADS -> saveToPublicDownloads(context, track, downloadUrl)
        }
    }

    private fun saveToPrivateStorage(context: Context, track: OnlineTrack, downloadUrl: String): Uri {
        val root = File(context.filesDir, "nexora_downloads/audios").apply { mkdirs() }
        val fileName = buildFileName(track, downloadUrl)
        val output = File(root, fileName)
        openConnection(downloadUrl).inputStream.use { input ->
            output.outputStream().use { outputStream ->
                input.copyTo(outputStream)
            }
        }
        return Uri.fromFile(output)
    }

    private fun saveToPublicDownloads(context: Context, track: OnlineTrack, downloadUrl: String): Uri {
        val fileName = buildFileName(track, downloadUrl)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, guessMimeType(fileName))
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/NexoraPlayer/audios"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: error("Unable to create media store entry")
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    openConnection(downloadUrl).inputStream.use { input ->
                        input.copyTo(output)
                    }
                } ?: error("Unable to open output stream")
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }.also { updateValues ->
                    resolver.update(uri, updateValues, null, null)
                }
                uri
            } catch (throwable: Throwable) {
                resolver.delete(uri, null, null)
                throw throwable
            }
        } else {
            val root = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "NexoraPlayer/audios"
            ).apply { mkdirs() }
            val output = File(root, fileName)
            openConnection(downloadUrl).inputStream.use { input ->
                output.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
            Uri.fromFile(output)
        }
    }

    private fun openConnection(downloadUrl: String): HttpURLConnection {
        return (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NexoraPlayer")
        }
    }

    private fun buildFileName(track: OnlineTrack, downloadUrl: String): String {
        val rawBase = listOf(track.artist, track.title)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { track.title.ifBlank { "nexora_track" } }
        val safeBase = sanitizeFileName(rawBase)
        val extension = extensionFrom(downloadUrl)
        return "$safeBase.$extension"
    }

    private fun sanitizeFileName(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "nexora_track" }
    }

    private fun extensionFrom(downloadUrl: String): String {
        val segment = Uri.parse(downloadUrl).lastPathSegment.orEmpty()
        val candidate = segment.substringAfterLast('.', "")
            .substringBefore('?')
            .trim()
        return candidate.takeIf { it.isNotBlank() && it.length <= 5 } ?: "mp3"
    }

    private fun guessMimeType(fileName: String): String {
        return URLConnection.guessContentTypeFromName(fileName) ?: "audio/mpeg"
    }
}
