package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal class ChartEntry(val id: String, val title: String, val content: @Composable () -> Unit)

class FullscreenChartController internal constructor(private val active: MutableState<String?>) {
    internal val entries = mutableStateMapOf<String, ChartEntry>()
    val activeId: String? get() = active.value
    internal var returnFocusId by mutableStateOf<String?>(null)
        private set
    fun open(id: String) { if (entries.containsKey(id)) { returnFocusId = null; active.value = id } }
    fun close() { returnFocusId = active.value; active.value = null }
    internal fun focusRestored(id: String) { if (returnFocusId == id) returnFocusId = null }
    internal fun register(entry: ChartEntry) { entries[entry.id] = entry }
    internal fun unregister(entry: ChartEntry) {
        if (entries[entry.id] === entry) {
            entries.remove(entry.id)
        }
    }
}

val LocalFullscreenCharts = staticCompositionLocalOf<FullscreenChartController?> { null }
val LocalChartFullscreen = staticCompositionLocalOf { false }
private val LocalChartId = staticCompositionLocalOf<String?> { null }

/** One overlay in the existing window. Moving content keeps its state and subscriptions alive. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenChartHost(screenKey: String = "charts", content: @Composable () -> Unit) {
    val active = rememberSaveable(screenKey) { mutableStateOf<String?>(null) }
    val controller = remember(active, screenKey) { FullscreenChartController(active) }
    CompositionLocalProvider(LocalFullscreenCharts provides controller) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().then(if (controller.activeId != null) Modifier.clearAndSetSemantics {} else Modifier)) { content() }
            if (controller.activeId != null) {
                val activeId = controller.activeId
                val entry = controller.entries[activeId]
                val closeFocus = remember { FocusRequester() }
                Surface(Modifier.fillMaxSize().testTag("fullscreen_chart").semantics { paneTitle = "Fullscreen chart" }
                    .onPreviewKeyEvent {
                        if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) { controller.close(); true } else false
                    }, color = MaterialTheme.colorScheme.background) {
                    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, topBar = {
                        TopAppBar(title = { Text(entry?.title ?: "Chart", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            navigationIcon = {
                                IconButton(onClick = controller::close, modifier = Modifier.focusRequester(closeFocus).testTag("close_fullscreen_chart")) {
                                    Icon(Icons.Default.Close, "Close fullscreen chart")
                                }
                            })
                    }) { padding ->
                        CompositionLocalProvider(LocalChartFullscreen provides true) {
                            Box(Modifier.fillMaxSize().padding(padding)) {
                                if (entry != null) entry.content() else LoadingContent("Opening chart…")
                            }
                        }
                    }
                }
                LaunchedEffect(controller.activeId) { closeFocus.requestFocus() }
                LaunchedEffect(activeId, entry) {
                    if (entry == null) {
                        // Lazy items register during layout. Allow restoration to finish before
                        // dismissing a chart that was removed; disposal must not clear saved state.
                        withFrameNanos { }
                        if (controller.entries[activeId] == null) controller.close()
                    }
                }
                // Registered after the underlying navigation handlers so Back closes the overlay first.
                BackHandler { controller.close() }
            }
        }
    }
}

@Composable
fun ChartFullscreenButton(chartId: String? = LocalChartId.current) {
    val controller = LocalFullscreenCharts.current
    val fullscreen = LocalChartFullscreen.current
    if (controller == null || chartId == null || fullscreen) return
    val focus = remember(chartId) { FocusRequester() }
    IconButton(onClick = { controller.open(chartId) }, modifier = Modifier.focusRequester(focus).testTag("expand_$chartId")) {
        Icon(Icons.Default.OpenInFull, "View chart fullscreen", tint = MaterialTheme.colorScheme.primary)
    }
    LaunchedEffect(controller.returnFocusId) {
        if (controller.returnFocusId == chartId) { focus.requestFocus(); controller.focusRestored(chartId) }
    }
}

/** The inline slot keeps its height, so opening/closing never shifts the underlying lazy list. */
@Composable
fun FullscreenableChart(id: String, title: String, modifier: Modifier = Modifier,
                       scrollInFullscreen: Boolean = true, showButton: Boolean = true, content: @Composable () -> Unit) {
    val controller = LocalFullscreenCharts.current
    if (controller == null) { content(); return }
    val currentContent by rememberUpdatedState(content)
    // Stable saveable scope also restores values if Android recreates us while already expanded.
    val savedContent = rememberSaveableStateHolder()
    val body = remember(id) { movableContentOf { savedContent.SaveableStateProvider(id) { currentContent() } } }
    val entry = remember(id, scrollInFullscreen) {
        ChartEntry(id, title) {
            CompositionLocalProvider(LocalChartId provides id) {
                if (scrollInFullscreen) LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    item(id) { body() }
                } else body()
            }
        }
    }
    DisposableEffect(controller, entry) { controller.register(entry); onDispose { controller.unregister(entry) } }
    var heightDp by rememberSaveable(id) { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    Box(modifier.onSizeChanged { if (controller.activeId != id) heightDp = with(density) { it.height.toDp().value } }) {
        if (controller.activeId == id) Box(Modifier.fillMaxWidth().height(heightDp.dp))
        else CompositionLocalProvider(LocalChartId provides id) {
            Column {
                if (showButton && !LocalChartFullscreen.current) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ChartFullscreenButton() }
                Box(Modifier.fillMaxWidth()) { body() }
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun chartPlotHeight(compact: Dp): Dp {
    if (!LocalChartFullscreen.current) return compact
    val size = LocalWindowInfo.current.containerSize
    val height = with(LocalDensity.current) { size.height.toDp() }
    return (height * 0.45f).coerceIn(240.dp, 480.dp)
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun chartDonutSize(): Dp {
    if (!LocalChartFullscreen.current) return 240.dp
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) {
        minOf(size.width.toDp() - 64.dp, size.height.toDp() - 128.dp).coerceIn(240.dp, 420.dp)
    }
}
