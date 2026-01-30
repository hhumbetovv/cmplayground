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
}

class StateDelegate<State: ViewState> internal constructor(
    createState: () -> State,
) {
    private val _state = MutableStateFlow(createState())
    val state: StateFlow<State> = _state

    val currentState: State
        get() = state.value

    fun setState(producer: State.() -> State) {
        _state.update(producer)
    }

}

context(viewModel: ViewModel)
fun <State : ViewState> StateProvider<State>.StateDelegate(
    initializer: () -> State
): StateDelegate<State> {
    return StateDelegate(
        createState = initializer
    )
}

context(_: ViewModel)
fun <State : ViewState> StateProvider<State>.currentState(): State {
    return stateDelegate.currentState
}

context(_: ViewModel)
@Composable
fun <State : ViewState> StateProvider<State>.collectAsState(): androidx.compose.runtime.State<State> {
    return stateDelegate.state.collectAsStateWithLifecycle()
}

context(viewModel: ViewModel)
fun <State : ViewState> StateProvider<State>.setState(producer: State.() -> State) {
    stateDelegate.setState(producer)
}