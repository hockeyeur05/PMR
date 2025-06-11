package com.example.pmr_project

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pmr_project.speech.SpeechRecognitionManager
import com.example.pmr_project.ui.components.VoiceCommandButton
import com.example.pmr_project.ui.theme.PMR_ProjectTheme
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class MainActivity : ComponentActivity() {
    private var arCoreSession: Session? = null
    private lateinit var speechRecognitionManager: SpeechRecognitionManager

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            setupArCore()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        speechRecognitionManager = SpeechRecognitionManager(this)
        
        checkAndRequestPermissions()

        setContent {
            PMR_ProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        speechRecognitionManager = speechRecognitionManager
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            setupArCore()
        }
    }

    private fun setupArCore() {
        try {
            if (ArCoreApk.getInstance().requestInstall(this, true) == ArCoreApk.InstallStatus.INSTALLED) {
                arCoreSession = Session(this)
                // Configuration supplémentaire d'ARCore ici
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            // Gérer le cas où l'utilisateur refuse l'installation d'ARCore
        } catch (e: Exception) {
            // Gérer les autres erreurs
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognitionManager.isInitialized) {
            speechRecognitionManager.destroy()
        }
    }
}

@Composable
fun MainScreen(speechRecognitionManager: SpeechRecognitionManager) {
    val isListening by speechRecognitionManager.isListening.collectAsState()
    val recognizedText by speechRecognitionManager.recognizedText.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            speechRecognitionManager.stopListening()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isListening) "En écoute..." else recognizedText.ifEmpty { "Assistant RA d'atelier Automobile" },
            modifier = Modifier.padding(16.dp)
        )

        VoiceCommandButton(
            isListening = isListening,
            onStartListening = { speechRecognitionManager.startListening() },
            onStopListening = { speechRecognitionManager.stopListening() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}