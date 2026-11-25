package com.ojhdtapp.parabox.ui.message.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatImeGestureTest {
    @Test
    fun expandedImeRequestsEditorFocusAfterRelease() {
        val gesture = ChatImeGesture(48f)
        gesture.drag(-20f, atLatest = true, imeVisible = false, editorFocused = false)
        gesture.observeOpeningInsets(true)

        assertEquals(ChatImeFocusAction.Focus, gesture.finish(imeVisible = true))
    }

    @Test
    fun dismissedImeReleasesOnlyItsEditorFocus() {
        val gesture = ChatImeGesture(48f)
        gesture.drag(20f, atLatest = false, imeVisible = true, editorFocused = true)

        assertEquals(ChatImeFocusAction.Clear, gesture.finish(imeVisible = false))
        gesture.cancel()
        gesture.drag(20f, atLatest = false, imeVisible = true, editorFocused = false)
        assertNull(gesture.finish(imeVisible = false))
    }

    @Test
    fun nativeSnapBackDoesNotFallBackToOpening() {
        val gesture = ChatImeGesture(48f)
        gesture.drag(-70f, atLatest = true, imeVisible = false, editorFocused = false)
        gesture.observeOpeningInsets(true)

        assertNull(gesture.finish(imeVisible = false))
        gesture.cancel()
        gesture.drag(60f, atLatest = false, imeVisible = true, editorFocused = true)
        assertNull(gesture.finish(imeVisible = true))
    }

    @Test
    fun fallbackRequiresEnoughNetDistanceAtLatestMessage() {
        val gesture = ChatImeGesture(48f)
        gesture.drag(-90f, atLatest = false, imeVisible = false, editorFocused = false)
        assertNull(gesture.finish(imeVisible = false))

        gesture.drag(-55f, atLatest = true, imeVisible = false, editorFocused = false)
        gesture.drag(20f, atLatest = true, imeVisible = false, editorFocused = false)
        assertNull(gesture.finish(imeVisible = false))
        gesture.drag(-13f, atLatest = true, imeVisible = false, editorFocused = false)
        assertEquals(ChatImeFocusAction.OpenFallback, gesture.finish(imeVisible = false))
    }

    @Test
    fun cancellationDiscardsPendingFocusChange() {
        val gesture = ChatImeGesture(48f)
        gesture.drag(-70f, atLatest = true, imeVisible = false, editorFocused = false)
        gesture.cancel()
        assertNull(gesture.finish(imeVisible = true))
        assertNull(gesture.finish(imeVisible = false))
    }
}
