package com.example.pmr_project.ar

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiTaskAiClient {
    suspend fun analyzeTaskCommand(context: Context, userText: String): GeminiTaskActionResult? {
        val apiKey = GeminiApiKeyProvider.getApiKey(context) ?: return null
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey"

        // Prompt pour forcer une réponse JSON structurée
        val prompt = """
        Tu es un assistant d'atelier automobile. Analyse la commande suivante et réponds uniquement en JSON compact de la forme : {\"action\":\"add|complete|start\",\"task\":\"description\"}. Si la commande ne concerne pas une tâche, réponds {\"action\":\"none\"}.
        Commande : $userText
        """.trimIndent()

        val requestBody = """
        {
          "contents": [
            { "parts": [ { "text": "$prompt" } ] }
          ]
        }
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(requestBody.toByteArray()) }

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                // Extraire le JSON de la réponse Gemini
                val cleanJson = text.substringAfter('{').substringBeforeLast('}')
                val resultJson = JSONObject("{" + cleanJson + "}")
                val action = resultJson.optString("action", "none")
                val task = resultJson.optString("task", "")
                GeminiTaskActionResult(action, task)
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class GeminiTaskActionResult(
    val action: String, // "add", "complete", "start", "none"
    val task: String
) 