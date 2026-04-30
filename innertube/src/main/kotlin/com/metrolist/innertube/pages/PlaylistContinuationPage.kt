package com.ecoute.music.innertube.pages

import com.ecoute.music.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
