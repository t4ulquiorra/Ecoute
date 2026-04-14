package com.ecoute.music.ui.screens.playlist

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.valentinilk.shimmer.shimmer
import com.ecoute.compose.persist.persist
import com.ecoute.innertube.Innertube
import com.ecoute.innertube.models.bodies.BrowseBody
import com.ecoute.innertube.requests.playlistPage
import com.ecoute.music.Database
import com.ecoute.music.LocalPlayerAwareWindowInsets
import com.ecoute.music.LocalPlayerServiceBinder
import com.ecoute.music.R
import com.ecoute.music.models.Playlist
import com.ecoute.music.models.SongPlaylistMap
import com.ecoute.music.query
import com.ecoute.music.transaction
import com.ecoute.music.ui.components.LocalMenuState
import com.ecoute.music.ui.components.ShimmerHost
import com.ecoute.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.ecoute.music.ui.components.themed.Header
import com.ecoute.music.ui.components.themed.HeaderIconButton
import com.ecoute.music.ui.components.themed.HeaderPlaceholder
import com.ecoute.music.ui.components.themed.LayoutWithAdaptiveThumbnail
import com.ecoute.music.ui.components.themed.NonQueuedMediaItemMenu
import com.ecoute.music.ui.components.themed.SecondaryTextButton
import com.ecoute.music.ui.components.themed.TextFieldDialog
import com.ecoute.music.ui.components.themed.adaptiveThumbnailContent
import com.ecoute.music.ui.items.SongItem
import com.ecoute.music.ui.items.SongItemPlaceholder
import com.ecoute.music.ui.styling.Dimensions
import com.ecoute.music.ui.styling.LocalAppearance
import com.ecoute.music.ui.styling.px
import com.ecoute.music.utils.asMediaItem
import com.ecoute.music.utils.completed
import com.ecoute.music.utils.enqueue
import com.ecoute.music.utils.forcePlayAtIndex
import com.ecoute.music.utils.forcePlayFromBeginning
import com.ecoute.music.utils.isLandscape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun PlaylistSongList(
    browseId: String,
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    var playlistPage by persist<Innertube.PlaylistOrAlbumPage?>("playlist/$browseId/playlistPage")

    LaunchedEffect(Unit) {
        if (playlistPage != null && playlistPage?.songsPage?.continuation == null) return@LaunchedEffect

        playlistPage = withContext(Dispatchers.IO) {
            Innertube.playlistPage(BrowseBody(browseId = browseId))?.completed()?.getOrNull()
        }
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px

    var isImportingPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    if (isImportingPlaylist) {
        TextFieldDialog(
            hintText = "Enter the playlist name",
            initialTextInput = playlistPage?.title ?: "",
            onDismiss = { isImportingPlaylist = false },
            onDone = { text ->
                query {
                    transaction {
                        val playlistId = Database.insert(Playlist(name = text, browseId = browseId))

                        playlistPage?.songsPage?.items
                            ?.map(Innertube.SongItem::asMediaItem)
                            ?.onEach(Database::insert)
                            ?.mapIndexed { index, mediaItem ->
                                SongPlaylistMap(
                                    songId = mediaItem.mediaId,
                                    playlistId = playlistId,
                                    position = index
                                )
                            }?.let(Database::insertSongPlaylistMaps)
                    }
                }
            }
        )
    }

    val headerContent: @Composable () -> Unit = {
        if (playlistPage == null) {
            HeaderPlaceholder(
                modifier = Modifier
                    .shimmer()
            )
        } else {
            Header(title = playlistPage?.title ?: "Unknown") {
                SecondaryTextButton(
                    text = "Enqueue",
                    enabled = playlistPage?.songsPage?.items?.isNotEmpty() == true,
                    onClick = {
                        playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)?.let { mediaItems ->
                            binder?.player?.enqueue(mediaItems)
                        }
                    }
                )

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                )

                HeaderIconButton(
                    icon = R.drawable.add,
                    color = colorPalette.text,
                    onClick = { isImportingPlaylist = true }
                )

                HeaderIconButton(
                    icon = R.drawable.share_social,
                    color = colorPalette.text,
                    onClick = {
                        (playlistPage?.url ?: "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}").let { url ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }

                            context.startActivity(Intent.createChooser(sendIntent, null))
                        }
                    }
                )
            }
        }
    }

    val thumbnailContent = adaptiveThumbnailContent(playlistPage == null, playlistPage?.thumbnail?.url)

    val lazyListState = rememberLazyListState()

    LayoutWithAdaptiveThumbnail(thumbnailContent = thumbnailContent) {
        Box {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        headerContent()
                        if (!isLandscape) thumbnailContent()
                    }
                }

                itemsIndexed(items = playlistPage?.songsPage?.items ?: emptyList()) { index, song ->
                    SongItem(
                        song = song,
                        thumbnailSizePx = songThumbnailSizePx,
                        thumbnailSizeDp = songThumbnailSizeDp,
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
                                    playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)?.let { mediaItems ->
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(mediaItems, index)
                                    }
                                }
                            )
                    )
                }

                if (playlistPage == null) {
                    item(key = "loading") {
                        ShimmerHost(
                            modifier = Modifier
                                .fillParentMaxSize()
                        ) {
                            repeat(4) {
                                SongItemPlaceholder(thumbnailSizeDp = songThumbnailSizeDp)
                            }
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = R.drawable.shuffle,
                onClick = {
                    playlistPage?.songsPage?.items?.let { songs ->
                        if (songs.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(
                                songs.shuffled().map(Innertube.SongItem::asMediaItem)
                            )
                        }
                    }
                }
            )
        }
    }
}
