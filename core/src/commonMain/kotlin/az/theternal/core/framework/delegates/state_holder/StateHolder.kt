package az.theternal.core.framework.delegates.state_holder

interface StateHolder<State : ViewState> {
    val stateDelegate: StateDelegate<State>

    val currentState: State
        get() = stateDelegate.currentState
}
