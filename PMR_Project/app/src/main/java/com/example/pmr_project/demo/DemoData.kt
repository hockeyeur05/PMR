package com.example.pmr_project.demo

import com.example.pmr_project.data.entities.Part
import com.example.pmr_project.data.entities.Task
import com.example.pmr_project.data.entities.TaskStatus

object DemoData {

    private val parts = mutableListOf(
        Part(
            qrCode = "PMR-PART-1",
            name = "Filtre à huile",
            description = "Filtre à huile standard pour moteurs essence.",
            location = "Bloc moteur, côté droit",
            technicalSpecs = "Type: Cartouche, Diamètre: 76mm",
            requiredTools = "Clé à filtre à sangle"
        ),
        Part(
            qrCode = "PMR-PART-2",
            name = "Plaquettes de frein avant",
            description = "Jeu de 4 plaquettes pour freins à disque avant.",
            location = "Étrier de frein avant",
            technicalSpecs = "Composition: Céramique, Épaisseur: 14mm",
            requiredTools = "Repousse-piston, Clé dynamométrique"
        ),
        Part(
            qrCode = "PMR-PART-3",
            name = "Bougie d'allumage",
            description = "Bougie Iridium longue durée.",
            location = "Culasse, puits de bougie",
            technicalSpecs = "Électrode: Iridium, Culot: 16mm",
            requiredTools = "Clé à bougie de 16mm"
        )
    )

    private val tasks = mutableListOf(
        Task(
            id = 1,
            title = "Changer le filtre à huile",
            description = "Remplacer le filtre à huile et faire l'appoint.",
            vehicleId = "AB-123-CD",
            status = TaskStatus.COMPLETED,
            assignedTo = "Jean Dupont"
        ),
        Task(
            id = 2,
            title = "Remplacer les plaquettes de frein",
            description = "Vérifier les disques et remplacer les plaquettes.",
            vehicleId = "AB-123-CD",
            status = TaskStatus.IN_PROGRESS,
            assignedTo = "Marie Martin"
        ),
        Task(
            id = 3,
            title = "Vérifier la pression des pneus",
            description = "Ajuster la pression à 2.5 bar sur les 4 pneus.",
            vehicleId = "EF-456-GH",
            status = TaskStatus.PENDING,
            assignedTo = "Lucie Dubois"
        )
    )

    fun getParts(): List<Part> = parts
    fun getPartByQRCode(qrCode: String): Part? = parts.find { it.qrCode == qrCode }

    fun getTasks(): List<Task> = tasks.toList() // Return a copy to ensure immutability
    fun addTask(taskTitle: String) {
        val newId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        tasks.add(
            Task(
                id = newId,
                title = taskTitle,
                description = taskTitle,
                vehicleId = "N/A",
                status = TaskStatus.PENDING,
                assignedTo = "Non assigné"
            )
        )
    }

    fun completeTask(taskName: String): Boolean {
        val task = tasks.find { it.title.equals(taskName, ignoreCase = true) }
        return if (task != null) {
            task.status = TaskStatus.COMPLETED
            true
        } else false
    }

    fun startTask(taskName: String): Boolean {
        val task = tasks.find { it.title.equals(taskName, ignoreCase = true) }
        return if (task != null) {
            task.status = TaskStatus.IN_PROGRESS
            true
        } else false
    }
} 