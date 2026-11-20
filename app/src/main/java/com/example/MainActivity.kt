package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.BankRepository
import com.example.game3d.BankGlSurfaceView
import com.example.ui.GameHudOverlay
import com.example.ui.GameModals
import com.example.ui.GameViewModel
import com.example.ui.ProfileUiState
import com.example.ui.theme.CharcoalDark
import com.example.ui.theme.SkyBankTheme
import com.example.ui.theme.TakaGold
import com.example.ui.theme.TakaGoldLight
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkyBankTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            SkyBankGameScreen(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "ম্যানেজার"
            )
        }
    }
}

@Composable
fun SkyBankGameScreen(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: GameViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GameViewModel(
                    repository = BankRepository(db),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileLoadState.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsState.collectAsStateWithLifecycle()

    when (val state = profileState) {
        is ProfileUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_skybank_splash),
                    contentDescription = "ইস্কাই ব্যাংক লোডিং",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x9906130F))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = TakaGoldLight,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "ইস্কাই ব্যাংক প্রস্তুত হচ্ছে...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        is ProfileUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CharcoalDark)
                    .safeDrawingPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ডাটাবেস সংযোগে সমস্যা",
                            color = TakaGoldLight,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.message,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                signOutUser(credentialManager, scope)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TakaGold)
                        ) {
                            Text("অ্যাকাউন্ট পরিবর্তন করুন", color = CharcoalDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        is ProfileUiState.Ready -> {
            Box(modifier = Modifier.fillMaxSize()) {
                BankGlSurfaceView(
                    snapshot = hudState.worldSnapshot,
                    cameraSensitivity = hudState.cameraSensitivity,
                    onCameraDelta = { dyaw, dpitch -> viewModel.onCameraDelta(dyaw, dpitch) },
                    onViewportTap = { viewModel.onInteractTapped() },
                    onMarkersUpdated = { markers -> viewModel.onMarkersUpdated(markers) },
                    modifier = Modifier.fillMaxSize()
                )

                GameHudOverlay(
                    hud = hudState,
                    viewModel = viewModel
                )

                GameModals(
                    hud = hudState,
                    transactions = transactions,
                    viewModel = viewModel,
                    onSignOut = {
                        signOutUser(credentialManager, scope)
                    }
                )
            }
        }
    }
}
