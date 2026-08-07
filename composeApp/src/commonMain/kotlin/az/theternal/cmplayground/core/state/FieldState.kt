package az.theternal.cmplayground.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.reflect.KProperty

/**
 * Holder for state that is **owned by the UI**, not by the reducer: sheet expansion, a selected
 * tab, a "show password" flag — anything that never leaves the screen and never needs to survive
 * as business truth.
 *
 * Each [field] is its own `MutableState`, so writing one field invalidates only the components
 * that read that field. There is no state class copy and no diffing.
 *
 * The write side is not part of the public surface: [Field] only implements [State], so a
 * composable can read it but cannot write it. Writing requires the [FieldOwner] receiver, which
 * the holder itself implements — the same "only the owner mutates" guarantee a reducer gives,
 * without the reducer.
 *
 * ```
 * @Stable
 * class TaskEditorUiState : FieldState(), FieldOwner {
 *     val isAdvancedExpanded = field(false)
 *
 *     fun toggleAdvanced() = isAdvancedExpanded.update { !this }
 * }
 * ```
 *
 * Business state does NOT go here — it belongs in the [az.theternal.cmplayground.core.mvi.UiState]
 * the state holder reduces. See `docs/ARCHITECTURE.md` for where the line sits.
 */
@Stable
abstract class FieldState {

    @Stable
    class Field<T> internal constructor(
        private val backing: MutableState<T>,
    ) : State<T> by backing {
        internal fun write(value: T) {
            backing.value = value
        }
    }

    protected fun <T> field(initial: T): Field<T> = Field(mutableStateOf(initial))
}

/**
 * Write access to [FieldState.Field]s, granted by implementing this interface. Mutation is only
 * possible where this receiver is in scope, which is the point.
 */
interface FieldOwner {

    fun <T> FieldState.Field<T>.set(value: T) = write(value)

    fun <T> FieldState.Field<T>.update(producer: T.() -> T) = write(value.producer())

    operator fun <T> FieldState.Field<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

    /** Applies several field writes as one atomic snapshot change. */
    fun batch(block: () -> Unit) {
        Snapshot.withMutableSnapshot(block)
    }
}

/** Write access to a [FieldState] limited to previews. */
class FieldStateEditor<S : FieldState> internal constructor(val state: S) {
    fun <T> FieldState.Field<T>.set(value: T) = write(value)
}

/**
 * Fills a [FieldState] with preview values, but only inside `@Preview` rendering — at runtime the
 * block is skipped, so the same holder can be used by the real screen and by previews.
 */
@Composable
fun <S : FieldState> S.preview(block: FieldStateEditor<S>.() -> Unit): S {
    if (LocalInspectionMode.current) FieldStateEditor(this).block()
    return this
}
