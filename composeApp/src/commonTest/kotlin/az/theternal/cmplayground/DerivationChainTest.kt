package az.theternal.cmplayground

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

private data class Source(val query: String = "", val items: List<Int> = listOf(1, 2, 3))

/**
 * The architecture assumes that a derivation reading another derivation is not recomputed when the
 * screen state changes in a way that leaves its input equal — that absorbing an unrelated change
 * costs one cheap selector and nothing more.
 *
 * The whole design rests on it, so it is asserted rather than believed.
 */
class DerivationChainTest {

    @Test
    fun chainedDerivationSkipsWorkWhenItsInputIsUnchanged() {
        val source = mutableStateOf(Source())
        var selectorRuns = 0
        var sumRuns = 0

        val items = derivedStateOf(structuralEqualityPolicy()) {
            selectorRuns++
            source.value.items
        }
        val sum = derivedStateOf(structuralEqualityPolicy()) {
            sumRuns++
            items.value.sum()
        }

        assertEquals(6, sum.value)
        assertEquals(1, selectorRuns)
        assertEquals(1, sumRuns)

        Snapshot.withMutableSnapshot { source.value = source.value.copy(query = "a") }
        assertEquals(6, sum.value)

        assertEquals(2, selectorRuns, "the cheap selector re-runs on any state change")
        assertEquals(1, sumRuns, "the expensive derivation must not re-run: its input is unchanged")

        Snapshot.withMutableSnapshot { source.value = source.value.copy(items = listOf(1, 2, 3, 4)) }
        assertEquals(10, sum.value)
        assertEquals(2, sumRuns, "the expensive derivation re-runs when its input changes")
    }

    @Test
    fun unchainedDerivationRecomputesOnEveryChange() {
        val source = mutableStateOf(Source())
        var sumRuns = 0

        // Reads the screen state directly instead of a narrowed derivation — the mistake this
        // architecture forbids.
        val sum = derivedStateOf(structuralEqualityPolicy()) {
            sumRuns++
            source.value.items.sum()
        }

        assertEquals(6, sum.value)
        Snapshot.withMutableSnapshot { source.value = source.value.copy(query = "a") }
        assertEquals(6, sum.value)

        assertEquals(2, sumRuns, "reading the state directly costs a recomputation per change")
    }
}
