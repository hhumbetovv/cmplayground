package az.theternal.core.framework.delegates

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

interface ViewState

interface StateHolder<State: ViewState> {
    val stateStore: StateStore<State>
}

class StateStore<State: ViewState> internal constructor(
    createState: () -> State,
) {
    private val _viewState = MutableStateFlow(createState())
    val viewState: StateFlow<State> = _viewState

    val currentState: State
        get() = viewState.value

    fun setState(producer: State.() -> State) {
        _viewState.update(producer)
    }

}

context(viewModel: ViewModel)
fun <State : ViewState> StateHolder<State>.StateStore(
    initializer: () -> State
): StateStore<State> {
    return StateStore(
        createState = initializer
    )
}

context(_: ViewModel)
fun <State : ViewState> StateHolder<State>.currentState(): State {
    return stateStore.currentState
}

@Composable
context(_: ViewModel)
fun <State : ViewState> StateHolder<State>.collectAsState(): androidx.compose.runtime.State<State> {
    return stateStore.viewState.collectAsStateWithLifecycle()
}

context(viewModel: ViewModel)
fun <State : ViewState> StateHolder<State>.setState(producer: State.() -> State) {
    stateStore.setState(producer)
}