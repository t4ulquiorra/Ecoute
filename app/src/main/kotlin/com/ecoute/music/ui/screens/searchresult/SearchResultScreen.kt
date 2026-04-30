package com.ecoute.music.ui.screens.searchresult

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ecoute.compose.persist.PersistMapCleanup
import com.ecoute.compose.persist.persistMap
import com.ecoute.innertube.YouTube
import com.ecoute.innertube.models.bodies.ContinuationBody
import com.ecoute.innertube.models.bodies.SearchBody
import com.ecoute.innertube.requests.searchPage
import com.ecoute.innertube.utils.from
import com.ecoute.compose.routing.RouteHandler
import com.ecoute.music.LocalPlayerServiceBinder
import com.ecoute.music.R
import com.ecoute.music.ui.components.LocalMenuState
import com.ecoute.music.ui.components.themed.Header
import com.ecoute.music.ui.components.themed.NonQueuedMediaItemMenu
import com.ecoute.music.ui.components.themed.Scaffold
import com.ecoute.music.ui.items.AlbumItem
import com.ecoute.music.ui.items.AlbumItemPlaceholder
import com.ecoute.music.ui.items.ArtistItem
import com.ecoute.music.ui.items.ArtistItemPlaceholder
import com.ecoute.music.ui.items.PlaylistItem
import com.ecoute.music.ui.items.PlaylistItemPlaceholder
import com.ecoute.music.ui.items.SongItem
import com.ecoute.music.ui.items.SongItemPlaceholder
import com.ecoute.music.ui.items.VideoItem
import com.ecoute.music.ui.items.VideoItemPlaceholder
import com.ecoute.music.ui.screens.albumRoute
import com.ecoute.music.ui.screens.artistRoute
import com.ecoute.music.ui.screens.globalRoutes
import com.ecoute.music.ui.screens.playlistRoute
import com.ecoute.music.ui.styling.Dimensions
import com.ecoute.music.ui.styling.px
import com.ecoute.music.utils.asMediaItem
import com.ecoute.music.utils.forcePlay
import com.ecoute.music.utils.rememberPreference
import com.ecoute.music.utils.searchResultScreenTabIndexKey

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun SearchResultScreen(query: String, onSearchAgain: () -> Unit) {
    val context = LocalContext.current
    val saveableStateHolder = rememberSaveableStateHolder()
    val (tabIndex, onTabIndexChanges) = rememberPreference(searchResultScreenTabIndexKey, 0)

    PersistMapCleanup(tagPrefix = "searchResults/$query/")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit = {
                Header(
                    title = query,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures {
                                context.persistMap?.keys?.removeAll {
                                    it.startsWith("searchResults/$query/")
                                }
                                onSearchAgain()
                            }
                        }
                )
            }

            val emptyItemsText = "No results found. Please try a different query or category"

            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanges,
                tabColumnContent = { Item ->
                    Item(0, "Songs", R.drawable.musical_notes)
                    Item(1, "Albums", R.drawable.disc)
                    Item(2, "Artists", R.drawable.person)
                    Item(3, "Videos", R.drawable.film)
                    Item(4, "Playlists", R.drawable.playlist)
                    Item(5, "Featured", R.drawable.playlist)
                }
            ) { tabIndex ->
                saveableStateHolder.SaveableStateProvider(tabIndex) {
                    when (tabIndex) {
                        0 -> {
                            val binder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailSizeDp = Dimensions.thumbnails.song
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/songs",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        YouTube.searchPage(
                                            body = SearchBody(query = query, params = YouTube.SearchFilter.Song.value),
                                            fromMusicShelfRendererContent = YouTube.SongItem.Companion::from
                                        )
                                    } else {
                                        YouTube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = YouTube.SongItem.Companion::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { song ->
                                    SongItem(
                                        song = song,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        NonQueuedMediaItemMenu(
                                        onDismiss = menuState::hide,
                                        mediaItem = song.asMediaItem,
                                    )
                                                    }
                                                },
                                                onClick = {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlay(song.asMediaItem)
                                                    binder?.setupRadio(song.info?.endpoint)
                                                }
                                            )
                                    )
                                },
                                itemPlaceholderContent = {
                                    SongItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        1 -> {
                            val thumbnailSizeDp = 108.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/albums",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        YouTube.searchPage(
                                            body = SearchBody(query = query, params = YouTube.SearchFilter.Album.value),
                                            fromMusicShelfRendererContent = YouTube.AlbumItem::from
                                        )
                                    } else {
                                        YouTube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = YouTube.AlbumItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { album ->
                                    AlbumItem(
                                        album = album,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = { albumRoute(album.key) })
                                    )

                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        2 -> {
                            val thumbnailSizeDp = 64.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/artists",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        YouTube.searchPage(
                                            body = SearchBody(query = query, params = YouTube.SearchFilter.Artist.value),
                                            fromMusicShelfRendererContent = YouTube.ArtistItem::from
                                        )
                                    } else {
                                        YouTube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = YouTube.ArtistItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { artist ->
                                    ArtistItem(
                                        artist = artist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = { artistRoute(artist.key) })
                                    )
                                },
                                itemPlaceholderContent = {
                                    ArtistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        3 -> {
                            val binder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailHeightDp = 72.dp
                            val thumbnailWidthDp = 128.dp

                            ItemsPage(
                                tag = "searchResults/$query/videos",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        YouTube.searchPage(
                                            body = SearchBody(query = query, params = YouTube.SearchFilter.Video.value),
                                            fromMusicShelfRendererContent = YouTube.VideoItem::from
                                        )
                                    } else {
                                        YouTube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = YouTube.VideoItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { video ->
                                    VideoItem(
                                        video = video,
                                        thumbnailWidthDp = thumbnailWidthDp,
                                        thumbnailHeightDp = thumbnailHeightDp,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onLongClick = {
                                                    menuState.display {
                                                        NonQueuedMediaItemMenu(
                                                            mediaItem = video.asMediaItem,
                                                            onDismiss = menuState::hide
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlay(video.asMediaItem)
                                                    binder?.setupRadio(video.info?.endpoint)
                                                }
                                            )
                                    )
                                },
                                itemPlaceholderContent = {
                                    VideoItemPlaceholder(
                                        thumbnailHeightDp = thumbnailHeightDp,
                                        thumbnailWidthDp = thumbnailWidthDp
                                    )
                                }
                            )
                        }

                        4, 5 -> {
                            val thumbnailSizeDp = 108.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/${if (tabIndex == 4) "playlists" else "featured"}",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        val filter = if (tabIndex == 4) {
                                            YouTube.SearchFilter.CommunityPlaylist
                                        } else {
                                            YouTube.SearchFilter.FeaturedPlaylist
                                        }

                                        YouTube.searchPage(
                                            body = SearchBody(query = query, params = filter.value),
                                            fromMusicShelfRendererContent = YouTube.PlaylistItem::from
                                        )
                                    } else {
                                        YouTube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = YouTube.PlaylistItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = { playlistRoute(playlist.key) })
                                    )
                                },
                                itemPlaceholderContent = {
                                    PlaylistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
