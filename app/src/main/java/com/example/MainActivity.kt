package com.example

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.model.GameMode
import com.example.model.GameSession
import com.example.model.ShipType
import com.example.ui.ShipDockView

/**
 * ============================================================================
 * DSA WARSHIP: Main Entry Point & Mission Configurator
 * ============================================================================
 * Styled with the clean slate-blue and warm coral aesthetic.
 * Previews all 5 distinct warships, allows mode and grid selection,
 * and launches the game via the prominent "START GAME" action.
 */
class MainActivity : ComponentActivity() {

    private var selectedMode: GameMode = GameMode.VS_AI
    private var selectedGridSize: Int = 10

    private lateinit var radioVsAi: RadioButton
    private lateinit var radioPassAndPlay: RadioButton
    private lateinit var cardVsAi: LinearLayout
    private lateinit var cardPassAndPlay: LinearLayout
    private lateinit var btnSize10: Button
    private lateinit var btnSize8: Button
    private lateinit var btnDsaGuide: Button
    private lateinit var btnStartDeployment: Button
    private lateinit var dockHomePreview: ShipDockView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        radioVsAi = findViewById(R.id.radio_vs_ai)
        radioPassAndPlay = findViewById(R.id.radio_pass_and_play)
        cardVsAi = findViewById(R.id.card_mode_vs_ai)
        cardPassAndPlay = findViewById(R.id.card_mode_pass_and_play)
        btnSize10 = findViewById(R.id.btn_size_10)
        btnSize8 = findViewById(R.id.btn_size_8)
        btnDsaGuide = findViewById(R.id.btn_dsa_guide)
        btnStartDeployment = findViewById(R.id.btn_start_deployment)
        dockHomePreview = findViewById(R.id.dock_home_preview)

        // Showcase the 5 warships on the Home Screen!
        dockHomePreview.setupDock(ShipType.standardFleet())

        updateModeSelection(GameMode.VS_AI)
        updateSizeSelection(10)
    }

    private fun setupListeners() {
        cardVsAi.setOnClickListener { updateModeSelection(GameMode.VS_AI) }
        radioVsAi.setOnClickListener { updateModeSelection(GameMode.VS_AI) }

        cardPassAndPlay.setOnClickListener { updateModeSelection(GameMode.PASS_AND_PLAY) }
        radioPassAndPlay.setOnClickListener { updateModeSelection(GameMode.PASS_AND_PLAY) }

        btnSize10.setOnClickListener { updateSizeSelection(10) }
        btnSize8.setOnClickListener { updateSizeSelection(8) }

        btnDsaGuide.setOnClickListener { showDsaGuideDialog() }

        btnStartDeployment.setOnClickListener {
            // Reset in-memory session with selected game configuration
            GameSession.resetMatch(mode = selectedMode, size = selectedGridSize)

            val intent = Intent(this, DeploymentActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateModeSelection(mode: GameMode) {
        selectedMode = mode
        radioVsAi.isChecked = (mode == GameMode.VS_AI)
        radioPassAndPlay.isChecked = (mode == GameMode.PASS_AND_PLAY)
    }

    private fun updateSizeSelection(size: Int) {
        selectedGridSize = size
        if (size == 10) {
            btnSize10.setBackgroundResource(R.drawable.button_random_pill)
            btnSize10.setTextColor(getColor(R.color.white))
            btnSize8.setBackgroundResource(R.drawable.button_secondary_bg)
            btnSize8.setTextColor(getColor(R.color.text_subtitle))
        } else {
            btnSize8.setBackgroundResource(R.drawable.button_random_pill)
            btnSize8.setTextColor(getColor(R.color.white))
            btnSize10.setBackgroundResource(R.drawable.button_secondary_bg)
            btnSize10.setTextColor(getColor(R.color.text_subtitle))
        }
    }

    private fun showDsaGuideDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_dsa_guide, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.btn_close_dsa_guide).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
