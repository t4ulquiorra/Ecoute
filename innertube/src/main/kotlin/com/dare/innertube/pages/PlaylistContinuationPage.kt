package com.ecoute.innertube.pages

import com.ecoute.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
