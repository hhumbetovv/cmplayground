package az.theternal.cmplayground

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform