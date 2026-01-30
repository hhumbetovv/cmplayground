//package az.theternal.core.framework.base
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.flow.MutableSharedFlow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.flow.receiveAsFlow
//
//interface UiIntent
//interface UiState
//interface UiEffect
//
//abstract class BaseViewModel<Intent : UiIntent, State : UiState, Effect : UiEffect> : ViewModel() {
//
//    var logState = true
//    var logEffect = true
//    var logIntent = true
//    var logEventFire = true
//    var logEventCatch = true
//
//    //! Getters
//    private val initialState: State by lazy {
//        createState()
//    }
//
//    abstract fun createState(): State
//
//    val state: State
//        get() = uiState.value
//
//    private val _uiState: MutableStateFlow<State> = MutableStateFlow(initialState)
//    val uiState = _uiState.asStateFlow()
//    private val _uiIntent: MutableSharedFlow<Intent> = MutableSharedFlow()
//    private val _uiEffect: Channel<Effect> = Channel()
//    val uiEffect = _uiEffect.receiveAsFlow()
////    private val _uiMessage: Channel<UiMessage> = Channel()
////    val uiMessage = _uiMessage.receiveAsFlow()
//    val eventSubscriptions = mutableListOf<Job>()
//    private val eventScope = object : EventScope() {
//        override fun setupObserver() {
//            with(this@BaseViewModel) {
//                observeEvents()
//            }
//        }
//    }
//
//    //! Lifecycle
//    init {
//        LogObserverProvider.notify(
//            LogEvent.ViewModel.Initialized(
//                viewModel = this::class.simpleName.toString()
//            )
//        )
//        _uiIntent.onEach { intent ->
//            onIntentUpdate(intent)
//        }.launchIn(viewModelScope)
//
//        eventScope.setupObserver()
//    }
//
//    override fun onCleared() {
//        LogObserverProvider.notify(
//            LogEvent.ViewModel.Disposed(
//                viewModel = this::class.simpleName.toString()
//            )
//        )
//        eventSubscriptions.forEach { it.cancel() }
//        eventSubscriptions.clear()
//    }
//
//    //! Setters
//    protected fun setState(producer: State.() -> State) {
//        val currentState = _uiState.value
//        val newState = producer(currentState)
//        if (logState) {
//            LogObserverProvider.notify(
//                LogEvent.ViewModel.StateEvent(
//                    viewModel = this::class.simpleName.toString(),
//                    oldState = currentState.toString(),
//                    newState = newState.toString(),
//                )
//            )
//        }
//        launch {
//            _uiState.emit(newState)
//        }
//    }
//
//    fun postIntent(intent: Intent) {
//        if (logIntent) {
//            LogObserverProvider.notify(
//                LogEvent.ViewModel.IntentEvent(
//                    viewModel = this::class.simpleName.toString(),
//                    action = intent.toString()
//                )
//            )
//        }
//        launch {
//            _uiIntent.emit(intent)
//        }
//    }
//
//    fun postIntent(intent: () -> Intent) = postIntent(intent())
//
//    protected fun launchEffect(effect: Effect) {
//        if (logEffect) {
//            LogObserverProvider.notify(
//                LogEvent.ViewModel.EffectEvent(
//                    viewModel = this::class.simpleName.toString(),
//                    action = effect.toString()
//                )
//            )
//        }
//        launch {
//            _uiEffect.send(effect)
//        }
//    }
//
//    protected fun launchEffect(effect: () -> Effect) = launchEffect(effect())
//
//    protected fun <T : BaseEvent> fireEvent(event: T) {
//        if (logEventFire) {
//            LogObserverProvider.notify(
//                LogEvent.ViewModel.EventFireEvent(
//                    viewModel = this::class.simpleName.toString(),
//                    event = event::class.simpleName.toString(),
//                    data = event.toString()
//                )
//            )
//        }
//        launch {
//            EventBus.fire(event)
//        }
//    }
//
//    fun sendMessage(message: UIString, type: MessageType) {
//        launch {
//            _uiMessage.send(UiMessage(message, type))
//        }
//    }
//
//    protected fun <T : BaseEvent> fireEvent(event: () -> T) = fireEvent(event())
//
//    protected open fun EventScope.observeEvents() {}
//
//    protected open fun onIntentUpdate(intent: Intent) {}
//}