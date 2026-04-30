package com.ecoute.music.innertube.models.body

import com.ecoute.music.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
