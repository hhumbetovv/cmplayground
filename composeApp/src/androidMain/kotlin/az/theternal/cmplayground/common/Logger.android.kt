package az.theternal.cmplayground.common

import android.util.Log

actual fun log(message: String) {
    Log.d("Native Logger", message)
}