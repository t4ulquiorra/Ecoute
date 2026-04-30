package com.ecoute.providers.innertube.pages

import com.ecoute.providers.innertube.models.Album
import com.ecoute.providers.innertube.models.Artist
import com.ecoute.providers.innertube.models.BrowseEndpoint
import com.ecoute.providers.innertube.models.PlaylistPanelVideoRenderer
import com.ecoute.providers.innertube.models.SongItem
import com.ecoute.providers.innertube.models.WatchEndpoint
import com.ecoute.providers.innertube.models.oddElements
import com.ecoute.providers.innertube.models.splitBySeparator
import com.ecoute.providers.innertube.utils.parseTime

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint, // current or continuation next endpoint
)

object NextPage {
    /**
     * Parses a [PlaylistPanelVideoRenderer] (the "up next" / queue item renderer from YouTube Music)
     * into a [SongItem]. Returns `null` if required fields (videoId, title) are missing.
     *
     * Extracts artist and album info from [PlaylistPanelVideoRenderer.longBylineText],
     * falling back to [PlaylistPanelVideoRenderer.shortBylineText], and resolves library
     * add/remove tokens from the renderer's menu items.
     */
    fun fromPlaylistPanelVideoRenderer(renderer: PlaylistPanelVideoRenderer): SongItem? {
        val longByLineRuns = (renderer.longBylineText ?: renderer.shortBylineText)?.runs?.splitBySeparator()

        // Extract library tokens using the new method that properly handles multiple toggle items
        val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

        return SongItem(
            id = renderer.videoId ?: return null,
            title =
                renderer.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text ?: return null,
            artists =
                longByLineRuns?.firstOrNull()?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                } ?: emptyList(),
            album =
                longByLineRuns
                    ?.getOrNull(1)
                    ?.firstOrNull()
                    ?.let {
                        val albumId = it.navigationEndpoint?.browseEndpoint?.browseId
                            ?: return@let null
                        Album(
                            name = it.text,
                            id = albumId,
                        )
                    },
            duration =
                renderer.lengthText
                    ?.runs
                    ?.firstOrNull()
                    ?.text
                    ?.parseTime(),
            musicVideoType = renderer.navigationEndpoint.musicVideoType,
            thumbnail =
                renderer.thumbnail.thumbnails
                    .lastOrNull()
                    ?.url ?: "",
            explicit =
                renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
            libraryAddToken = libraryTokens.addToken,
            libraryRemoveToken = libraryTokens.removeToken
        )
    }
}
