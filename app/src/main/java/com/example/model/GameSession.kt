package com.example.model

/**
 * In-memory Game Session manager to hold active match configuration
 * between DeploymentActivity and GameActivity, avoiding Android IPC TransactionTooLargeException.
 */
object GameSession {
    var gameMode: GameMode = GameMode.VS_AI
    var gridSize: Int = 10
    var player1Name: String = "Player 1"
    var player2Name: String = "Player 2"

    var player1Board: Board = Board(10)
    var player2Board: Board = Board(10)
    var aiPlayer: AIPlayer? = null

    fun resetMatch(mode: GameMode, size: Int = 10) {
        gameMode = mode
        gridSize = size
        player1Name = "Player 1"
        player2Name = if (mode == GameMode.VS_AI) "AI Commander" else "Player 2"

        player1Board = Board(size)
        player2Board = Board(size)

        aiPlayer = if (mode == GameMode.VS_AI) {
            AIPlayer(gridSize = size, name = player2Name)
        } else {
            null
        }
    }
}
