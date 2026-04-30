package com.ecoute.providers.innertube.models.body

import com.ecoute.providers.innertube.models.Context
import com.ecoute.providers.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
