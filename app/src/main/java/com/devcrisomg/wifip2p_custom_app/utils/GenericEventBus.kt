package com.devcrisomg.wifip2p_custom_app.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class GenericEventBus<T> {
    private val listeners = mutableListOf<(T) -> Unit>()

    fun publish(event: T) {
        listeners.forEach { it(event) } // Llama a todos los oyentes con el evento
    }

    fun subscribe(listener: (T) -> Unit) {
        listeners.add(listener)
    }
}

open class GenericStateFlowEventBus<T> {
    private val _stateFlow = MutableStateFlow<T?>(null)
    val stateFlow: StateFlow<T?> get() = _stateFlow

    fun publish(event: T) {
        _stateFlow.value = event
    }
}
