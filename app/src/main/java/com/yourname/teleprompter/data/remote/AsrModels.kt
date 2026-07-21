package com.yourname.teleprompter.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class AsrConfig(
    val sampleRate: Int = 16_000,
    val lang: String = "zh",
    val format: String = "pcm16k"
)

@JsonClass(generateAdapter = false)
data class AsrServerMessage(
    val type: String? = null,        // "partial" / "final" / "error"
    val text: String? = null,
    val isFinal: Boolean? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = false)
data class AsrClientAudio(
    val type: String = "audio",
    val audio: String,               // base64
    val format: String = "pcm16k",
    val lang: String = "zh"
)