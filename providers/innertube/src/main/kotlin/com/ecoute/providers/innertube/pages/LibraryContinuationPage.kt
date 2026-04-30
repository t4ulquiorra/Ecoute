package com.ecoute.providers.innertube.pages

import com.ecoute.providers.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
