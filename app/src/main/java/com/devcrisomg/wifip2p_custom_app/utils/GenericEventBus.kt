package com.devcrisomg.wifip2p_custom_app.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

open class GenericEventBus<T> {
    private val listeners = mutableListOf<(T) -> Unit>()

    fun publish(event: T) {
        listeners.forEach { it(event) } // Llama a todos los oyentes con el evento
    }

    fun subscribe(listener: (T) -> Unit) {
        listeners.add(listener)
    }
}

open class _GenericStateFlowEventBus<T> {
    private val _stateFlow = MutableStateFlow<T?>(null)
    val stateFlow: StateFlow<T?> get() = _stateFlow

    fun publish(event: T) {
        _stateFlow.value = event
    }
}

open class __GenericStateFlowEventBus<T>(
    private val bufferSize: Int = 64
) {
    private val _stateFlow = MutableStateFlow<T?>(null)
    val stateFlow: StateFlow<T?> = _stateFlow.asStateFlow()

    @Synchronized
    fun publish(event: T) {
        _stateFlow.value = event
    }

    fun clear() {
        _stateFlow.value = null
    }
}

open class GenericSharedFlowEventBus<T> {
    private val _events = MutableSharedFlow<T>(
        replay = 0,  // No retiene eventos
        extraBufferCapacity = 64  // Buffer para backpressure
    )
    val events = _events.asSharedFlow()

    /**
     * Intenta publicar un evento sin suspender.
     * @return true si el evento fue emitido exitosamente, false si el buffer está lleno
     */
    fun publish(event: T): Boolean = _events.tryEmit(event)

    /**
     * Versión suspendida para cuando necesitas garantizar la entrega del evento.
     * Suspende la corrutina si el buffer está lleno hasta que haya espacio.
     */
    suspend fun publishSafeAsync(event: T) = _events.emit(event)
}