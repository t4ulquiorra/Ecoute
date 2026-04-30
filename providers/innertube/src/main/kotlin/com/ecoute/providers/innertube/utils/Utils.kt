package com.ecoute.providers.innertube.utils

import com.ecoute.providers.innertube.Innertube
import com.ecoute.providers.innertube.models.SectionListRenderer

private val SectionListRenderer.Content.title: String? get() {
    val title = musicCarouselShelfRenderer
        ?.header
        ?.musicCarouselShelfBasicHeaderRenderer
        ?.title
        ?: musicShelfRenderer
            ?.title

    return title?.runs?.firstOrNull()?.text
}

private val SectionListRenderer.Content.strapline get() = musicCarouselShelfRenderer
    ?.header
    ?.musicCarouselShelfBasicHeaderRenderer
    ?.strapline
    ?.runs
    ?.firstOrNull()
    ?.text

internal fun SectionListRenderer.findSectionByTitle(text: String) = contents
    ?.find { it.title == text }
    ?: contents?.find { it.title?.contains(text, ignoreCase = true) == true }

internal fun SectionListRenderer.findSectionByStrapline(text: String) = contents?.find { it.strapline == text }

infix operator fun <T : Innertube.Item> Innertube.ItemsPage<T>?.plus(other: Innertube.ItemsPage<T>) =
    other.copy(
        items = (this?.items?.plus(other.items ?: emptyList()) ?: other.items)
            ?.distinctBy(Innertube.Item::key),
        continuation = other.continuation ?: this?.continuation
    )

fun String.parseTime(): Int? {
    val parts = trim().split(":").map { it.toIntOrNull() ?: return null }
    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> null
    }
}
