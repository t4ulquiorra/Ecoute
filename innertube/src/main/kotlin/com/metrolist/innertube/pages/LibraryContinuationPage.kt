package com.ecoute.music.innertube.pages

import com.ecoute.music.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
