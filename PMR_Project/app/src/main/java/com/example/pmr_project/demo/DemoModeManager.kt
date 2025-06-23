package com.example.pmr_project.demo

import android.content.Context
import android.os.Build
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session

object DemoModeManager {
    
    /**
     * Vérifie si ARCore est disponible et installé sur l'appareil
     */
    fun isArCoreAvailable(context: Context): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability.isSupported
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Vérifie si l'appareil supporte ARCore (même s'il n'est pas installé)
     */
    fun isArCoreSupported(context: Context): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability.isSupported
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Détermine si le mode démo doit être activé
     * Forcé sur un émulateur (Build.FINGERPRINT ou MODEL contient 'emulator')
     */
    fun shouldUseDemoMode(context: Context): Boolean {
        val isEmulator = Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for")
        return isEmulator || !isArCoreAvailable(context)
    }
    
    /**
     * Tente de créer une session ARCore pour vérifier la compatibilité
     */
    fun canCreateArCoreSession(context: Context): Boolean {
        return try {
            val session = Session(context)
            session.close()
            true
        } catch (e: Exception) {
            false
        }
    }
} 