package com.example.pmr_project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pmr_project.ar.ARActivity
import com.example.pmr_project.data.entities.Part
import com.example.pmr_project.data.entities.Task
import com.example.pmr_project.demo.DemoModeManager
import com.example.pmr_project.qr.QRScannerActivity
import com.example.pmr_project.speech.SpeechRecognitionManager
import com.example.pmr_project.ui.components.VoiceCommandButton
import com.example.pmr_project.ui.theme.PMR_ProjectTheme
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import androidx.compose.foundation.clickable
import com.example.pmr_project.data.entities.TaskStatus
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pmr_project.demo.DemoActivity

class MainActivity : ComponentActivity() {
    private var arCoreSession: Session? = null
    private lateinit var speechRecognitionManager: SpeechRecognitionManager

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            checkArCoreAndProceed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechRecognitionManager = SpeechRecognitionManager(this)
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            checkArCoreAndProceed()
        }
    }

    private fun checkArCoreAndProceed() {
        if (DemoModeManager.shouldUseDemoMode(this)) {
            // Mode démo si ARCore n'est pas disponible
            setContent {
                PMR_ProjectTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(
                            speechRecognitionManager = speechRecognitionManager,
                            isDemoMode = true
                        )
                    }
                }
            }
        } else {
            // Mode AR complet
        setupArCore()
        setContent {
            PMR_ProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                            speechRecognitionManager = speechRecognitionManager,
                            isDemoMode = false
                    )
                    }
                }
            }
        }
    }

    private fun setupArCore() {
        try {
            if (ArCoreApk.getInstance().requestInstall(this, true) == ArCoreApk.InstallStatus.INSTALLED) {
                arCoreSession = Session(this)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    speechRecognitionManager: SpeechRecognitionManager,
    isDemoMode: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assistant RA d'Atelier",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    if (isDemoMode) {
                        Text(
                            text = "Mode Démo",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                VoiceCommandButton(
                    isListening = speechRecognitionManager.isListening.collectAsState().value,
                    onStartListening = { speechRecognitionManager.startListening() },
                    onStopListening = { speechRecognitionManager.stopListening() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    navController = navController,
                    isDemoMode = isDemoMode
                )
            }
            composable("tasks") {
                TasksScreen(navController = navController)
            }
            composable("qr_scanner") {
                QRScannerScreen(navController = navController)
            }
            composable("ar_viewer") {
                ARViewerScreen(navController = navController)
            }
            composable("voice_commands") {
                VoiceCommandsScreen(
                    speechRecognitionManager = speechRecognitionManager,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    navController: NavHostController,
    isDemoMode: Boolean
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header avec statut
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tableau de Bord",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isDemoMode) "Mode Démo - ARCore non disponible" else "Mode AR Complet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Grille des actions principales
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "Tâches",
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("tasks") }
                    )
                    
                    ActionCard(
                        title = "Scanner QR",
                        icon = Icons.Default.QrCodeScanner,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("qr_scanner") }
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "Vue AR",
                        icon = Icons.Default.ViewInAr,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("ar_viewer") }
                    )
                    
                    ActionCard(
                        title = "Commande Vocale",
                        icon = Icons.Default.Mic,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("voice_commands") }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Statistiques rapides
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Statistiques",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Tâches", "12", "En cours")
                    StatItem("Pièces", "8", "Scannées")
                    StatItem("Notes", "5", "Vocales")
                }
            }
        }

        // Button for the new AR Workspace
        DashboardButton(
            text = "Mode Mécanicien AR",
            icon = Icons.Default.Engineering,
            onClick = {
                context.startActivity(Intent(context, ARActivity::class.java))
            },
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    status: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun TasksScreen(navController: NavHostController) {
    val tasks = remember {
        listOf(
            Task(
                id = 1,
                title = "Remplacement filtre à air",
                description = "Remplacer le filtre à air du moteur",
                status = TaskStatus.IN_PROGRESS,
                vehicleId = "VH001",
                assignedTo = "Mécanicien 1"
            ),
            Task(
                id = 2,
                title = "Vérification freins",
                description = "Contrôler l'état des plaquettes de frein",
                status = TaskStatus.PENDING,
                vehicleId = "VH002",
                assignedTo = "Mécanicien 2"
            ),
            Task(
                id = 3,
                title = "Changement huile",
                description = "Vidanger et remplacer l'huile moteur",
                status = TaskStatus.COMPLETED,
                vehicleId = "VH003",
                assignedTo = "Mécanicien 1"
            )
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, "Retour")
            }
            
            Text(
                text = "Tâches",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { /* Ajouter tâche */ }) {
                Icon(Icons.Default.Add, "Ajouter")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                TaskCard(task = task)
            }
        }
    }
}

@Composable
fun TaskCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (task.status) {
                TaskStatus.COMPLETED -> Color(0xFFE8F5E8)
                TaskStatus.IN_PROGRESS -> Color(0xFFFFF3E0)
                TaskStatus.PENDING -> Color(0xFFF3E5F5)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StatusChip(status = task.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Véhicule: ${task.vehicleId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Assigné: ${task.assignedTo ?: "Non assigné"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        TaskStatus.COMPLETED -> Triple(Color(0xFF4CAF50), Color.White, "Terminé")
        TaskStatus.IN_PROGRESS -> Triple(Color(0xFFFF9800), Color.White, "En cours")
        TaskStatus.PENDING -> Triple(Color(0xFF9C27B0), Color.White, "En attente")
        TaskStatus.PAUSED -> Triple(Color(0xFF607D8B), Color.White, "Pausé")
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun QRScannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, "Retour")
            }
            
            Text(
                text = "Scanner QR Code",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { 
                val intent = Intent(context, QRScannerActivity::class.java)
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.QrCodeScanner, "Scanner")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "QR Scanner",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Scanner de QR Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Scannez les QR codes des pièces pour obtenir les informations techniques",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        val intent = Intent(context, QRScannerActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, "Scanner")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lancer le Scanner")
                }
            }
        }
    }
}

@Composable
fun ARViewerScreen(navController: NavHostController) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, "Retour")
            }
            
            Text(
                text = "Vue Réalité Augmentée",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { 
                val intent = Intent(context, ARActivity::class.java)
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.ViewInAr, "AR")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "AR Viewer",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Vue Réalité Augmentée",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Visualisez les pièces en réalité augmentée avec les informations techniques",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        val intent = Intent(context, ARActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ViewInAr, "AR")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lancer la Vue AR")
                }
            }
        }
    }
}

@Composable
fun VoiceCommandsScreen(
    speechRecognitionManager: SpeechRecognitionManager,
    navController: NavHostController
) {
    val isListening by speechRecognitionManager.isListening.collectAsState()
    val recognizedText by speechRecognitionManager.recognizedText.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, "Retour")
            }
            
            Text(
                text = "Commandes Vocales",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Commands",
                    modifier = Modifier.size(64.dp),
                    tint = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isListening) "En écoute..." else "Commandes Vocales",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Utilisez votre voix pour naviguer et contrôler l'application",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (recognizedText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = recognizedText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Button(
                    onClick = { 
                        if (isListening) {
                            speechRecognitionManager.stopListening()
                        } else {
                            speechRecognitionManager.startListening()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        if (isListening) "Arrêter" else "Commencer"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isListening) "Arrêter l'écoute" else "Commencer l'écoute")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Liste des commandes vocales disponibles
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
    ) {
        Text(
                    text = "Commandes disponibles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf(
                    "Ouvrir tâches",
                    "Scanner QR code",
                    "Vue réalité augmentée",
                    "Ajouter note",
                    "Marquer tâche terminée"
                ).forEach { command ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.KeyboardVoice,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = command,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color
) {
    Button(
        onClick = onClick,
            modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}