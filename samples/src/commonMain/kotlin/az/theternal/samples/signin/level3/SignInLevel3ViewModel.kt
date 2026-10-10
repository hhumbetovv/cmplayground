package az.theternal.samples.signin.level3

import androidx.lifecycle.ViewModel
import az.theternal.mvi.StoreOwner
import az.theternal.mvi.mapState
import az.theternal.mvi.store
import az.theternal.samples.data.AuthRepository
import az.theternal.samples.signin.SignInEffect
import az.theternal.samples.signin.SignInIntent
import az.theternal.samples.signin.SignInUi

/** Level 3: phases, with work bound by `whileIn` and `runOn`. The screen sees only [SignInUi]: the level's own state stays inside, mapped by `mapState`. */
class SignInLevel3ViewModel(
    auth: AuthRepository,
) : ViewModel(),
    StoreOwner<SignInUi, SignInIntent, SignInEffect> {
    override val store = store(SignInPhase.Editing(), SignInLevel3Logic(auth)).mapState { it.toUi() }
}
