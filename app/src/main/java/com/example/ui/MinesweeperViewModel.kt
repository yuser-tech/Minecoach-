package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiCoachRepository
import com.example.game.Cell
import com.example.game.Difficulty
import com.example.game.GameStatus
import com.example.game.MinesweeperGame
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CoachMessage(
    val sender: String, // "Captain Sandy" or "Recruit"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MinesweeperViewModel : ViewModel() {

    private var game = MinesweeperGame(Difficulty.EASY)

    // Game state flows
    private val _boardState = MutableStateFlow<List<List<Cell>>>(emptyList())
    val boardState: StateFlow<List<List<Cell>>> = _boardState.asStateFlow()

    private val _gameStatus = MutableStateFlow(GameStatus.IDLE)
    val gameStatus: StateFlow<GameStatus> = _gameStatus.asStateFlow()

    private val _minesLeft = MutableStateFlow(0)
    val minesLeft: StateFlow<Int> = _minesLeft.asStateFlow()

    private val _totalMines = MutableStateFlow(0)
    val totalMines: StateFlow<Int> = _totalMines.asStateFlow()

    private val _difficulty = MutableStateFlow(Difficulty.EASY)
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    private val _timerValue = MutableStateFlow(0)
    val timerValue: StateFlow<Int> = _timerValue.asStateFlow()

    // Mode: Digging vs Flagging
    private val _isFlagMode = MutableStateFlow(false)
    val isFlagMode: StateFlow<Boolean> = _isFlagMode.asStateFlow()

    // Local win/loss counters
    private val _winsCount = MutableStateFlow(0)
    val winsCount: StateFlow<Int> = _winsCount.asStateFlow()

    private val _lossesCount = MutableStateFlow(0)
    val lossesCount: StateFlow<Int> = _lossesCount.asStateFlow()

    // Coach state flows
    private val _isCoachLoading = MutableStateFlow(false)
    val isCoachLoading: StateFlow<Boolean> = _isCoachLoading.asStateFlow()

    private val _coachChatHistory = MutableStateFlow<List<CoachMessage>>(emptyList())
    val coachChatHistory: StateFlow<List<CoachMessage>> = _coachChatHistory.asStateFlow()

    // Active message shown in speech bubble or popover
    private val _latestCoachAdvice = MutableStateFlow<String>("")
    val latestCoachAdvice: StateFlow<String> = _latestCoachAdvice.asStateFlow()

    private var timerJob: Job? = null

    init {
        startNewGame(Difficulty.EASY)
    }

    fun startNewGame(selectedDifficulty: Difficulty) {
        stopTimer()
        _timerValue.value = 0
        _difficulty.value = selectedDifficulty
        
        game = MinesweeperGame(selectedDifficulty)
        updateGameStates()

        // Friendly welcoming message
        val welcomeMsg = "Ready for field scanning, recruit! Standard procedure is to clear a safe landing zone by clicking anywhere on the grid. I'll cover your flank! Click 'Coach Scan' anytime for strategic assistance."
        _latestCoachAdvice.value = welcomeMsg
        _coachChatHistory.value = listOf(CoachMessage("Captain Sandy", welcomeMsg))
    }

    fun toggleFlagMode() {
        _isFlagMode.value = !_isFlagMode.value
    }

    fun setFlagMode(active: Boolean) {
        _isFlagMode.value = active
    }

    fun onCellClicked(row: Int, col: Int) {
        val currentStatus = game.status
        val cell = game.board[row][col]
        if (cell.isRevealed) return

        val stateChanged = if (_isFlagMode.value) {
            game.toggleFlag(row, col)
        } else {
            game.revealCell(row, col)
        }

        if (stateChanged) {
            // Start timer on first playing action
            if (currentStatus == GameStatus.IDLE && game.status == GameStatus.PLAYING) {
                startTimer()
            }
            handleStatusChanges(currentStatus, game.status)
            updateGameStates()
        }
    }

    fun onCellLongPressed(row: Int, col: Int) {
        val currentStatus = game.status
        val stateChanged = game.toggleFlag(row, col)
        if (stateChanged) {
            if (currentStatus == GameStatus.IDLE && game.status == GameStatus.PLAYING) {
                startTimer()
            }
            handleStatusChanges(currentStatus, game.status)
            updateGameStates()
        }
    }

    private fun handleStatusChanges(oldStatus: GameStatus, newStatus: GameStatus) {
        if (oldStatus != newStatus) {
            if (newStatus == GameStatus.WON || newStatus == GameStatus.LOST) {
                stopTimer()
                
                if (newStatus == GameStatus.WON) {
                    _winsCount.value += 1
                    val winMsg = "Outstanding job, recruit! Mission accomplished. We've neutralized all mines and secured the perimeter! Take a breather, you've earned it."
                    postCoachMessage(winMsg)
                } else if (newStatus == GameStatus.LOST) {
                    _lossesCount.value += 1
                    val loseMsg = "Man down! We hit a mine. Keep your chin up, recruit—demolition is a dangerous business. Analyze the layout, learn from this experience, and let's go again! I'm here to coach you."
                    postCoachMessage(loseMsg)
                }
            }
        }
    }

    private fun postCoachMessage(msg: String) {
        _latestCoachAdvice.value = msg
        _coachChatHistory.value = _coachChatHistory.value + CoachMessage("Captain Sandy", msg)
    }

    fun askCoachForHelp() {
        if (_isCoachLoading.value) return

        val boardRep = game.getBoardRepresentation()
        val totalM = game.totalMines
        val leftM = game.minesLeft

        // Add recruit question to history
        val recruitQuestion = "Requesting field scan advice, Captain!"
        _coachChatHistory.value = _coachChatHistory.value + CoachMessage("Recruit", recruitQuestion)

        _isCoachLoading.value = true
        _latestCoachAdvice.value = "Analyzing field coordinates... Stand by, recruit!"

        viewModelScope.launch {
            try {
                val advice = GeminiCoachRepository.getCoachHint(boardRep, totalM, leftM)
                _latestCoachAdvice.value = advice
                _coachChatHistory.value = _coachChatHistory.value + CoachMessage("Captain Sandy", advice)
            } catch (e: Exception) {
                val errorMsg = "Transmission scrambled! Captain Sandy is out of range. Check your credentials in the Secrets panel, or verify internet connection."
                _latestCoachAdvice.value = errorMsg
                _coachChatHistory.value = _coachChatHistory.value + CoachMessage("Captain Sandy", errorMsg)
            } finally {
                _isCoachLoading.value = false
            }
        }
    }

    fun clearCoachAdvice() {
        _latestCoachAdvice.value = ""
    }

    fun resetStats() {
        _winsCount.value = 0
        _lossesCount.value = 0
    }

    private fun updateGameStates() {
        _boardState.value = game.getBoardCopy()
        _gameStatus.value = game.status
        _minesLeft.value = game.minesLeft
        _totalMines.value = game.totalMines
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timerValue.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
