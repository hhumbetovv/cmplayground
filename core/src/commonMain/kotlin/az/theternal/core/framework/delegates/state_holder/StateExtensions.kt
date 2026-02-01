package az.theternal.core.framework.delegates.state_holder

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

context(_: ViewModel, _: StateHolder<State>)
fun <State : ViewState> StateDelegate(
    initializer: () -> State,
): StateDelegate<State> = StateDelegate(
    createState = initializer
)

context(_: ViewModel, holder: StateHolder<State>)
fun <State : ViewState> setState(reducer: State.() -> State) {
    holder.stateDelegate.setState(reducer)
}

@Composable
fun <State : ViewState> StateHolder<State>.collectAsState(): androidx.compose.runtime.State<State> {
    return stateDelegate.state.collectAsStateWithLifecycle()
}
