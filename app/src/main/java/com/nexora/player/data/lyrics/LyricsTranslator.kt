package com.nexora.player.data.lyrics

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LyricsTranslator {

    suspend fun translateRawLyrics(
        rawText: String,
        targetLanguageTag: String
    ): String? = withContext(Dispatchers.IO) {
        val targetCode = TranslateLanguage.fromLanguageTag(targetLanguageTag) ?: return@withContext null
        val sampleText = rawText
            .lineSequence()
            .map { stripTags(it).trim() }
            .filter { it.isNotBlank() }
            .take(24)
            .joinToString("\n")

        if (sampleText.isBlank()) return@withContext null

        val languageId = LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder().build()
        )

        val sourceLanguageTag = try {
            Tasks.await(languageId.identifyLanguage(sampleText)).takeIf { it != "und" }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { languageId.close() }
        }

        if (sourceLanguageTag.isNullOrBlank()) return@withContext null
        if (sourceLanguageTag.equals(targetLanguageTag, ignoreCase = true)) return@withContext null

        val sourceCode = TranslateLanguage.fromLanguageTag(sourceLanguageTag) ?: return@withContext null
        if (sourceCode == targetCode) return@withContext null

        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()
        )

        try {
            Tasks.await(translator.downloadModelIfNeeded(DownloadConditions.Builder().build()))

            rawText.lineSequence().joinToString("\n") { line ->
                val match = LRC_PREFIX_REGEX.find(line)
                if (match == null) {
                    translateLine(translator, line)
                } else {
                    val prefix = match.groupValues[1]
                    val remainder = match.groupValues[2].trim()
                    if (remainder.isBlank()) line else "$prefix${translateLine(translator, remainder)}"
                }
            }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { translator.close() }
        }
    }

    private fun translateLine(
        translator: com.google.mlkit.nl.translate.Translator,
        text: String
    ): String {
        if (text.isBlank()) return text
        return try {
            Tasks.await(translator.translate(text))
        } catch (_: Throwable) {
            text
        }
    }

    private fun stripTags(line: String): String {
        val match = LRC_PREFIX_REGEX.find(line)
        return match?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() } ?: line
    }

    private val LRC_PREFIX_REGEX = Regex("^((?:\\[[^\\]]+\\])+)(.*)$")
}
