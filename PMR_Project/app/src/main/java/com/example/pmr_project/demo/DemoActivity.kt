package com.example.pmr_project.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pmr_project.data.entities.Part
import com.example.pmr_project.data.entities.Task
import com.example.pmr_project.data.entities.TaskStatus
import com.example.pmr_project.ui.theme.PMR_ProjectTheme

class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PMR_ProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DemoScreen()
                }
            }
        }
    }
}

@Composable
fun DemoScreen() {
    val tasks = remember { DemoData.getTasks() }
    val parts = remember { DemoData.getParts() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mode Démo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Tâches", style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                TaskCard(task = task)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Pièces", style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(parts) { part ->
                PartCard(part = part)
            }
        }
    }
}

@Composable
fun TaskCard(task: Task) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(task.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Véhicule: ${task.vehicleId}", style = MaterialTheme.typography.bodySmall)
                StatusChip(status = task.status)
            }
        }
    }
}

@Composable
fun PartCard(part: Part) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(part.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("QR: ${part.qrCode}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus) {
    val (backgroundColor, text) = when (status) {
        TaskStatus.COMPLETED -> Pair(Color.Green, "Terminé")
        TaskStatus.IN_PROGRESS -> Pair(Color.Yellow, "En cours")
        TaskStatus.PENDING -> Pair(Color.Gray, "En attente")
        TaskStatus.PAUSED -> Pair(Color.Blue, "Pausé")
    }
    Surface(color = backgroundColor, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
    }
} 