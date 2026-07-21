package com.yourname.teleprompter.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * 16kHz / 16bit / 单声道 PCM 采集
 * 输出 ByteArray（每个 chunk 大小 = 200ms 音频）
 */
class AudioRecorder {

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun stream(): Flow<ByteArray> = callbackFlow {
        val sampleRate = 16_000
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        val bufferSize = (minBuffer * 2).coerceAtLeast(6400)   // 至少 200ms

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate, channel, encoding, bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            close(IllegalStateException("AudioRecord 初始化失败"))
            return@callbackFlow
        }

        recorder.startRecording()
        val chunkSize = (sampleRate * 0.2).toInt() * 2   // 200ms

        val readThread = Thread {
            val buffer = ByteArray(chunkSize)
            while (!isClosedForSend) {
                val n = recorder.read(buffer, 0, buffer.size)
                if (n > 0) {
                    trySend(buffer.copyOf(n))
                }
            }
        }.apply { start() }

        awaitClose {
            readThread.join(500)
            try { recorder.stop() } catch (_: Throwable) {}
            try { recorder.release() } catch (_: Throwable) {}
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHUNK_MS = 200
    }
}