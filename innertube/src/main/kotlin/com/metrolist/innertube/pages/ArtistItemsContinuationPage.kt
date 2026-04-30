package com.ecoute.music.innertube.pages

import com.ecoute.music.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
