package com.ecoute.innertube.pages

import com.ecoute.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
