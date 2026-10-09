package az.theternal.mvi

import androidx.compose.runtime.Immutable

/** What a screen shows: one immutable value; every change is a new copy, made by a [Partial]. */
@Immutable
interface ViewState

/** What the user did, in the user's words: `SubmitClicked`, `QueryChanged` — not what to do about it. */
@Immutable
interface ViewIntent

/** A one-off thing for the screen to do — navigate, show a message. Delivered once, never re-rendered. */
@Immutable
interface ViewEffect
