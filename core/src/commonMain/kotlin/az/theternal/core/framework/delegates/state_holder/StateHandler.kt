package az.theternal.core.framework.delegates.state_holder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class StateHandler<State : ViewState> internal constructor(
    createState: () -> State,
) {
    private val _state = MutableStateFlow(createState())
    val state: StateFlow<State> = _state
    val currentState: State get() = _state.value

    fun setState(reducer: State.() -> State) {
        _state.update(reducer)
    }
}
