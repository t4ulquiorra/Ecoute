package com.ecoute.music.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import com.ecoute.compose.persist.PersistMapCleanup
import com.ecoute.compose.routing.RouteHandler
import com.ecoute.compose.routing.defaultStacking
import com.ecoute.compose.routing.defaultStill
import com.ecoute.compose.routing.defaultUnstacking
import com.ecoute.compose.routing.isStacking
import com.ecoute.compose.routing.isUnknown
import com.ecoute.compose.routing.isUnstacking
import com.ecoute.music.Database
import com.ecoute.music.R
import com.ecoute.music.models.SearchQuery
import com.ecoute.music.query
import com.ecoute.music.ui.components.themed.Scaffold
import com.ecoute.music.ui.screens.albumRoute
import com.ecoute.music.ui.screens.artistRoute
import com.ecoute.music.ui.screens.builtInPlaylistRoute
import com.ecoute.music.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import com.ecoute.music.ui.screens.globalRoutes
import com.ecoute.music.ui.screens.localPlaylistRoute
import com.ecoute.music.ui.screens.localplaylist.LocalPlaylistScreen
import com.ecoute.music.ui.screens.playlistRoute
import com.ecoute.music.ui.screens.search.SearchScreen
import com.ecoute.music.ui.screens.searchResultRoute
import com.ecoute.music.ui.screens.searchRoute
import com.ecoute.music.ui.screens.searchresult.SearchResultScreen
import com.ecoute.music.ui.screens.settings.SettingsScreen
import com.ecoute.music.ui.screens.settingsRoute
import com.ecoute.music.utils.homeScreenTabIndexKey
import com.ecoute.music.utils.pauseSearchHistoryKey
import com.ecoute.music.utils.preferences
import com.ecoute.music.utils.rememberPreference

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeScreen(onPlaylistUrl: (String) -> Unit) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup("home/")

    RouteHandler(
        listenToGlobalEmitter = true,
        transitionSpec = {
            when {
                isStacking -> defaultStacking
                isUnstacking -> defaultUnstacking
                isUnknown -> when {
                    initialState.route == searchRoute && targetState.route == searchResultRoute -> defaultStacking
                    initialState.route == searchResultRoute && targetState.route == searchRoute -> defaultUnstacking
                    else -> defaultStill
                }

                else -> defaultStill
            }
        }
    ) {
        globalRoutes()

        settingsRoute {
            SettingsScreen()
        }

        localPlaylistRoute { playlistId ->
            LocalPlaylistScreen(
                playlistId = playlistId ?: error("playlistId cannot be null")
            )
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(
                builtInPlaylist = builtInPlaylist
            )
        }

        searchResultRoute { query ->
            SearchResultScreen(
                query = query,
                onSearchAgain = {
                    searchRoute(query)
                }
            )
        }

        searchRoute { initialTextInput ->
            val context = LocalContext.current

            SearchScreen(
                initialTextInput = initialTextInput,
                onSearch = { query ->
                    pop()
                    searchResultRoute(query)

                    if (!context.preferences.getBoolean(pauseSearchHistoryKey, false)) {
                        query {
                            Database.insert(SearchQuery(query = query))
                        }
                    }
                },
                onViewPlaylist = onPlaylistUrl
            )
        }

        host {
            val (tabIndex, onTabChanged) = rememberPreference(
                homeScreenTabIndexKey,
                defaultValue = 0
            )

            Scaffold(
                topIconButtonId = R.drawable.equalizer,
                onTopIconButtonClick = { settingsRoute() },
                tabIndex = tabIndex,
                onTabChanged = onTabChanged,
                tabColumnContent = { Item ->
                    Item(0, "Quick picks", R.drawable.sparkles)
                    Item(1, "Songs", R.drawable.musical_notes)
                    Item(2, "Playlists", R.drawable.playlist)
                    Item(3, "Artists", R.drawable.person)
                    Item(4, "Albums", R.drawable.disc)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> QuickPicks(
                            onAlbumClick = { albumRoute(it) },
                            onArtistClick = { artistRoute(it) },
                            onPlaylistClick = { playlistRoute(it) },
                            onSearchClick = { searchRoute("") }
                        )

                        1 -> HomeSongs(
                            onSearchClick = { searchRoute("") }
                        )

                        2 -> HomePlaylists(
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )

                        3 -> HomeArtistList(
                            onArtistClick = { artistRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )

                        4 -> HomeAlbums(
                            onAlbumClick = { albumRoute(it.id) },
                            onSearchClick = { searchRoute("") }
                        )
                    }
                }
            }
        }
    }
}
