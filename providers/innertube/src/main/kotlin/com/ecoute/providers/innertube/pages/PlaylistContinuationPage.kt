package com.ecoute.providers.innertube.pages

import com.ecoute.providers.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
