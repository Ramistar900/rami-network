package com.rami.network.data

import com.rami.network.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AdminApi {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    suspend fun signIn(email: String, password: String): String = withContext(Dispatchers.IO) {
        val response = request(
            "$baseUrl/auth/v1/token?grant_type=password",
            JSONObject().put("email", email.trim()).put("password", password),
            null
        )
        response.optString("access_token").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("لم يُرجع تسجيل الدخول جلسة صالحة.")
    }

    suspend fun call(accessToken: String, action: String, payload: JSONObject = JSONObject()): JSONObject =
        withContext(Dispatchers.IO) {
            val response = request(
                "$baseUrl/functions/v1/rami-network-control",
                JSONObject().put("action", action).put("payload", payload),
                accessToken
            )
            if (response.optBoolean("ok")) response
            else throw IllegalStateException(response.optString("error", "تعذر تنفيذ العملية."))
        }

    private fun request(endpoint: String, body: JSONObject, accessToken: String?): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("apikey", publishableKey)
            if (accessToken != null) setRequestProperty("Authorization", "Bearer " + accessToken)
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (status !in 200..299) {
                val message = json.optString("msg")
                    .ifBlank { json.optString("message") }
                    .ifBlank { json.optString("error_description") }
                    .ifBlank { json.optString("error") }
                    .ifBlank { "فشل الاتصال بالخادم (HTTP $status)." }
                throw IllegalStateException(message)
            }
            return json
        } finally {
            connection.disconnect()
        }
    }
}
