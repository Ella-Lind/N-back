package com.example.nback.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nback.model.SequenceGenerator
import com.example.nback.model.Stimulus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class GameStatus { IDLE, RUNNING, FINISHED }

data class NBackUiState(
    val status: GameStatus = GameStatus.IDLE,
    val nValue: Int = 2,
    val totalEvents: Int = 20,
    val eventDurationMs: Long = 2500L,
    val currentIndex: Int = -1,
    val currentStimulus: Stimulus? = null,
    val correctHits: Int = 0,
    val falseAlarms: Int = 0,
    val hasPressedMatchThisTick: Boolean = false,
    val isWrongGuessFeedbackActive: Boolean = false
)

class NBackViewModel : ViewModel(), TextToSpeech.OnInitListener {

    private val _uiState = MutableStateFlow(NBackUiState())
    val uiState: StateFlow<NBackUiState> = _uiState.asStateFlow()

    private val sequenceGenerator = SequenceGenerator()
    private var currentSequence: List<Stimulus> = emptyList()
    private var gameJob: Job? = null
    private var tts: TextToSpeech? = null

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    fun startGame() {
        gameJob?.cancel()
        val n = _uiState.value.nValue
        val total = _uiState.value.totalEvents
        currentSequence = sequenceGenerator.generateSequence(totalEvents = total, n = n)

        _uiState.update {
            it.copy(
                status = GameStatus.RUNNING,
                currentIndex = -1,
                correctHits = 0,
                falseAlarms = 0,
                hasPressedMatchThisTick = false
            )
        }

        gameJob = viewModelScope.launch {
            for (index in currentSequence.indices) {
                val stimulus = currentSequence[index]
                _uiState.update {
                    it.copy(
                        currentIndex = index,
                        currentStimulus = stimulus,
                        hasPressedMatchThisTick = false
                    )
                }

                // Speak auditory stimulus
                tts?.speak(stimulus.letter.toString(), TextToSpeech.QUEUE_FLUSH, null, "NBackTTS")

                delay(_uiState.value.eventDurationMs)
            }
            finishGame()
        }
    }

    fun onMatchButtonPressed() {
        val state = _uiState.value

        // Guards: Ignore press if not running, inside first N steps, or already pressed this tick
        if (state.status != GameStatus.RUNNING || state.currentIndex < state.nValue || state.hasPressedMatchThisTick) {
            return
        }

        val currentIndex = state.currentIndex
        val targetIndex = currentIndex - state.nValue
        val isTrueMatch = currentSequence[currentIndex] == currentSequence[targetIndex]

        if (isTrueMatch) {
            _uiState.update { it.copy(correctHits = it.correctHits + 1, hasPressedMatchThisTick = true) }
        } else {
            // Trigger non-offensive animated feedback on incorrect press
            viewModelScope.launch {
                _uiState.update { it.copy(falseAlarms = it.falseAlarms + 1, hasPressedMatchThisTick = true, isWrongGuessFeedbackActive = true) }
                delay(400)
                _uiState.update { it.copy(isWrongGuessFeedbackActive = false) }
            }
        }
    }

    private fun finishGame() {
        _uiState.update { it.copy(status = GameStatus.FINISHED, currentStimulus = null) }
    }

    override fun onCleared() {
        super.onCleared()
        gameJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}