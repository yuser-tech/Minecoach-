package com.example.game

import kotlin.random.Random

enum class Difficulty(val label: String, val rows: Int, val cols: Int, val minesCount: Int) {
    EASY("Easy (8x8)", 8, 8, 10),
    MEDIUM("Medium (10x10)", 10, 10, 16),
    HARD("Hard (12x12)", 12, 12, 28)
}

enum class GameStatus {
    IDLE,
    PLAYING,
    WON,
    LOST
}

data class Cell(
    val row: Int,
    val col: Int,
    var isMine: Boolean = false,
    var isRevealed: Boolean = false,
    var isFlagged: Boolean = false,
    var adjacentMines: Int = 0
) {
    fun copyState(): Cell {
        return Cell(row, col, isMine, isRevealed, isFlagged, adjacentMines)
    }
}

class MinesweeperGame(
    val difficulty: Difficulty
) {
    val rows = difficulty.rows
    val cols = difficulty.cols
    val totalMines = difficulty.minesCount

    var status = GameStatus.IDLE
        private set

    var board: List<List<Cell>> = List(rows) { r ->
        List(cols) { c -> Cell(row = r, col = c) }
    }
        private set

    var minesLeft = totalMines
        private set

    // Whether the board mines have been generated yet
    var isMinesGenerated = false
        private set

    /**
     * Handles a cell click to reveal it.
     * Returns true if there was a state change.
     */
    fun revealCell(row: Int, col: Int): Boolean {
        if (status == GameStatus.LOST || status == GameStatus.WON) return false
        val cell = board[row][col]
        if (cell.isRevealed || cell.isFlagged) return false

        if (status == GameStatus.IDLE) {
            status = GameStatus.PLAYING
        }

        // First click protection
        if (!isMinesGenerated) {
            generateMines(row, col)
            isMinesGenerated = true
        }

        if (cell.isMine) {
            // Clicked a mine! Game Over.
            cell.isRevealed = true
            status = GameStatus.LOST
            revealAllMines()
            return true
        }

        // Perform cascade reveal
        cascadeReveal(row, col)

        // Check if won
        checkWinCondition()

        return true
    }

    /**
     * Toggles a flag on an unrevealed cell.
     */
    fun toggleFlag(row: Int, col: Int): Boolean {
        if (status == GameStatus.LOST || status == GameStatus.WON) return false
        val cell = board[row][col]
        if (cell.isRevealed) return false

        cell.isFlagged = !cell.isFlagged
        minesLeft = totalMines - countFlaggedMines()

        if (status == GameStatus.IDLE) {
            status = GameStatus.PLAYING
        }

        return true
    }

    /**
     * Generates mines while ensuring that the first clicked cell
     * and its immediate neighbors are guaranteed to be safe (no mines).
     */
    private fun generateMines(startRow: Int, startCol: Int) {
        val safeCells = mutableSetOf<Pair<Int, Int>>()
        // The first clicked cell and its 8 adjacent neighbors are safe
        for (dr in -1..1) {
            for (dc in -1..1) {
                val nr = startRow + dr
                val nc = startCol + dc
                if (nr in 0 until rows && nc in 0 until cols) {
                    safeCells.add(Pair(nr, nc))
                }
            }
        }

        val allPositions = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (!safeCells.contains(Pair(r, c))) {
                    allPositions.add(Pair(r, c))
                }
            }
        }

        // If there are not enough positions (which shouldn't happen with our bounds), fallback to just the clicked cell
        if (allPositions.size < totalMines) {
            allPositions.clear()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (r != startRow || c != startCol) {
                        allPositions.add(Pair(r, c))
                    }
                }
            }
        }

        // Place mines randomly
        allPositions.shuffle()
        val minePositions = allPositions.take(totalMines).toSet()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                board[r][c].isMine = minePositions.contains(Pair(r, c))
            }
        }

        // Calculate adjacency numbers
        calculateAdjacency()
    }

    private fun calculateAdjacency() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = board[r][c]
                if (cell.isMine) continue

                var count = 0
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols) {
                            if (board[nr][nc].isMine) {
                                count++
                            }
                        }
                    }
                }
                cell.adjacentMines = count
            }
        }
    }

    private fun cascadeReveal(startRow: Int, startCol: Int) {
        val queue = mutableListOf<Pair<Int, Int>>()
        queue.add(Pair(startRow, startCol))
        board[startRow][startCol].isRevealed = true

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeAt(0)
            val cell = board[r][c]

            // If it has 0 adjacent mines, expand to neighbors
            if (cell.adjacentMines == 0) {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols) {
                            val neighbor = board[nr][nc]
                            if (!neighbor.isRevealed && !neighbor.isFlagged && !neighbor.isMine) {
                                neighbor.isRevealed = true
                                queue.add(Pair(nr, nc))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkWinCondition() {
        var unrevealedSafeCells = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = board[r][c]
                if (!cell.isMine && !cell.isRevealed) {
                    unrevealedSafeCells++
                }
            }
        }

        if (unrevealedSafeCells == 0) {
            status = GameStatus.WON
            // Flag all remaining mines automatically for the player
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val cell = board[r][c]
                    if (cell.isMine) {
                        cell.isFlagged = true
                    }
                }
            }
            minesLeft = 0
        }
    }

    private fun revealAllMines() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = board[r][c]
                if (cell.isMine) {
                    cell.isRevealed = true
                }
            }
        }
    }

    private fun countFlaggedMines(): Int {
        var count = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (board[r][c].isFlagged) {
                    count++
                }
            }
        }
        return count
    }

    /**
     * Formats the grid into a structured text layout for the Gemini API.
     */
    fun getBoardRepresentation(): String {
        val sb = StringBuilder()
        
        // Column Index Header
        sb.append("   ")
        for (c in 0 until cols) {
            sb.append(String.format("%2d", c))
        }
        sb.append("\n")

        // Board grid
        for (r in 0 until rows) {
            sb.append(String.format("%2d ", r))
            for (c in 0 until cols) {
                val cell = board[r][c]
                val symbol = when {
                    cell.isFlagged -> "F"
                    !cell.isRevealed -> "_"
                    cell.isMine -> "*"
                    else -> cell.adjacentMines.toString()
                }
                sb.append(String.format("%2s", symbol))
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * Deep copy the current board state for ViewModel updates.
     */
    fun getBoardCopy(): List<List<Cell>> {
        return board.map { rowList -> rowList.map { cell -> cell.copyState() } }
    }
}
