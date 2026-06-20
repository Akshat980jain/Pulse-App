package com.example.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.pulse.theme.PulseBlue
import com.example.pulse.theme.WarmBackground
import com.example.pulse.ui.ai.AIHubScreen
import com.example.pulse.ui.auth.AuthScreen
import com.example.pulse.ui.auth.AuthViewModel
import com.example.pulse.ui.feed.FeedScreen
import io.github.jan.supabase.gotrue.SessionStatus

@Composable
fun MainNavigation() {
  val authViewModel: AuthViewModel = hiltViewModel()
  val sessionStatus by authViewModel.sessionStatus.collectAsState()

  when (sessionStatus) {
    is SessionStatus.LoadingFromStorage -> {
      SplashScreen()
    }
    else -> {
      val startDestination = if (sessionStatus is SessionStatus.Authenticated) Feed else Auth
      val backStack = rememberNavBackStack(startDestination)

      LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
          if (backStack.lastOrNull() != Feed) {
            backStack.clear()
            backStack.add(Feed)
          }
        } else if (sessionStatus is SessionStatus.NotAuthenticated) {
          if (backStack.lastOrNull() != Auth) {
            backStack.clear()
            backStack.add(Auth)
          }
        }
      }

      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
          entryProvider {
            entry<Auth> {
              AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                  if (backStack.lastOrNull() != Feed) {
                    backStack.clear()
                    backStack.add(Feed)
                  }
                }
              )
            }
            entry<Feed> {
              FeedScreen(
                viewModel = hiltViewModel(),
                onNavigateToAIHub = {
                  backStack.add(AIHub)
                },
                onSignOut = {
                  authViewModel.signOut()
                }
              )
            }
            entry<AIHub> {
              AIHubScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = {
                  backStack.removeLastOrNull()
                }
              )
            }
          },
      )
    }
  }
}

@Composable
private fun SplashScreen() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(WarmBackground),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.Home,
        contentDescription = null,
        tint = PulseBlue,
        modifier = Modifier.size(64.dp)
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text = "Pulse",
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        color = PulseBlue
      )
      Spacer(Modifier.height(24.dp))
      CircularProgressIndicator(color = PulseBlue)
    }
  }
}
