package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AppsScriptSyncClient {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(webAppUrl: String): Result<String> = withContext(Dispatchers.IO) {
        val url = webAppUrl.trim()
        if (!url.startsWith("https://")) {
            return@withContext Result.failure(IllegalArgumentException("URL Web App harus diawali https://"))
        }
        try {
            val req = Request.Builder()
                .url(url)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    Result.success("Terhubung (HTTP ${resp.code}): ${body.take(120).ifBlank { "OK" }}")
                } else {
                    Result.failure(IllegalStateException("Gagal terhubung (HTTP ${resp.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Koneksi gagal: ${e.localizedMessage ?: "Tidak dapat menjangkau URL"}"))
        }
    }

    suspend fun uploadBackup(webAppUrl: String, backupJson: String): Result<String> = withContext(Dispatchers.IO) {
        val url = webAppUrl.trim()
        if (!url.startsWith("https://")) {
            return@withContext Result.failure(IllegalArgumentException("URL Web App Google Drive belum valid"))
        }
        try {
            val wrapper = JSONObject().apply {
                put("action", "backup_sewa_outdoor")
                put("timestamp", Formatters.nowTimestampLabel())
                put("data", JSONObject(backupJson))
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = wrapper.toString().toRequestBody(mediaType)
            val req = Request.Builder()
                .url(url)
                .post(body)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success("Backup tersinkronisasi ke Google Drive (${Formatters.nowTimestampLabel()})")
                } else {
                    Result.failure(IllegalStateException("Gagal sync ke Drive (HTTP ${resp.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Sync Drive gagal: ${e.localizedMessage ?: "Error jaringan"}"))
        }
    }
}
