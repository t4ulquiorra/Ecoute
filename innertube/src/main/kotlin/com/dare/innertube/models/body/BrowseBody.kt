package com.ecoute.innertube.models.body

import com.ecoute.innertube.models.Context
import com.ecoute.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
