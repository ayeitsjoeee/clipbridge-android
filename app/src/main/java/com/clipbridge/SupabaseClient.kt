package com.clipbridge

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object SupabaseClient {
    private const val SUPABASE_URL = "https://etfcsdcmcncxqldvfucp.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_M03zbACG3IncmDR5e0EeEw_rLMx3pPx"
    private const val SOURCE_DEVICE = "android"

    private val client = OkHttpClient()
    private val JSON_MEDIA = "application/json".toMediaType()

    private fun baseHeaders() = arrayOf(
        "apikey" to SUPABASE_KEY,
        "Authorization" to "Bearer $SUPABASE_KEY",
        "Content-Type" to "application/json"
    )

    fun pushText(text: String) {
        val body = JSONObject().apply {
            put("content", text)
            put("content_type", "text")
            put("source_device", SOURCE_DEVICE)
        }.toString().toRequestBody(JSON_MEDIA)

        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/clipboard_items")
            .apply { baseHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .addHeader("Prefer", "return=minimal")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    fun getLatestFromMac(callback: (JSONObject?) -> Unit) {
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/clipboard_items?source_device=eq.mac&order=created_at.desc&limit=1")
            .apply { baseHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null) }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                response.close()
                try {
                    val arr = JSONArray(body)
                    callback(if (arr.length() > 0) arr.getJSONObject(0) else null)
                } catch (e: Exception) { callback(null) }
            }
        })
    }
}
