package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.ui.ShoppingViewModel
import com.example.ui.screens.ShoppingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(this)[ShoppingViewModel::class.java]

        setContent {
            val isDarkThemeOverride by viewModel.isDarkTheme.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = isDarkThemeOverride ?: systemDark

            MyApplicationTheme(darkTheme = useDarkTheme) {
                ShoppingScreen(
                    viewModel = viewModel,
                    isDarkModeActive = useDarkTheme,
                    onToggleDarkMode = { viewModel.setDarkTheme(!useDarkTheme) }
                )
            }
        }
    }
}
