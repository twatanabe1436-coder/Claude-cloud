package io.github.twatanabe1436.hanaso.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.twatanabe1436.hanaso.HanasoApp
import io.github.twatanabe1436.hanaso.Screen
import io.github.twatanabe1436.hanaso.core.EngineMode
import io.github.twatanabe1436.hanaso.core.Scenario
import io.github.twatanabe1436.hanaso.ui.talk.TalkScreen

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Screen.Home, "ホーム", Icons.Filled.Home),
    Tab(Screen.Scenarios, "会話", AppIcons.Forum),
    Tab(Screen.Phrases, "フレーズ帳", AppIcons.Book),
    Tab(Screen.Settings, "設定", Icons.Filled.Settings),
)

@Composable
fun HanasoRoot(app: HanasoApp) {
    val nav = app.nav
    val screen = nav.current
    val settings by app.store.settings.collectAsStateWithLifecycle()
    val apiKey by app.store.apiKey.collectAsStateWithLifecycle()
    val sessions by app.store.sessions.collectAsStateWithLifecycle()
    val phrases by app.store.phrases.collectAsStateWithLifecycle()
    var intro by remember { mutableStateOf<Scenario?>(null) }

    CompositionLocalProvider(LocalApp provides app) {
        BackHandler(enabled = nav.canGoBack && screen !is Screen.Talk) { nav.back() }
        // 会話画面から離れたら、会話 (マイク・読み上げ・通信) を片付ける
        LaunchedEffect(screen) { if (screen !is Screen.Talk) app.closeTalk() }

        when (screen) {
            is Screen.Talk -> {
                val session = app.talk
                if (session == null) {
                    LaunchedEffect(Unit) { nav.back() }
                } else {
                    FullScreen {
                        TalkScreen(
                            session = session,
                            onQuit = {
                                app.closeTalk()
                                nav.back()
                            },
                            onFinish = { app.finishTalk() },
                        )
                    }
                }
            }

            Screen.Summary -> {
                val finished = app.finished
                if (finished == null) {
                    LaunchedEffect(Unit) { nav.back() }
                } else {
                    FullScreen {
                        SummaryScreen(
                            result = finished,
                            onAgain = {
                                nav.back()
                                app.startTalk(finished.scenario, finished.level)
                            },
                            onHome = { nav.tab(Screen.Home) },
                        )
                    }
                }
            }

            else -> Scaffold(
                bottomBar = {
                    NavigationBar {
                        TABS.forEach { tab ->
                            NavigationBarItem(
                                selected = screen == tab.screen,
                                onClick = { nav.tab(tab.screen) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (screen) {
                        Screen.Scenarios -> ScenariosScreen(onOpen = { intro = it })
                        Screen.Phrases -> PhrasesScreen(onStartTalk = { nav.tab(Screen.Scenarios) })
                        Screen.Settings -> SettingsScreen()
                        else -> HomeScreen(
                            level = settings.level,
                            mode = app.modeFor(apiKey, settings),
                            totals = remember(sessions, phrases) { app.store.totals() },
                            recent = sessions,
                            onOpen = { intro = it },
                            onOpenSettings = { nav.tab(Screen.Settings) },
                            onSeeAll = { nav.tab(Screen.Scenarios) },
                        )
                    }
                }
            }
        }

        intro?.let { scenario ->
            ScenarioIntroSheet(
                scenario = scenario,
                initialLevel = settings.level,
                mode = app.modeFor(apiKey, settings),
                onPlay = { text ->
                    app.speaker.stop()
                    app.speaker.say(text)
                },
                onDismiss = { intro = null },
                onStart = { level ->
                    intro = null
                    // 台本モードのロールプレイは台本のレベルで始まるので、自分のレベル設定は変えない
                    val scriptRolePlay = app.modeFor(apiKey, settings) == EngineMode.SCRIPT && !scenario.isFreeTalk
                    if (!scriptRolePlay && level != settings.level) app.store.updateSettings { it.copy(level = level) }
                    app.startTalk(scenario, level)
                },
            )
        }
    }
}

/** 会話・振り返り: タブバーなしの全画面 (キーボードの分も避ける) */
@Composable
private fun FullScreen(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) { content() }
}
