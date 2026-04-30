package com.ecoute.music.utils

import androidx.media3.common.MediaItem
import com.ecoute.innertube.YouTube
import com.ecoute.innertube.models.bodies.ContinuationBody
import com.ecoute.innertube.models.bodies.NextBody
import com.ecoute.innertube.requests.nextPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class YouTubeRadio(
    private val videoId: String? = null,
    private var playlistId: String? = null,
    private var playlistSetVideoId: String? = null,
    private var parameters: String? = null
) {
    private var nextContinuation: String? = null

    suspend fun process(): List<MediaItem> {
        var mediaItems: List<MediaItem>? = null

        nextContinuation = withContext(Dispatchers.IO) {
            val continuation = nextContinuation

            if (continuation == null) {
               YouTube.nextPage(
                    NextBody(
                        videoId = videoId,
                        playlistId = playlistId,
                        params = parameters,
                        playlistSetVideoId = playlistSetVideoId
                    )
                )?.map { nextResult ->
                    playlistId = nextResult.playlistId
                    parameters = nextResult.params
                    playlistSetVideoId = nextResult.playlistSetVideoId

                    nextResult.itemsPage
                }
            } else {
                YouTube.nextPage(ContinuationBody(continuation = continuation))
            }?.getOrNull()?.let { songsPage ->
                mediaItems = songsPage.items?.map(YouTube.SongItem::asMediaItem)
                songsPage.continuation?.takeUnless { nextContinuation == it }
            }

        }

        return mediaItems ?: emptyList()
    }
}
