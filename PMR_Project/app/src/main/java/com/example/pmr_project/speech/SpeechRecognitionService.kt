package com.example.pmr_project.speech

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class SpeechRecognitionService : Service() {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    override fun onCreate() {
        super.onCreate()
        initializeSpeechRecognizer()
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    // Début de la parole
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Changement du volume
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Buffer reçu
                }

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> _recognizedText.value = "Aucune correspondance trouvée"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> _recognizedText.value = "Timeout de la parole"
                        SpeechRecognizer.ERROR_NETWORK -> _recognizedText.value = "Erreur réseau"
                        else -> _recognizedText.value = "Erreur de reconnaissance"
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _recognizedText.value = matches[0]
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Résultats partiels
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Événements
                }
            })
        }
    }

    fun startListening() {
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...")
            }
            speechRecognizer?.startListening(intent)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
} 