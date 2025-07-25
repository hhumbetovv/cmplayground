package az.theternal.cmplayground.ui.utils

fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}