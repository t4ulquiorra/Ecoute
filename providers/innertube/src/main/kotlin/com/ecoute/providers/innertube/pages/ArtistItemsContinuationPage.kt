package com.ecoute.providers.innertube.pages

import com.ecoute.providers.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
