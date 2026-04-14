package com.ecoute.music.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.ecoute.compose.persist.PersistMapCleanup
import com.ecoute.compose.routing.RouteHandler
import com.ecoute.music.R
import com.ecoute.music.ui.components.themed.Scaffold
import com.ecoute.music.ui.screens.globalRoutes

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun PlaylistScreen(browseId: String) {
    val saveableStateHolder = rememberSaveableStateHolder()
    PersistMapCleanup(tagPrefix = "playlist/$browseId")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChanged = { },
                tabColumnContent = { Item ->
                    Item(0, "Songs", R.drawable.musical_notes)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> PlaylistSongList(browseId = browseId)
                    }
                }
            }
        }
    }
}
