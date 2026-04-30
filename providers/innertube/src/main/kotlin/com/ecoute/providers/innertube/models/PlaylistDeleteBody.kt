package com.ecoute.providers.innertube.models.body

import com.ecoute.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)
