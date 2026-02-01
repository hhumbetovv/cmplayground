package az.theternal.core.framework.delegates.state_holder

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

context(_: ViewModel, _: StateHolder<State>)
fun <State : ViewState> StateHandler(
    initializer: () -> State,
): StateHandler<State> = StateHandler(
    createState = initializer
)

context(_: ViewModel, holder: StateHolder<State>)
fun <State : ViewState> setState(reducer: State.() -> State) {
    holder.stateHandler.setState(reducer)
}

@Composable
fun <State : ViewState> StateHolder<State>.collectAsState(): androidx.compose.runtime.State<State> {
    return stateHandler.state.collectAsStateWithLifecycle()
}
