package az.theternal.cmplayground.common

import platform.Foundation.NSLog

actual fun log(message: String) = NSLog(message)