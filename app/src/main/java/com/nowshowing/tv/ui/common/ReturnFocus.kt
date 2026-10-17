package com.nowshowing.tv.ui.common

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

internal class ReturnFocusState(val selected: MutableState<String>) {
    var pending: String? = selected.value.takeIf { it.isNotEmpty() }
}
internal val LocalReturnFocus = staticCompositionLocalOf<ReturnFocusState?> { null }

/** Saved per Navigation entry; each return restores only one selected card. */
@Composable
fun ReturnFocusScope(content: @Composable () -> Unit) {
    val selected = rememberSaveable { mutableStateOf("") }
    val state = remember { ReturnFocusState(selected) }
    CompositionLocalProvider(LocalReturnFocus provides state, content = content)
}

@Composable
internal fun rememberReturnFocusRequester(key: String): Pair<FocusRequester, () -> Unit> {
    val state = LocalReturnFocus.current
    val requester = remember { FocusRequester() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(key, state) {
        if (state?.pending == key) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (state.pending == key) {
                    withFrameNanos { }
                    requester.requestFocus()
                    state.pending = null
                }
            }
        }
    }
    return requester to { state?.selected?.value = key }
}
