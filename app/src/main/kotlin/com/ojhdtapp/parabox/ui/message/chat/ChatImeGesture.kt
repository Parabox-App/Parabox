package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

internal enum class ChatImeFocusAction { Focus, Clear, OpenFallback }

internal class ChatImeGesture(private val fallbackDistance: Float) {
    private enum class Direction { Open, Close }

    private var direction: Direction? = null
    private var openingDistance = 0f
    private var sawOpeningInsets = false
    private var editorLostFocus = false

    val isActive: Boolean get() = direction != null
    val isOpening: Boolean get() = direction == Direction.Open

    fun drag(deltaY: Float, atLatest: Boolean, imeVisible: Boolean, editorFocused: Boolean) {
        if (!isActive) {
            direction = when {
                deltaY < 0f && atLatest && !imeVisible -> Direction.Open
                deltaY > 0f && imeVisible && editorFocused -> Direction.Close
                else -> null
            }
        }
        if (isOpening) openingDistance = (openingDistance - deltaY).coerceAtLeast(0f)
    }

    fun observeOpeningInsets(occupiesBottom: Boolean) {
        if (isOpening && occupiesBottom) sawOpeningInsets = true
    }

    fun observeEditorFocus(focused: Boolean) {
        if (direction == Direction.Close && !focused) editorLostFocus = true
    }

    fun finish(imeVisible: Boolean): ChatImeFocusAction? = when (direction) {
        Direction.Open -> when {
            imeVisible && sawOpeningInsets -> ChatImeFocusAction.Focus
            !imeVisible && !sawOpeningInsets && openingDistance >= fallbackDistance ->
                ChatImeFocusAction.OpenFallback
            else -> null
        }
        Direction.Close -> ChatImeFocusAction.Clear.takeIf { !imeVisible && !editorLostFocus }
        null -> null
    }

    fun cancel() {
        direction = null
        openingDistance = 0f
        sawOpeningInsets = false
        editorLostFocus = false
    }
}

private data class ImePosition(val shown: Boolean, val bottom: Int, val target: Int) {
    val fullyHidden: Boolean get() = !shown && bottom == 0 && target == 0
    val fullyShown: Boolean get() = shown && bottom > 0 && bottom == target
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberChatImeScrollModifier(
    enabled: Boolean,
    chatId: Long?,
    listState: LazyListState,
    editorRequester: FocusRequester,
    editorFocused: Boolean,
): Modifier {
    if (!enabled) return Modifier

    val ime = WindowInsets.ime
    val target = WindowInsets.imeAnimationTarget
    val imeVisible = rememberUpdatedState(WindowInsets.isImeVisible)
    val focused = rememberUpdatedState(editorFocused)
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val observer = remember(chatId, listState, editorRequester, ime, target, density, focusManager, keyboard) {
        val gesture = ChatImeGesture(with(density) { 48.dp.toPx() })
        object : NestedScrollConnection {
            private var settleJob: Job? = null

            fun position() = ImePosition(imeVisible.value, ime.getBottom(density), target.getBottom(density))

            fun recordInsets(bottom: Int) = gesture.observeOpeningInsets(bottom > 0)

            fun recordFocus(editorHasFocus: Boolean) = gesture.observeEditorFocus(editorHasFocus)

            fun cancel() {
                settleJob?.cancel()
                settleJob = null
                gesture.cancel()
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || abs(available.y) <= abs(available.x)) {
                    return Offset.Zero
                }
                if (settleJob != null) cancel()
                val position = position()
                gesture.drag(
                    deltaY = available.y,
                    atLatest = !listState.canScrollBackward,
                    imeVisible = position.shown || position.bottom > 0,
                    editorFocused = focused.value,
                )
                recordInsets(position.bottom)
                return Offset.Zero
            }

            // As the outer connection, this runs after imeNestedScroll settles its fling.
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (gesture.isActive && settleJob == null) {
                    settleJob = scope.launch {
                        val opening = gesture.isOpening
                        val ended = withTimeoutOrNull(250L) {
                            snapshotFlow { position() }.first {
                                recordInsets(it.bottom)
                                if (opening) it.fullyShown else it.fullyHidden
                            }
                        } ?: position()
                        recordInsets(ended.bottom)
                        val action = when {
                            ended.fullyShown -> gesture.finish(imeVisible = true)
                            ended.fullyHidden -> gesture.finish(imeVisible = false)
                            else -> null
                        }
                        gesture.cancel()
                        settleJob = null
                        when (action) {
                            ChatImeFocusAction.Focus -> if (!focused.value) editorRequester.requestFocus() else Unit
                            ChatImeFocusAction.OpenFallback -> {
                                if (focused.value) keyboard?.show()
                                else editorRequester.requestFocus()
                            }
                            ChatImeFocusAction.Clear -> if (focused.value) focusManager.clearFocus()
                            null -> Unit
                        }
                    }
                }
                return Velocity.Zero
            }
        }
    }
    LaunchedEffect(observer) {
        snapshotFlow { observer.position().bottom }.collect(observer::recordInsets)
    }
    LaunchedEffect(observer) {
        snapshotFlow { focused.value }.collect(observer::recordFocus)
    }
    LaunchedEffect(observer, listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Cancel) observer.cancel()
        }
    }
    DisposableEffect(observer) {
        onDispose { observer.cancel() }
    }
    return Modifier.nestedScroll(observer).imeNestedScroll()
}
