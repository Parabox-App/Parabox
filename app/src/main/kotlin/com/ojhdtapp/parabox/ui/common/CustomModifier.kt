package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager

@OptIn(ExperimentalLayoutApi::class)
fun Modifier.clearFocusOnKeyboardDismiss(onClear: (() -> Unit)? = null): Modifier = composed {
    var isFocused by remember {
        mutableStateOf(false)
    }
    var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }
    if (isFocused) {
        val imeIsVisible = WindowInsets.isImeVisible
        val focusManager = LocalFocusManager.current
        LaunchedEffect(imeIsVisible) {
            if (imeIsVisible) {
                keyboardAppearedSinceLastFocused = true
            } else if (keyboardAppearedSinceLastFocused) {
                focusManager.clearFocus()
                onClear?.run {
                    invoke()
                }
            }
        }
    }
    onFocusEvent {
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                keyboardAppearedSinceLastFocused = false
            }
        }
    }
}

/**
 * Focus anchor for the root of a pane hosted by Material 3 Adaptive `ThreePaneScaffold`
 * (via Navigation 3 `ListDetailSceneStrategy`).
 *
 * `ThreePaneScaffold` calls `FocusRequester.requestFocus()` on the current destination pane
 * whenever the scaffold value changes (e.g. portrait -> landscape switching 1 pane -> 2 panes).
 * That request is forwarded to the first focusable descendant. In touch mode, buttons / clickable
 * items are not focusable, so the focus would land on the first text field — e.g.
 * `SearchBarDefaults.InputField`, whose `onFocusChanged` calls `onExpandedChange(true)` and
 * auto-expands the search bar, or the chat input field which pops up the IME.
 *
 * Making the pane root itself focusable lets it absorb that programmatic focus without side
 * effects, while direct taps on text fields still focus them normally.
 */
fun Modifier.paneFocusAnchor(): Modifier = this.focusable()
