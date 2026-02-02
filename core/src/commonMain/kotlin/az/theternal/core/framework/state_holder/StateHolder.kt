package az.theternal.core.framework.state_holder

interface ViewState

interface StateHolder<State : ViewState> {
    val stateHandler: StateHandler<State>

    val currentState: State
        get() = stateHandler.currentState
}
