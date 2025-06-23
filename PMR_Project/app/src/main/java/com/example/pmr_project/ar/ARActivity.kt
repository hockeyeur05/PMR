package com.example.pmr_project.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.pmr_project.data.entities.Part
import com.example.pmr_project.data.entities.Task
import com.example.pmr_project.data.entities.TaskStatus
import com.example.pmr_project.demo.DemoData
import com.example.pmr_project.speech.SpeechRecognitionManager
import com.example.pmr_project.ui.theme.PMR_ProjectTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pmr_project.ar.GeminiTaskAiClient

// --- Data classes for QR code positions ---
data class BarcodeWithPosition(
    val barcode: Barcode,
    val position: BarcodePosition
)
data class BarcodePosition(
    val x: Float,
    val y: Float
)

class ARActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var speechManager: SpeechRecognitionManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            setContentWithCamera()
        } else {
            Toast.makeText(this, "Permissions caméra et micro requises", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        speechManager = SpeechRecognitionManager(this)
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            setContentWithCamera()
        }
    }

    private fun setContentWithCamera() {
        setContent {
            PMR_ProjectTheme {
                ARWorkspaceScreen(cameraExecutor, speechManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        speechManager.destroy()
    }
}

@Composable
fun ARWorkspaceScreen(cameraExecutor: ExecutorService, speechManager: SpeechRecognitionManager) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val tasks = remember { mutableStateListOf(*DemoData.getTasks().toTypedArray()) }
    var detectedBarcodesWithPosition by remember { mutableStateOf<List<BarcodeWithPosition>>(emptyList()) }
    var previousPositions by remember { mutableStateOf<Map<String, BarcodePosition>>(emptyMap()) }
    var voiceCommandFeedback by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val recognizedText by speechManager.recognizedText.collectAsState()
    var isListening by remember { mutableStateOf(false) }

    // Analyse locale de la commande vocale (robuste, sans Gemini)
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotBlank()) {
            val lower = recognizedText.lowercase().trim().removeSuffix(".")
            val startRegex = Regex("je commence( la)? (tache|tâche)? ?(.+)")
            val finishRegex = Regex("j'ai fini( la)? (tache|tâche)? ?(.+)")
            val addRegex = Regex("ajoute(r)?( la)? (tache|tâche)? ?(.+)")
            when {
                startRegex.matches(lower) -> {
                    val match = startRegex.find(lower)
                    val taskName = match?.groupValues?.get(3)?.ifBlank { match.groupValues.getOrNull(4) ?: "" }?.trim() ?: ""
                    val found = DemoData.startTask(taskName)
                    tasks.clear()
                    tasks.addAll(DemoData.getTasks())
                    voiceCommandFeedback = if (found) "Tâche démarrée : '$taskName'" else "Tâche non trouvée : '$taskName'"
                }
                finishRegex.matches(lower) -> {
                    val match = finishRegex.find(lower)
                    val taskName = match?.groupValues?.get(3)?.ifBlank { match.groupValues.getOrNull(4) ?: "" }?.trim() ?: ""
                    val found = DemoData.completeTask(taskName)
                    tasks.clear()
                    tasks.addAll(DemoData.getTasks())
                    voiceCommandFeedback = if (found) "Tâche terminée : '$taskName'" else "Tâche non trouvée : '$taskName'"
                }
                addRegex.matches(lower) -> {
                    val match = addRegex.find(lower)
                    val taskName = match?.groupValues?.get(4)?.ifBlank { match.groupValues.getOrNull(5) ?: "" }?.trim() ?: ""
                    if (taskName.isNotBlank()) {
                        DemoData.addTask(taskName)
                        tasks.clear()
                        tasks.addAll(DemoData.getTasks())
                        voiceCommandFeedback = "Tâche ajoutée : '$taskName'"
                    } else {
                        voiceCommandFeedback = "Aucune tâche à ajouter."
                    }
                }
                else -> {
                    voiceCommandFeedback = "Commande non reconnue.\nEssayez :\n- je commence (tâche)\n- j'ai fini (tâche)\n- ajoute la tache (tâche)"
                }
            }
        }
    }

    // Timer to hide voice command feedback
    LaunchedEffect(voiceCommandFeedback) {
        if (voiceCommandFeedback != null) {
            delay(4000)
            voiceCommandFeedback = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxyWithPosition(
                                barcodeScanner = scanner,
                                imageProxy = imageProxy,
                                previewView = previewView,
                                onBarcodesDetected = { barcodesWithPosition ->
                                    // Appliquer la stabilisation pour réduire les tremblements
                                    val stabilizedBarcodes = barcodesWithPosition.map { barcodeWithPos ->
                                        val qrCode = barcodeWithPos.barcode.rawValue ?: ""
                                        val previousPos = previousPositions[qrCode]
                                        
                                        val stabilizedPosition = if (previousPos != null) {
                                            // Lissage des positions pour réduire les tremblements
                                            val smoothingFactor = 0.7f
                                            val newX = previousPos.x * smoothingFactor + barcodeWithPos.position.x * (1f - smoothingFactor)
                                            val newY = previousPos.y * smoothingFactor + barcodeWithPos.position.y * (1f - smoothingFactor)
                                            BarcodePosition(newX, newY)
                                        } else {
                                            barcodeWithPos.position
                                        }
                                        
                                        BarcodeWithPosition(barcodeWithPos.barcode, stabilizedPosition)
                                    }
                                    
                                    // Mettre à jour les positions précédentes
                                    previousPositions = stabilizedBarcodes.associate { 
                                        (it.barcode.rawValue ?: "") to it.position 
                                    }
                                    
                                    detectedBarcodesWithPosition = stabilizedBarcodes
                                    scanError = null
                                },
                                onFailure = { exception ->
                                    scanError = "Erreur du scanner: ${exception.localizedMessage}"
                                    detectedBarcodesWithPosition = emptyList()
                                }
                            )
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("ARWorkspace", "Use case binding failed", exc)
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        ARWorkspaceOverlay(
            tasks = tasks,
            detectedBarcodesWithPosition = detectedBarcodesWithPosition,
            voiceCommandFeedback = voiceCommandFeedback,
            scanError = scanError,
            isListening = isListening,
            onMicClick = {
                if (isListening) {
                    speechManager.stopListening()
                } else {
                    speechManager.startListening()
                }
                isListening = !isListening
            }
        )
    }
}

@Composable
fun ARWorkspaceOverlay(
    tasks: List<Task>,
    detectedBarcodesWithPosition: List<BarcodeWithPosition>,
    voiceCommandFeedback: String?,
    scanError: String?,
    isListening: Boolean,
    onMicClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)

    Box(modifier = Modifier.fillMaxSize()) {
        // Bouton retour/maison en haut à gauche
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            IconButton(onClick = { activity?.finish() }) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Retour à l'accueil",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        // Barre de titre en haut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Engineering, contentDescription = "Mode Mécanicien", tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mode Mécanicien AR", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Liste des tâches à gauche avec design amélioré
        Card(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(0.7f)
                .width(320.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E2E).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00D4FF),
                        Color(0xFF7B2CBF)
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Tâches",
                        tint = Color(0xFF00D4FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Tâches à faire",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D4FF)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        TaskItem(task)
                    }
                }
            }
        }

        // Affichage des QR codes détectés avec positionnement précis
        detectedBarcodesWithPosition.forEach { barcodeWithPosition ->
            val part = DemoData.getPartByQRCode(barcodeWithPosition.barcode.rawValue ?: "")
            
            if (part != null) {
                CompactPartInfoCard(
                    part = part,
                    modifier = Modifier
                        .offset(
                            x = barcodeWithPosition.position.x.dp,
                            y = barcodeWithPosition.position.y.dp
                        )
                        .widthIn(max = 280.dp)
                )
            } else {
                CompactGenericCard(
                    content = barcodeWithPosition.barcode.rawValue ?: "",
                    context = context,
                    modifier = Modifier
                        .offset(
                            x = barcodeWithPosition.position.x.dp,
                            y = barcodeWithPosition.position.y.dp
                        )
                        .widthIn(max = 200.dp)
                )
            }
        }

        // Feedback vocal
        voiceCommandFeedback?.let { feedback ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(text = feedback, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
            }
        }

        // Message de recherche
        if (detectedBarcodesWithPosition.isEmpty()) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (voiceCommandFeedback != null) 80.dp else 16.dp),
                colors = CardDefaults.cardColors(containerColor = if (scanError != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            ) {
                Text(
                    text = scanError ?: "Recherche de QR codes...",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = if (scanError != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Bouton micro flottant en bas à droite (utilise le manager partagé)
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = onMicClick,
                containerColor = if (isListening) Color(0xFF00D4FF) else MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = if (isListening) "Arrêter la reconnaissance vocale" else "Démarrer la reconnaissance vocale",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun TaskItem(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.status == TaskStatus.COMPLETED) {
                Color(0xFF1A472A).copy(alpha = 0.8f) // Vert foncé pour les tâches terminées
            } else {
                Color(0xFF2A2A3E).copy(alpha = 0.9f) // Bleu foncé pour les tâches en cours
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.status == TaskStatus.COMPLETED) {
                Color(0xFF00FF88)
            } else {
                Color(0xFF00D4FF)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (task.status) {
                    TaskStatus.COMPLETED -> Icons.Default.CheckCircle
                    else -> Icons.Default.Schedule
                },
                contentDescription = "Status",
                tint = if (task.status == TaskStatus.COMPLETED) {
                    Color(0xFF00FF88)
                } else {
                    Color(0xFF00D4FF)
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = if (task.status == TaskStatus.COMPLETED) FontWeight.Normal else FontWeight.Medium
            )
        }
    }
}

@Composable
fun CompactPartInfoCard(part: Part, modifier: Modifier = Modifier) {
    var isConfirmed by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(200, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        Card(
            modifier = modifier
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00D4FF),
                            Color(0xFF7B2CBF)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isConfirmed) {
                    Color(0xFF1A1A2E).copy(alpha = 0.95f)
                } else {
                    Color(0xFF16213E).copy(alpha = 0.9f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // En-tête compact
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = when {
                            part.name.contains("huile", ignoreCase = true) -> Icons.Default.Opacity
                            part.name.contains("filtre", ignoreCase = true) -> Icons.Default.FilterAlt
                            part.name.contains("frein", ignoreCase = true) -> Icons.Default.DoNotTouch
                            else -> Icons.Default.Build
                        },
                        contentDescription = "Type de pièce",
                        tint = Color(0xFF00D4FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        part.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D4FF),
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    part.qrCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color(0xFF00D4FF).copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                
                // Informations compactes
                CompactInfoRow(icon = Icons.Default.Place, value = part.location)
                CompactInfoRow(icon = Icons.Default.Settings, value = part.technicalSpecs, maxLines = 2)
                
                // Bouton de confirmation pour l'huile
                if (part.name.contains("huile", ignoreCase = true) && !isConfirmed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { isConfirmed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D4FF)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Confirmer",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Confirmer",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Message de confirmation
                if (isConfirmed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Confirmé",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Confirmé",
                            color = Color(0xFF00FF88),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactInfoRow(icon: ImageVector, value: String, maxLines: Int = 1) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00D4FF),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = maxLines,
            fontSize = 11.sp
        )
    }
}

@Composable
fun CompactGenericCard(content: String, context: android.content.Context, modifier: Modifier = Modifier) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(200, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "QR Code détecté",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    content,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Impossible d'ouvrir ce contenu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = "Ouvrir", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Ouvrir", fontSize = 10.sp)
                }
            }
        }
    }
}

@ExperimentalGetImage
private fun processImageProxyWithPosition(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    previewView: PreviewView,
    onBarcodesDetected: (List<BarcodeWithPosition>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val barcodesWithPosition = barcodes.map { barcode ->
                    val boundingBox = barcode.boundingBox
                    val position = if (boundingBox != null) {
                        // Convertir les coordonnées de l'image en coordonnées d'écran
                        val imageWidth = imageProxy.width.toFloat()
                        val imageHeight = imageProxy.height.toFloat()
                        val screenWidth = previewView.width.toFloat()
                        val screenHeight = previewView.height.toFloat()
                        
                        // Calculer le centre du QR code
                        val centerX = (boundingBox.centerX() / imageWidth) * screenWidth
                        val centerY = (boundingBox.centerY() / imageHeight) * screenHeight
                        
                        // Calculer la taille du QR code à l'écran
                        val qrWidth = (boundingBox.width() / imageWidth) * screenWidth
                        val qrHeight = (boundingBox.height() / imageHeight) * screenHeight
                        
                        // Ajuster pour centrer la carte sur le QR code
                        // La carte fait environ 280dp de large, on la centre sur le QR code
                        val cardWidth = 280f
                        val cardHeight = 200f
                        val adjustedX = centerX - (cardWidth / 2)
                        val adjustedY = centerY - (cardHeight / 2)
                        
                        // S'assurer que la carte reste dans les limites de l'écran
                        val finalX = adjustedX.coerceIn(16f, screenWidth - cardWidth - 16f)
                        val finalY = adjustedY.coerceIn(100f, screenHeight - cardHeight - 100f)
                        
                        BarcodePosition(finalX, finalY)
                    } else {
                        BarcodePosition(0f, 0f)
                    }
                    
                    BarcodeWithPosition(barcode, position)
                }
                onBarcodesDetected(barcodesWithPosition)
            }
            .addOnFailureListener { exception ->
                Log.e("ARActivity", "QR Code scanning failed", exception)
                onFailure(exception)
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}
