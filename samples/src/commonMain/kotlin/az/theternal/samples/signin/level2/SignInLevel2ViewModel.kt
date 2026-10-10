package az.theternal.samples.signin.level2

import androidx.lifecycle.ViewModel
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.mapState
import az.theternal.mvi.store
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.signin.SignInEffect
import az.theternal.samples.signin.SignInIntent
import az.theternal.samples.signin.SignInUi
import az.theternal.samples.signin.level1.toUi

/** Level 2: the same state as level 1, with concurrency moved into policies. The screen sees only [SignInUi]: the level's own state stays inside, mapped by `mapState`. */
class SignInLevel2ViewModel(
    auth: AuthRepository,
) : ViewModel(),
    StoreOwner<SignInUi, SignInIntent, SignInEffect> {
    override val store = store(SignInLevel2State(), SignInLevel2Logic(auth)).mapState { it.toUi() }
}
