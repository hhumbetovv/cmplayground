package az.theternal.cmplayground.core.navigation.result

data class ResultState<T>(
    val value: T? = null,
    val key: String = "",
)