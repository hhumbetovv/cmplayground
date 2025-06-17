@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package az.theternal.common.utils

import platform.Foundation.NSLog

actual object Logger {
    actual fun d(message: Any) {
        NSLog("[Native Logger]: $message")
    }
}