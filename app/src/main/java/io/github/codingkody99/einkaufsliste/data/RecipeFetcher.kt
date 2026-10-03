package io.github.codingkody99.einkaufsliste.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

sealed interface FetchResult {
    data class Success(val html: String) : FetchResult

    /** [reason] is shown to the user, so it is phrased for them. */
    data class Failure(val reason: String) : FetchResult
}

interface RecipeFetcher {
    suspend fun fetch(url: String): FetchResult
}

/**
 * Downloads a recipe page with the JDK's own HTTP client — one GET needs no
 * networking library.
 *
 * The response is capped at [MAX_BYTES]: a phone must not try to hold an
 * arbitrarily large page in memory, and the structured data we are after sits
 * well within that.
 */
class HttpRecipeFetcher(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val userAgent: String = DEFAULT_USER_AGENT,
) : RecipeFetcher {

    override suspend fun fetch(url: String): FetchResult = withContext(dispatcher) {
        val target = normalize(url) ?: return@withContext FetchResult.Failure("Keine gültige Adresse.")

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(target).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
                setRequestProperty("Accept-Language", "de-DE,de;q=0.9")
                setRequestProperty("Accept-Encoding", "gzip")
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                return@withContext FetchResult.Failure(
                    when (status) {
                        401, 403 -> "Die Seite hat den Zugriff abgelehnt ($status)."
                        404, 410 -> "Die Seite wurde nicht gefunden ($status)."
                        in 500..599 -> "Die Seite hat einen Serverfehler gemeldet ($status)."
                        else -> "Die Seite antwortete mit Fehler $status."
                    },
                )
            }

            val contentType = connection.contentType.orEmpty()
            if (contentType.isNotEmpty() && !contentType.contains("html", ignoreCase = true) &&
                !contentType.contains("xml", ignoreCase = true)
            ) {
                return@withContext FetchResult.Failure("Die Adresse zeigt auf keine Webseite.")
            }

            val body = connection.bodyStream().use { stream ->
                stream.readCapped(MAX_BYTES).toString(charsetOf(contentType))
            }
            if (body.isBlank()) FetchResult.Failure("Die Seite war leer.") else FetchResult.Success(body)
        } catch (e: java.net.UnknownHostException) {
            FetchResult.Failure("Keine Verbindung — ist das Internet erreichbar?")
        } catch (e: java.net.SocketTimeoutException) {
            FetchResult.Failure("Die Seite hat zu lange gebraucht.")
        } catch (e: java.io.IOException) {
            FetchResult.Failure("Die Seite konnte nicht geladen werden (${e.javaClass.simpleName}).")
        } finally {
            connection?.disconnect()
        }
    }

    private fun HttpURLConnection.bodyStream(): InputStream {
        val raw = inputStream
        return if (contentEncoding?.contains("gzip", ignoreCase = true) == true) {
            GZIPInputStream(raw)
        } else {
            raw
        }
    }

    /** Accepts a bare host as well, which is what people paste more often than not. */
    private fun normalize(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return runCatching { URL(withScheme) }.getOrNull()?.takeIf { !it.host.isNullOrBlank() }?.toString()
    }

    private fun charsetOf(contentType: String): java.nio.charset.Charset {
        val name = CHARSET.find(contentType)?.groupValues?.get(1)?.trim('"', '\'', ' ')
        return runCatching { java.nio.charset.Charset.forName(name) }.getOrDefault(Charsets.UTF_8)
    }

    private fun InputStream.readCapped(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        while (out.size() < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        /**
         * Sites commonly turn away unknown clients, so the request identifies
         * itself as the browser the app is running in.
         */
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0 Mobile Safari/537.36"

        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 20_000
        const val MAX_BYTES = 3 * 1024 * 1024
        const val BUFFER_SIZE = 16 * 1024

        val CHARSET = Regex("charset=([^;]+)", RegexOption.IGNORE_CASE)
    }
}
