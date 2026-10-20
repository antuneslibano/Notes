package com.blocodenotas.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.blocodenotas.app.data.ThemeMode
import com.blocodenotas.app.ui.AppRoot
import com.blocodenotas.app.ui.LockScreen
import com.blocodenotas.app.ui.Navigator
import com.blocodenotas.app.ui.NotesTheme

class MainActivity : FragmentActivity() {

    private var locked by mutableStateOf(false)
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        locked = Graph.settings.pinEnabled

        setContent {
            val settings = Graph.settings
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
            val navigator = remember { Navigator() }
            NotesTheme(dark = dark) {
                if (locked && settings.pinEnabled) {
                    LockScreen(resumeTick = resumeTick, onUnlock = { locked = false })
                } else {
                    AppRoot(navigator)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Graph.skipNextLock = false
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onStop() {
        super.onStop()
        Graph.repo.flush()
        if (Graph.settings.pinEnabled && !Graph.skipNextLock && !isChangingConfigurations) {
            locked = true
        }
    }
}
