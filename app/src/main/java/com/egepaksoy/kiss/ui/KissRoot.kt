package com.egepaksoy.kiss.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.egepaksoy.kiss.KissApp
import com.egepaksoy.kiss.ui.calc.CalculatorScreen
import com.egepaksoy.kiss.ui.home.AppsScreen
import com.egepaksoy.kiss.ui.home.HomeScreen
import com.egepaksoy.kiss.ui.phone.PhoneScreen
import com.egepaksoy.kiss.ui.settings.AppPickerScreen
import com.egepaksoy.kiss.ui.settings.ChangePinScreen
import com.egepaksoy.kiss.ui.settings.CountdownScreen
import com.egepaksoy.kiss.ui.settings.GateScreen
import com.egepaksoy.kiss.ui.settings.OnboardingScreen
import com.egepaksoy.kiss.ui.settings.SettingsScreen
import com.egepaksoy.kiss.ui.settings.StrictScreen
import com.egepaksoy.kiss.ui.sms.NewMessageScreen
import com.egepaksoy.kiss.ui.sms.ThreadScreen
import com.egepaksoy.kiss.ui.theme.Kiss

@Composable
fun KissRoot() {
    val app = KissApp.instance
    val s by app.settings.state.collectAsState()
    val nav = app.nav
    val screen = nav.stack.last()

    // Ana ekranda geri tuşu hiçbir şey yapmaz; diğer ekranlarda bir geri gider.
    BackHandler(true) { nav.pop() }

    Box(Modifier.fillMaxSize().background(Kiss.colors.bg)) {
        if (!s.onboarded) {
            OnboardingScreen()
            return@Box
        }
        if (!s.enabled && !com.egepaksoy.kiss.policy.Policy.isStrictActive(androidx.compose.ui.platform.LocalContext.current)) {
            com.egepaksoy.kiss.ui.settings.PausedScreen()
            return@Box
        }
        when (screen) {
            Screen.Home -> HomeScreen(s, nav)
            is Screen.Phone -> PhoneScreen(screen.tab, s, nav)
            Screen.AppsList -> AppsScreen(s, nav)
            Screen.NewMessage -> NewMessageScreen(nav)
            is Screen.Thread -> ThreadScreen(screen.threadId, screen.address, nav)
            Screen.Calculator -> CalculatorScreen(nav)
            Screen.Gate -> GateScreen(s, nav)
            Screen.Settings -> SettingsScreen(s, nav)
            is Screen.AppPicker -> AppPickerScreen(s, screen.mode, nav)
            Screen.Strict -> StrictScreen(s, nav)
            Screen.ChangePin -> ChangePinScreen(nav)
            is Screen.Countdown -> CountdownScreen(screen.action, nav)
        }
    }
}
