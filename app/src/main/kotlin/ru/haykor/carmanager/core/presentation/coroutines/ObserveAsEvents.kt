package ru.haykor.carmanager.core.presentation.coroutines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onEvent: suspend (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(flow, lifecycleOwner, key1) {
        lifecycleOwner.repeatOnLifecycle(lifecycleState) {
            flow.collect { event ->
                currentOnEvent(event)
            }
        }
    }
}
