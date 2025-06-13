package az.theternal.core

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
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
        get() = uiState.value

    private val _uiState: MutableStateFlow<State> = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _uiIntent: MutableSharedFlow<Intent> = MutableSharedFlow()
    val uiIntent = _uiIntent.asSharedFlow()

    private val _uiEffect: Channel<Effect> = Channel()
    val uiEffect = _uiEffect.receiveAsFlow()

    override fun onCleared() {}

    //! Initializers
    init {
        _uiIntent.onEach { intent ->
            onIntentUpdate(intent)
        }.launchIn(viewModelScope)
    }

    //! Setters
    protected fun setState(update: State.() -> State) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val newState = update(currentState)
            _uiState.emit(newState)
        }
    }

    fun postIntent(intent: Intent) {
        viewModelScope.launch {
            _uiIntent.emit(intent)
        }
    }

    fun launchEffect(effect: Effect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    protected open fun onIntentUpdate(intent: Intent) {}
}

@Composable
fun <State, Result> StateFlow<State>.select(
    transform: (State) -> Result,
): Result {
    return map(transform)
        .collectAsStateWithLifecycle(null)
        .value!!
}