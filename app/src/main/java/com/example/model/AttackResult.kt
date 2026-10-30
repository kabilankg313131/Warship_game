package com.example.model

import java.io.Serializable

/**
 * Result of executing an attack on a board coordinate.
 */
sealed class AttackResult : Serializable {
    abstract val coordinate: Coordinate

    data class Miss(
        override val coordinate: Coordinate
    ) : AttackResult()

    data class Hit(
        override val coordinate: Coordinate,
        val ship: Ship
    ) : AttackResult()

    data class Sunk(
        override val coordinate: Coordinate,
        val ship: Ship,
        val isGameOver: Boolean
    ) : AttackResult()

    data class AlreadyAttacked(
        override val coordinate: Coordinate
    ) : AttackResult()

    data class Invalid(
        override val coordinate: Coordinate
    ) : AttackResult()
}
