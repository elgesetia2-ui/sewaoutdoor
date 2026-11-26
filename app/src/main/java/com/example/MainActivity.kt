package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainScaffold
import com.example.ui.theme.SewaOutdoorTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SewaOutdoorTheme {
                val mainViewModel: MainViewModel = viewModel()
                val session by mainViewModel.session.collectAsState()
                val employees by mainViewModel.employees.collectAsState()
                val settings by mainViewModel.settings.collectAsState()
                val loginError by mainViewModel.loginError.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    mainViewModel.toastMessage.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                if (!session.isLoggedIn) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            LoginScreen(
                                storeName = settings.store.namaToko,
                                employees = employees,
                                loginError = loginError,
                                onClearError = { mainViewModel.clearLoginError() },
                                onLogin = { role, name, pin -> mainViewModel.login(role, name, pin) }
                            )
                        }
                    }
                } else {
                    MainScaffold(
                        viewModel = mainViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SewaOutdoorLoginPreview() {
    SewaOutdoorTheme {
        LoginScreen(
            storeName = "Sewa Outdoor Nusantara",
            employees = com.example.data.SeedData.initialEmployees(),
            loginError = null,
            onClearError = {},
            onLogin = { _, _, _ -> }
        )
    }
}
