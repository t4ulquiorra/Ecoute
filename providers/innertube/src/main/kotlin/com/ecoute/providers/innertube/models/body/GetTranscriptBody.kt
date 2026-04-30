package com.ecoute.providers.innertube.models.body

import com.ecoute.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
