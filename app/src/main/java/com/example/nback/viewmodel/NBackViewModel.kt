package com.example.nback.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nback.db.AppDatabase
import com.example.nback.db.Score
import com.example.nback.db.ScoreDao
import com.example.nback.db.ScoreSortType
import com.example.nback.model.PressRules
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
import kotlin.time.Duration.Companion.milliseconds

enum class GameStatus { IDLE, RUNNING, FINISHED }
enum class AppScreen { GAME, SCORES }

val ALL_AVAILABLE_LETTERS: List<Char> = listOf('C', 'K', 'P', 'T', 'Q', 'R', 'V', 'L', 'S', 'X', 'B', 'H', 'M')

data class NBackUiState(
    val status: GameStatus = GameStatus.IDLE,
    val currentScreen: AppScreen = AppScreen.GAME,
    val nValue: Int = 2,
    val gridSize: Int = 3,
    val totalEvents: Int = 20,
    val letterCount: Int = 7,
    val eventDurationMs: Long = 2500L,
    val currentIndex: Int = -1,
    val currentStimulus: Stimulus? = null,
    val correctHits: Int = 0,
    val falseAlarms: Int = 0,
    val hasPressedMatchThisTick: Boolean = false,
    val isWrongGuessFeedbackActive: Boolean = false,
    val showSaveScoreDialog: Boolean = false,
    val scores: List<Score> = emptyList(),
    val sortType: ScoreSortType = ScoreSortType.DATE_DESC
)

class NBackViewModel : ViewModel(), TextToSpeech.OnInitListener {

    private val _uiState = MutableStateFlow(NBackUiState())
    val uiState: StateFlow<NBackUiState> = _uiState.asStateFlow()

    private var currentSequence: List<Stimulus> = emptyList()
    private var gameJob: Job? = null
    private var tts: TextToSpeech? = null
    private var scoreDao: ScoreDao? = null
    private var scoresJob: Job? = null

    fun initTtsAndDb(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
        if (scoreDao == null) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            scoreDao = db.scoreDao()
            observeScores()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    fun setScreen(screen: AppScreen) {
        if (screen == AppScreen.SCORES && _uiState.value.status == GameStatus.RUNNING) {
            cancelGame()
        }
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setSortType(sortType: ScoreSortType) {
        _uiState.update { it.copy(sortType = sortType) }
        observeScores()
    }

    private fun observeScores() {
        val dao = scoreDao ?: return
        scoresJob?.cancel()
        scoresJob = viewModelScope.launch {
            val flow = when (_uiState.value.sortType) {
                ScoreSortType.NAME_ASC -> dao.getScoresSortedByName()
                ScoreSortType.SCORE_DESC -> dao.getScoresSortedByScore()
                ScoreSortType.DATE_DESC -> dao.getScoresSortedByDate()
            }
            flow.collect { list ->
                _uiState.update { it.copy(scores = list) }
            }
        }
    }

    fun saveScore(playerName: String) {
        val name = playerName.trim()
        if (name.isEmpty()) return

        val state = _uiState.value
        val score = Score(
            playerName = name,
            score = state.correctHits,
            totalEvents = state.totalEvents,
            nValue = state.nValue,
            gridSize = state.gridSize
        )

        viewModelScope.launch {
            scoreDao?.insertScore(score)
            _uiState.update { it.copy(showSaveScoreDialog = false) }
        }
    }

    fun dismissSaveScoreDialog() {
        _uiState.update { it.copy(showSaveScoreDialog = false) }
    }

    fun setGridSize(size: Int) {
        if (size in 3..5) {
            _uiState.update { it.copy(gridSize = size) }
        }
    }

    fun setNValue(n: Int) {
        if (n >= 1) {
            val newTotal = maxOf(_uiState.value.totalEvents, n + 1)
            _uiState.update { it.copy(nValue = n, totalEvents = newTotal) }
        }
    }

    fun increaseN() {
        setNValue(_uiState.value.nValue + 1)
    }

    fun decreaseN() {
        if (_uiState.value.nValue > 1) {
            setNValue(_uiState.value.nValue - 1)
        }
    }

    fun setLetterCount(count: Int) {
        if (count in 3..ALL_AVAILABLE_LETTERS.size) {
            _uiState.update { it.copy(letterCount = count) }
        }
    }

    fun increaseLetterCount() {
        if (_uiState.value.letterCount < ALL_AVAILABLE_LETTERS.size) {
            _uiState.update { it.copy(letterCount = it.letterCount + 1) }
        }
    }

    fun decreaseLetterCount() {
        if (_uiState.value.letterCount > 3) {
            _uiState.update { it.copy(letterCount = it.letterCount - 1) }
        }
    }

    fun setTotalEvents(events: Int) {
        if (events > _uiState.value.nValue) {
            _uiState.update { it.copy(totalEvents = events) }
        }
    }

    fun increaseTotalEvents() {
        _uiState.update { it.copy(totalEvents = it.totalEvents + 5) }
    }

    fun decreaseTotalEvents() {
        val minEvents = _uiState.value.nValue + 1
        if (_uiState.value.totalEvents - 5 >= minEvents) {
            _uiState.update { it.copy(totalEvents = it.totalEvents - 5) }
        }
    }

    fun startGame() {
        gameJob?.cancel()
        val n = _uiState.value.nValue
        val total = _uiState.value.totalEvents
        val gridSize = _uiState.value.gridSize
        val positionCount = gridSize * gridSize
        val letters = ALL_AVAILABLE_LETTERS.take(_uiState.value.letterCount)
        val generator = SequenceGenerator(availableLetters = letters)
        currentSequence = generator.generateSequence(
            totalEvents = total,
            n = n,
            positionCount = positionCount
        )

        _uiState.update {
            it.copy(
                status = GameStatus.RUNNING,
                currentIndex = -1,
                correctHits = 0,
                falseAlarms = 0,
                hasPressedMatchThisTick = false,
                showSaveScoreDialog = false
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

                delay(_uiState.value.eventDurationMs.milliseconds)
            }
            finishGame()
        }
    }

    fun onMatchButtonPressed() {
        val state = _uiState.value

        // Guards: ignore the press if the round is not running, we are inside the first N events,
        // or the player already pressed during this event. The rule lives in PressRules so it can
        // be unit tested without Android.
        val allowed = PressRules.isPressAllowed(
            isRunning = state.status == GameStatus.RUNNING,
            currentIndex = state.currentIndex,
            n = state.nValue,
            alreadyPressedThisTick = state.hasPressedMatchThisTick
        )
        if (!allowed) return

        // The generator already knows which events are matches, so there is nothing to recompute.
        val isTrueMatch = currentSequence[state.currentIndex].isMatch

        if (isTrueMatch) {
            _uiState.update { it.copy(correctHits = it.correctHits + 1, hasPressedMatchThisTick = true) }
        } else {
            // Trigger non-offensive animated feedback on incorrect press
            viewModelScope.launch {
                _uiState.update { it.copy(falseAlarms = it.falseAlarms + 1, hasPressedMatchThisTick = true, isWrongGuessFeedbackActive = true) }
                delay(400.milliseconds)
                _uiState.update { it.copy(isWrongGuessFeedbackActive = false) }
            }
        }
    }

    fun cancelGame() {
        if (_uiState.value.status == GameStatus.RUNNING) {
            gameJob?.cancel()
            tts?.stop()
            _uiState.update {
                it.copy(
                    status = GameStatus.IDLE,
                    currentIndex = -1,
                    currentStimulus = null,
                    hasPressedMatchThisTick = false,
                    isWrongGuessFeedbackActive = false
                )
            }
        }
    }

    private fun finishGame() {
        _uiState.update {
            it.copy(
                status = GameStatus.FINISHED,
                currentStimulus = null,
                showSaveScoreDialog = true
            )
        }
    }

    override fun onCleared() {
        gameJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}