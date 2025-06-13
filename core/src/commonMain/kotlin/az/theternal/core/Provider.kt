package az.theternal.core

import kotlinx.coroutines.flow.StateFlow


data class Provider<Intent : ViewIntent, State : ViewState>(
    private val _state: StateFlow<State>? = null,
    val postIntent: (Intent) -> Unit = {},
) {
    val state: StateFlow<State>
        @Throws(ProviderNotFoundError::class)
        get() {
            if(_state == null) {
                throw ProviderNotFoundError()
            }
            return _state
        }
}

class ProviderNotFoundError : Error()