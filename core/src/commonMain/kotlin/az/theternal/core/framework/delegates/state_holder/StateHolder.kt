package az.theternal.core.framework.delegates.state_holder

interface StateHolder<State : ViewState> {
    val stateHandler: StateHandler<State>

    val currentState: State
        get() = stateHandler.currentState
}
