package com.yourname.teleprompter.engine

import android.util.Base64
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yourname.teleprompter.data.remote.AsrClientAudio
import com.yourname.teleprompter.data.remote.AsrServerMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * MiniMax 流式 ASR WebSocket 客户端
 * 注意：当前实现使用占位 URL，实际接入时替换为真实 endpoint
 */
class AsrEngine(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val msgAdapter = moshi.adapter(AsrServerMessage::class.java)
    private val audioAdapter = moshi.adapter(AsrClientAudio::class.java)

    fun stream(pcmFlow: Flow<ByteArray>): Flow<AsrServerMessage> = callbackFlow {
        val req = Request.Builder()
            .url(ASR_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Model", MODEL_NAME)
            .build()

        val socket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { msgAdapter.fromJson(text) }
                    .getOrNull()
                    ?.let { trySend(it) }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }
        })

        val job = launch(Dispatchers.IO) {
            pcmFlow.collect { pcm ->
                val b64 = Base64.encodeToString(pcm, Base64.NO_WRAP)
                socket.send(audioAdapter.toJson(
                    AsrClientAudio(audio = b64)
                ))
            }
        }

        awaitClose {
            job.cancel()
            socket.close(1000, "client_close")
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        // 占位 URL —— 接入时替换为 MiniMax 实际 ASR endpoint
        private const val ASR_URL = "wss://api.MiniMax.chat/v1/audio/asr/stream"
        private const val MODEL_NAME = "MiniMax-voice-01"
    }
}