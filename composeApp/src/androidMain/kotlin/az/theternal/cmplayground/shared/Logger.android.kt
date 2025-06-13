@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package az.theternal.cmplayground.shared

import android.util.Log

actual object Logger {
    actual fun d(message: Any) {
        Log.d("Native Logger", message.toString())
    }
}