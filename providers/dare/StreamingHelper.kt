package com.ecoute.providers.dare

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

object StreamingHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val PIPED_API = "https://pipedapi.kavin.rocks"

    suspend fun getPipedAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$PIPED_API/streams/$videoId")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = org.json.JSONObject(body)
            val audioStreams = json.optJSONArray("audioStreams") ?: return@withContext null

            var bestUrl: String? = null
            var bestBitrate = 0

            for (i in 0 until audioStreams.length()) {
                val stream = audioStreams.getJSONObject(i)
                val url = stream.optString("url", null)
                val bitrate = stream.optInt("bitrate", 0)

                if (url != null && bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = url
                }
            }

            bestUrl
        } catch (e: Exception) {
            null
        }
    }
}
