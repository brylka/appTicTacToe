package com.example.apptictactoe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var url: String = "http://10.0.2.2:5000" // Use 10.0.2.2 for Android emulator
    private lateinit var buttons: List<Button>
    private var gameId: Int? = null
    private val client = OkHttpClient()
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttons = listOf(
            findViewById(R.id.button0),
            findViewById(R.id.button1),
            findViewById(R.id.button2),
            findViewById(R.id.button3),
            findViewById(R.id.button4),
            findViewById(R.id.button5),
            findViewById(R.id.button6),
            findViewById(R.id.button7),
            findViewById(R.id.button8)
        )

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                onCellClicked(it, index)
            }
        }

        val gameIdExtra = intent.getIntExtra("GAME_ID", -1)
        if (gameIdExtra != -1) {
            gameId = gameIdExtra
            getGameState(gameId!!)
        } else {
            createNewGame()
        }

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                gameId?.let {
                    getGameState(it)
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)

    }

    private fun createNewGame() {
        val request = Request.Builder()
            .url("$url/game")
            .post("".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = response.body?.string()
                Log.d("CreateNewGame", "Response from server: $jsonResponse")
                gameId = jsonResponse?.let { parseGameId(it) }
                runOnUiThread {
                    Toast.makeText(applicationContext, "New game started with ID: $gameId", Toast.LENGTH_SHORT).show()
                    Log.d("CreateNewGame", "New game started with ID: $gameId")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }

    private fun onCellClicked(view: View, cellIndex: Int) {
        if (gameId == null) {
            Toast.makeText(this, "Game has not been started yet.", Toast.LENGTH_SHORT).show()
            return
        }
        makeMoveToServer(gameId!!, cellIndex)
    }

    private fun makeMoveToServer(gameId: Int, cellIndex: Int) {
        val json = "{\"cellIndex\":$cellIndex}"
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$url/game/$gameId/move")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        getGameState(gameId)
                    } else {
                        Toast.makeText(applicationContext, "Failed to make a move", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }

    private fun getGameState(gameId: Int) {
        val request = Request.Builder()
            .url("$url/game/$gameId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = response.body?.string()
                Log.d("GameState", "Response from server: $jsonResponse")
                jsonResponse?.let {
                    updateUIWithGameState(it)
                }

            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }

    private fun updateUIWithGameState(jsonResponse: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(jsonResponse)
                val moves = jsonObject.getString("moves")
                for (button in buttons) {
                    button.text = ""
                }
                for (i in moves.indices step 2) {
                    val cellIndex = moves[i].toString().toInt()
                    val playerSymbol = moves[i + 1].toString()
                    buttons[cellIndex].text = playerSymbol
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun parseGameId(jsonResponse: String): Int? {
        return try {
            val jsonObject = JSONObject(jsonResponse)
            jsonObject.optInt("game_id", -1).takeIf { it != -1 }
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onResume() {
        super.onResume()
        handler.post(runnable)
    }

}
