package com.example.model

import java.io.Serializable

enum class GameMode(val title: String) : Serializable {
    VS_AI("1-Player vs Computer (AI)"),
    PASS_AND_PLAY("2-Player (Pass & Play)")
}
