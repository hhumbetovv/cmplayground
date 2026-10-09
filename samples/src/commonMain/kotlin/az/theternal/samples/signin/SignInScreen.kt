package az.theternal.samples.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import az.theternal.mvi.CollectEffects
import az.theternal.mvi.Store
import az.theternal.mvi.collectAsState
import az.theternal.mvi.select
import az.theternal.samples.data.FakeAuthRepository
import az.theternal.samples.ui.ErrorText
import az.theternal.samples.ui.Hint
import az.theternal.samples.ui.SamplePage

/**
 * One screen for every level. It gets a store of [SignInUi] — the level's own state is mapped inside the
 * ViewModel (`mapState`) — so it cannot tell which level runs.
 */
@Composable
fun SignInScreen(
    store: Store<SignInUi, SignInIntent, SignInEffect>,
    snackbar: SnackbarHostState,
) {
    store.CollectEffects { effect ->
        when (effect) {
            SignInEffect.SignedIn -> snackbar.showSnackbar("Signed in")
        }
    }
    SignInContent(ui = store.collectAsState(), postIntent = store::postIntent)
}

@Composable
private fun SignInContent(
    ui: State<SignInUi>,
    postIntent: (SignInIntent) -> Unit,
) {
    val step by ui.select { it.step }
    SamplePage {
        Hint(
            "${FakeAuthRepository.PLAIN_ACCOUNT} signs in at once; ${FakeAuthRepository.TWO_FACTOR_ACCOUNT} asks " +
                "for a code (${FakeAuthRepository.CODE}). The password is \"${FakeAuthRepository.PASSWORD}\"; three " +
                "wrong ones lock the form for a minute.",
        )
        when (step) {
            SignInStep.Credentials -> CredentialsForm(ui = ui, postIntent = postIntent)
            SignInStep.Code -> CodeForm(ui = ui, postIntent = postIntent)
        }
        val error by ui.select { it.error }
        error?.let { ErrorText(it) }
    }
}

@Composable
private fun CredentialsForm(
    ui: State<SignInUi>,
    postIntent: (SignInIntent) -> Unit,
) {
    val form by ui.select { it }
    OutlinedTextField(
        value = form.email,
        onValueChange = { postIntent(SignInIntent.EditEmail(it)) },
        label = { Text("Email") },
        enabled = form.canEditCredentials,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.password,
        onValueChange = { postIntent(SignInIntent.EditPassword(it)) },
        label = { Text("Password") },
        enabled = form.canEditCredentials,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = { postIntent(SignInIntent.Submit) }, enabled = form.canEditCredentials) {
        Text(
            when {
                form.isBusy -> "Signing in…"
                form.lockedSeconds > 0 -> "Try again in ${form.lockedSeconds}s"
                else -> "Sign in"
            },
        )
    }
}

@Composable
private fun CodeForm(
    ui: State<SignInUi>,
    postIntent: (SignInIntent) -> Unit,
) {
    val form by ui.select { it }
    Text("We sent a code to ${form.email}")
    OutlinedTextField(
        value = form.code,
        onValueChange = { postIntent(SignInIntent.EditCode(it)) },
        label = { Text("Code") },
        enabled = !form.isBusy,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { postIntent(SignInIntent.Verify) }, enabled = !form.isBusy) {
            Text(if (form.isBusy) "Verifying…" else "Verify")
        }
        TextButton(onClick = { postIntent(SignInIntent.Resend) }, enabled = !form.isBusy && form.resendSeconds == 0) {
            Text(if (form.resendSeconds > 0) "Resend in ${form.resendSeconds}s" else "Resend code")
        }
        TextButton(onClick = { postIntent(SignInIntent.Back) }, enabled = !form.isBusy) { Text("Back") }
    }
}
