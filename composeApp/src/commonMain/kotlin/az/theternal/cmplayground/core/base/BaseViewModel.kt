package az.theternal.cmplayground.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ViewIntent
interface ViewState
interface ViewEffect


abstract class BaseViewModel<Intent: ViewIntent, State: ViewState, Effect: ViewEffect> : ViewModel() {
    //! Getters
    private val initialState: State by lazy {
        createState()
    }

    abstract fun createState(): State

    val currentState: State
        get() = viewState.value

    private val _viewState: MutableStateFlow<State> = MutableStateFlow(initialState)
    val viewState = _viewState.asStateFlow()

    private val _viewIntent: MutableSharedFlow<Intent> = MutableSharedFlow()
    val viewIntent = _viewIntent.asSharedFlow()

    private val _viewEffect: Channel<Effect> = Channel()
    val viewEffect = _viewEffect.receiveAsFlow()

    override fun onCleared() {}

    //! Initializers
    init {
        _viewIntent.onEach { intent ->
            onIntentUpdate(intent)
        }.launchIn(viewModelScope)
    }

    //! Setters
    protected fun setState(producer: State.() -> State) {
        _viewState.update { it.producer() }
    }

    fun postIntent(intent: Intent) {
        viewModelScope.launch {
            _viewIntent.emit(intent)
        }
    }

    fun launchEffect(effect: Effect) {
        viewModelScope.launch {
            _viewEffect.send(effect)
        }
    }

    protected open fun onIntentUpdate(intent: Intent) {}
}