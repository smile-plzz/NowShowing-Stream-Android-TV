package com.nowshowing.tv.ui.common

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

internal class ReturnFocusState(val selected: MutableState<String>) {
    var pending: String? = selected.value.takeIf { it.isNotEmpty() }
}
internal val LocalReturnFocus = staticCompositionLocalOf<ReturnFocusState?> { null }

/** Saved per Navigation entry; each return restores only one selected card. */
@Composable
fun ReturnFocusScope(initialKey: String = "", content: @Composable () -> Unit) {
    val selected = rememberSaveable { mutableStateOf(initialKey) }
    val state = remember { ReturnFocusState(selected) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) state.pending = selected.value.takeIf { it.isNotEmpty() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    CompositionLocalProvider(LocalReturnFocus provides state, content = content)
}

@Composable
internal fun rememberReturnFocusRequester(key: String): Pair<FocusRequester, () -> Unit> {
    val state = LocalReturnFocus.current
    val requester = remember { FocusRequester() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(key, state) {
        if (state != null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (state.pending == key) {
                    withFrameNanos { }
                    if (runCatching { requester.requestFocus(); true }.getOrDefault(false)) state.pending = null
                }
            }
        }
    }
    return requester to { state?.selected?.value = key }
}
