package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.AuthViewModel
import com.example.ui.AuthViewModelFactory
import com.example.ui.TodoAppShell
import com.example.ui.TodoViewModel
import com.example.ui.TodoViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val todoViewModel: TodoViewModel by viewModels { TodoViewModelFactory(application) }
  private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory(application) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Extract shared URLs / clipboard notes from another app
    var sharedText: String? = null
    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
      sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
    }

    setContent {
      val settings by todoViewModel.appSettings.collectAsState()
      
      // Premium feature: Customizable accent color is read from local Room settings
      val parsedAccentColor = try {
        Color(android.graphics.Color.parseColor(settings.primaryColorHex))
      } catch (e: Exception) {
        Color(0xFF7C4DFF) // Purple fallback
      }

      val systemDark = isSystemInDarkTheme()
      val forceDark = settings.isDarkMode ?: systemDark

      MyApplicationTheme(
        darkTheme = forceDark,
        dynamicColor = false // Force custom themed colors in applet
      ) {
        // Apply accent theme overrides dynamically
        val currentColorScheme = MaterialTheme.colorScheme.copy(
          primary = parsedAccentColor,
          secondary = parsedAccentColor.copy(alpha = 0.8f)
        )

        MaterialTheme(
          colorScheme = currentColorScheme,
          typography = MaterialTheme.typography
        ) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            TodoAppShell(
              todoViewModel = todoViewModel,
              authViewModel = authViewModel,
              sharedText = sharedText
            )
          }
        }
      }
    }
  }
}
