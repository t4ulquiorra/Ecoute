package com.ecoute.innertube.pages

import com.ecoute.innertube.models.Album
import com.ecoute.innertube.models.AlbumItem
import com.ecoute.innertube.models.Artist
import com.ecoute.innertube.models.ArtistItem
import com.ecoute.innertube.models.MusicResponsiveListItemRenderer
import com.ecoute.innertube.models.MusicTwoRowItemRenderer
import com.ecoute.innertube.models.PlaylistItem
import com.ecoute.innertube.models.SongItem
import com.ecoute.innertube.models.YTItem
import com.ecoute.innertube.models.oddElements
import com.ecoute.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
