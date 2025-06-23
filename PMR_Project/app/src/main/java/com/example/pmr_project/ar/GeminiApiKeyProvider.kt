package com.example.pmr_project.ar

import android.content.Context
import java.util.Properties

object GeminiApiKeyProvider {
    fun getApiKey(context: Context): String? {
        // Essayons d'abord via BuildConfig (si la clé est injectée au build)
        try {
            val clazz = Class.forName(context.packageName + ".BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            return field.get(null) as? String
        } catch (_: Exception) {}

        // Sinon, essayons de lire local.properties (en debug uniquement)
        return try {
            val assetManager = context.assets
            assetManager.open("local.properties").use { inputStream ->
                val props = Properties()
                props.load(inputStream)
                props.getProperty("GEMINI_API_KEY")
            }
        } catch (e: Exception) {
            null
        }
    }
} 