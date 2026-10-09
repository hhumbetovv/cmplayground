package az.theternal.samples.signin

import az.theternal.mvi.StoreViewModel
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.signin.level1.SignInLevel1Logic
import az.theternal.samples.signin.level1.SignInLevel1State
import az.theternal.samples.signin.level1.toUi
import az.theternal.samples.signin.level2.SignInLevel2Logic
import az.theternal.samples.signin.level2.SignInLevel2State
import az.theternal.samples.signin.level3.SignInPhase
import az.theternal.samples.signin.level3.signInLevel3Logic
import az.theternal.samples.signin.level3.toUi

object SignInVersions {
    val level1 = SignInVersion<SignInLevel1State>(
        title = "Level 1 · flags and jobs",
        initialState = SignInLevel1State(),
        logic = ::SignInLevel1Logic,
        toUi = { it.toUi() },
    )

    val level2 = SignInVersion<SignInLevel2State>(
        title = "Level 2 · actions and policies",
        initialState = SignInLevel2State(),
        logic = ::SignInLevel2Logic,
        toUi = { it.toUi() },
    )

    val level3 = SignInVersion<SignInPhase>(
        title = "Level 3 · state machine",
        initialState = SignInPhase.Editing(),
        logic = ::signInLevel3Logic,
        toUi = { it.toUi() },
    )
}

class SignInViewModel<S>(
    val version: SignInVersion<S>,
    auth: AuthRepository,
) : StoreViewModel<S, SignInIntent, SignInEffect>(version.initialState, version.logic(auth))
