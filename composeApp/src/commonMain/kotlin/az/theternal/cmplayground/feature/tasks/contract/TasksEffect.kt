package az.theternal.cmplayground.feature.tasks.contract

import az.theternal.cmplayground.core.mvi.UiEffect

sealed interface TasksEffect : UiEffect {
    data class OpenTask(val id: String) : TasksEffect
    data class ShowMessage(val message: String) : TasksEffect
}
