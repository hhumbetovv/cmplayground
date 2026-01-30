package az.theternal.core.framework.delegates

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

interface ViewState

interface StateProvider<State: ViewState> {
    val stateDelegate: StateDelegate<State>

    val currentState: State
        get() = stateDelegate.state.value
}

class StateDelegate<State: ViewState> internal constructor(
    createState: () -> State,
) {
    private val _state = MutableStateFlow(createState())
    val state: StateFlow<State> = _state

    fun setState(producer: State.() -> State) {
        _state.update(producer)
    }
}

context(_: ViewModel, _: StateProvider<State>)
fun <State : ViewState> StateDelegate(
    initializer: () -> State
): StateDelegate<State> {
    return StateDelegate(
        createState = initializer
    )
}

@Composable
fun <State : ViewState> StateProvider<State>.collectAsState(): androidx.compose.runtime.State<State> {
    return stateDelegate.state.collectAsStateWithLifecycle()
}

context(_: ViewModel)
fun <State : ViewState> StateProvider<State>.setState(producer: State.() -> State) {
    stateDelegate.setState(producer)
}