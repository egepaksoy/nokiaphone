package com.egepaksoy.kiss.ui

import androidx.compose.runtime.mutableStateListOf

sealed interface Screen {
    data object Home : Screen
    data object Dialer : Screen
    data object Messages : Screen
    data object NewMessage : Screen
    data class Thread(val threadId: Long?, val address: String) : Screen
    data object Camera : Screen
    data object Gallery : Screen
    data class Photo(val index: Int) : Screen
    data object Calculator : Screen
    data object Gate : Screen
    data object Settings : Screen
    data class AppPicker(val mode: PickerMode) : Screen
    data object Strict : Screen
    data object ChangePin : Screen
    data class Countdown(val action: CountdownAction) : Screen
}

enum class PickerMode { Allowed, Notes, Messaging }

enum class CountdownAction { ExitStrict, RemoveOwner }

class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()

    fun push(s: Screen) { if (current != s) stack.add(s) }
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
    fun replace(s: Screen) { stack[stack.lastIndex] = s }
    fun home() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        if (stack[0] != Screen.Home) stack[0] = Screen.Home
    }
}
