package az.theternal.samples.signin.level1

import androidx.lifecycle.ViewModel
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.mapState
import az.theternal.mvi.store
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.signin.SignInEffect
import az.theternal.samples.signin.SignInIntent
import az.theternal.samples.signin.SignInUi

/** Level 1: flags and hand-kept jobs. The screen sees only [SignInUi]: the level's own state stays inside, mapped by `mapState`. */
class SignInLevel1ViewModel(
    auth: AuthRepository,
) : ViewModel(),
    StoreOwner<SignInUi, SignInIntent, SignInEffect> {
    override val store = store(SignInLevel1State(), SignInLevel1Logic(auth)).mapState { it.toUi() }
}
