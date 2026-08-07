package az.theternal.cmplayground.core.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Screen state as a **stable holder of fields**, not as an immutable value.
 *
 * Each [field] is its own `MutableState`, so a write invalidates exactly the composables that read
 * that field. There is no state class copy per change, no diffing, and no equality check standing
 * between the write and the components that care: the snapshot system routes it.
 *
 * The instance itself never changes for the life of the screen. That is what lets component state
 * classes be built once, up front, out of `State` references — a component's parameters are then
 * constant, and it recomposes only where it reads a `.value`.
 *
 * ```
 * @Stable
 * class TasksState : UiState() {
 *     val query = field("")
 *     val isLoading = field(true)
 *
 *     val searchField = TasksSearchFieldState(
 *         query = query,
 *         isClearVisible = derived { query.value.isNotEmpty() },
 *     )
 * }
 * ```
 *
 * Reading is public; writing is not. [Field] only implements [State], so a composable can read it
 * and cannot write it. The write helpers below are `protected`, which lets a state class mutate
 * **its own** fields — the pattern for UI-owned state, where the holder is its own owner:
 *
 * ```
 * @Stable
 * class TaskEditorUiState : UiState() {
 *     val isNoteExpanded = field(false)
 *
 *     fun toggleNote() = isNoteExpanded.update { !this }
 * }
 * ```
 *
 * A state class owned by someone else — a screen state owned by its ViewModel — is written inside
 * a [reduceState] block, which is the only other place with access.
 *
 * Rules that keep this sound:
 * - immutable values in fields (immutable collections, data classes) — the field is the mutable
 *   part, never its contents;
 * - nothing derived is stored in a field: derived values are [derived], so they cannot go stale;
 * - subclasses are annotated `@Stable` explicitly, since their fields change without changing the
 *   instance.
 */
@Stable
abstract class UiState {

    @Stable
    class Field<T> internal constructor(
        private val backing: MutableState<T>,
    ) : State<T> by backing {
        internal fun write(value: T) {
            backing.value = value
        }
    }

    protected fun <T> field(initial: T): Field<T> = Field(mutableStateOf(initial))

    /**
     * A value computed from other fields, recomputed on read and only when a dependency actually
     * changed. Consumers are notified on structural inequality, so a derivation that lands on the
     * same value costs nothing downstream.
     */
    protected fun <T> derived(calculation: () -> T): State<T> =
        derivedStateOf(structuralEqualityPolicy(), calculation)

    protected fun <T> Field<T>.set(value: T) = write(value)

    protected fun <T> Field<T>.update(producer: T.() -> T) = write(value.producer())

    /** Applies several writes to this holder's own fields as one atomic snapshot change. */
    protected fun reduceState(block: () -> Unit) {
        Snapshot.withMutableSnapshot(block)
    }
}
