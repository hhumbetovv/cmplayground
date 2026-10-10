package az.theternal.samples.signin

import az.theternal.mvi.Store
import az.theternal.mvi.StoreHost
import az.theternal.mvi.mapState
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.data.FakeAuthRepository
import az.theternal.samples.data.SignInResult
import az.theternal.samples.signin.level1.SignInLevel1Logic
import az.theternal.samples.signin.level1.SignInLevel1State
import az.theternal.samples.signin.level1.toUi
import az.theternal.samples.signin.level2.SignInLevel2Logic
import az.theternal.samples.signin.level2.SignInLevel2State
import az.theternal.samples.signin.level3.SignInPhase
import az.theternal.samples.signin.level3.SignInLevel3Logic
import az.theternal.samples.signin.level3.toUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * One behaviour, three implementations. Every test runs against level 1, level 2 and level 3, and asserts only on what the screen draws ([SignInUi]),
 * the effects, and the requests made — which is what makes moving a screen from one level to another a
 * refactoring with a safety net.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class SignInBehaviourTest {
    /** The implementation under test, hosted in [scope] and mapped onto what the screen draws. */
    abstract fun store(
        auth: AuthRepository,
        scope: CoroutineScope,
    ): Store<SignInUi, SignInIntent, SignInEffect>

    private class CountingAuth(
        private val real: AuthRepository = FakeAuthRepository(latency = LATENCY),
    ) : AuthRepository {
        var signIns = 0
        var verifications = 0
        var resends = 0

        override suspend fun signIn(
            email: String,
            password: String,
        ): SignInResult {
            signIns++
            return real.signIn(email, password)
        }

        override suspend fun verifyCode(
            challengeId: String,
            code: String,
        ): Boolean {
            verifications++
            return real.verifyCode(challengeId, code)
        }

        override suspend fun resendCode(challengeId: String) {
            resends++
            real.resendCode(challengeId)
        }
    }

    /** What the screen has: a store of [SignInUi], whatever the level behind it. */
    private class Screen(
        val store: Store<SignInUi, SignInIntent, SignInEffect>,
        val auth: CountingAuth,
        val effects: List<SignInEffect>,
        private val scope: TestScope,
    ) {
        val ui: SignInUi get() = store.state.value

        fun send(vararg intents: SignInIntent) {
            intents.forEach(store::postIntent)
            scope.runCurrent()
        }

        fun pass(millis: Long) {
            scope.advanceTimeBy(millis)
            scope.runCurrent()
        }

        /** Types the credentials and waits for the answer. */
        fun signIn(
            email: String,
            password: String = FakeAuthRepository.PASSWORD,
        ) {
            send(SignInIntent.EditEmail(email), SignInIntent.EditPassword(password), SignInIntent.Submit)
            pass(LATENCY)
        }
    }

    private fun screen(test: suspend TestScope.(Screen) -> Unit) = runTest {
        val auth = CountingAuth()
        val store = store(auth, backgroundScope)
        val effects = mutableListOf<SignInEffect>()
        backgroundScope.launch { store.effects.toList(effects) }
        runCurrent()
        test(Screen(store, auth, effects, this))
    }

    @Test
    fun blankCredentialsAreRejectedWithoutARequest() = screen { screen ->
        screen.send(SignInIntent.Submit)
        assertEquals(SignInMessages.BLANK, screen.ui.error)
        assertEquals(0, screen.auth.signIns)
    }

    @Test
    fun aPlainAccountSignsIn() = screen { screen ->
        screen.signIn(FakeAuthRepository.PLAIN_ACCOUNT)
        assertEquals(listOf<SignInEffect>(SignInEffect.SignedIn), screen.effects)
    }

    @Test
    fun aDoubleTapSendsOneRequestAndTheFormIsFrozenMeanwhile() = screen { screen ->
        screen.send(
            SignInIntent.EditEmail(FakeAuthRepository.PLAIN_ACCOUNT),
            SignInIntent.EditPassword(FakeAuthRepository.PASSWORD),
            SignInIntent.Submit,
            SignInIntent.Submit,
            SignInIntent.EditEmail("someone@else.com"),
        )
        assertTrue(screen.ui.isBusy)
        assertEquals(FakeAuthRepository.PLAIN_ACCOUNT, screen.ui.email)
        screen.pass(LATENCY)
        assertEquals(1, screen.auth.signIns)
    }

    @Test
    fun threeWrongPasswordsLockTheFormForAMinute() = screen { screen ->
        screen.signIn(FakeAuthRepository.PLAIN_ACCOUNT, password = "wrong")
        assertEquals(SignInMessages.WRONG_CREDENTIALS, screen.ui.error)
        screen.signIn(FakeAuthRepository.PLAIN_ACCOUNT, password = "wrong")
        screen.signIn(FakeAuthRepository.PLAIN_ACCOUNT, password = "wrong")

        assertEquals(SignInMessages.TOO_MANY_ATTEMPTS, screen.ui.error)
        assertEquals(SignInRules.LOCK_SECONDS, screen.ui.lockedSeconds)
        screen.send(SignInIntent.Submit)
        assertEquals(3, screen.auth.signIns)

        screen.pass(10 * SignInRules.TICK_MILLIS)
        assertEquals(SignInRules.LOCK_SECONDS - 10, screen.ui.lockedSeconds)

        screen.pass(SignInRules.LOCK_SECONDS * SignInRules.TICK_MILLIS)
        assertEquals(0, screen.ui.lockedSeconds)
        assertNull(screen.ui.error)
        assertTrue(screen.ui.canEditCredentials)
    }

    @Test
    fun aTwoFactorAccountVerifiesItsCode() = screen { screen ->
        screen.signIn(FakeAuthRepository.TWO_FACTOR_ACCOUNT)
        assertEquals(SignInStep.Code, screen.ui.step)
        assertEquals(SignInRules.RESEND_SECONDS, screen.ui.resendSeconds)

        screen.send(SignInIntent.EditCode("123"), SignInIntent.Verify)
        assertEquals(SignInMessages.CODE_LENGTH, screen.ui.error)

        screen.send(SignInIntent.EditCode("000000"), SignInIntent.Verify)
        screen.pass(LATENCY)
        assertEquals(SignInMessages.WRONG_CODE, screen.ui.error)

        screen.send(SignInIntent.EditCode(FakeAuthRepository.CODE), SignInIntent.Verify, SignInIntent.Verify)
        screen.pass(LATENCY)
        assertEquals(listOf<SignInEffect>(SignInEffect.SignedIn), screen.effects)
        assertEquals(2, screen.auth.verifications)
    }

    @Test
    fun aNewCodeCanBeAskedForOnlyAfterTheCountdown() = screen { screen ->
        screen.signIn(FakeAuthRepository.TWO_FACTOR_ACCOUNT)
        screen.send(SignInIntent.Resend)
        assertEquals(0, screen.auth.resends)

        screen.pass(10 * SignInRules.TICK_MILLIS)
        assertEquals(SignInRules.RESEND_SECONDS - 10, screen.ui.resendSeconds)

        screen.pass(SignInRules.RESEND_SECONDS * SignInRules.TICK_MILLIS)
        assertEquals(0, screen.ui.resendSeconds)
        screen.send(SignInIntent.Resend, SignInIntent.Resend)
        screen.pass(LATENCY)
        assertEquals(1, screen.auth.resends)
        assertEquals(SignInRules.RESEND_SECONDS - 1, screen.ui.resendSeconds)
    }

    @Test
    fun goingBackStopsTheCountdownSoTheNextOneTicksAlone() = screen { screen ->
        screen.signIn(FakeAuthRepository.TWO_FACTOR_ACCOUNT)
        screen.pass(5 * SignInRules.TICK_MILLIS + 500)
        screen.send(SignInIntent.Back)
        assertEquals(SignInStep.Credentials, screen.ui.step)
        assertEquals(FakeAuthRepository.TWO_FACTOR_ACCOUNT, screen.ui.email)

        screen.send(SignInIntent.Submit)
        screen.pass(LATENCY)
        assertEquals(SignInRules.RESEND_SECONDS, screen.ui.resendSeconds)
        screen.pass(3 * SignInRules.TICK_MILLIS)
        assertEquals(SignInRules.RESEND_SECONDS - 3, screen.ui.resendSeconds)
    }

    private companion object {
        const val LATENCY = 1_000L
    }
}

class SignInLevel1Test : SignInBehaviourTest() {
    override fun store(auth: AuthRepository, scope: CoroutineScope) =
        StoreHost(SignInLevel1State(), scope, SignInLevel1Logic(auth)).mapState { it.toUi() }
}

class SignInLevel2Test : SignInBehaviourTest() {
    override fun store(auth: AuthRepository, scope: CoroutineScope) =
        StoreHost(SignInLevel2State(), scope, SignInLevel2Logic(auth)).mapState { it.toUi() }
}

class SignInLevel3Test : SignInBehaviourTest() {
    override fun store(auth: AuthRepository, scope: CoroutineScope) =
        StoreHost(SignInPhase.Editing(), scope, SignInLevel3Logic(auth)).mapState { it.toUi() }
}
