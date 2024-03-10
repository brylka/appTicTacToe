package com.example.apptictactoe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {

    private lateinit var gameIdEditText: EditText
    private lateinit var joinGameButton: Button
    private lateinit var newGameButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        gameIdEditText = findViewById(R.id.gameIdEditText)
        joinGameButton = findViewById(R.id.joinGameButton)
        newGameButton = findViewById(R.id.newGameButton)

        joinGameButton.setOnClickListener {
            val gameId = gameIdEditText.text.toString().toIntOrNull()
            if (gameId != null) {
                startMainActivity(gameId)
            } else {
                gameIdEditText.error = "Please enter a valid Game ID"
            }
        }

        newGameButton.setOnClickListener {
            startMainActivity(null)
        }
    }

    private fun startMainActivity(gameId: Int?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("GAME_ID", gameId)
        startActivity(intent)
    }
}
