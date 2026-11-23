package com.example.memorina

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val cardScope = CoroutineScope(Dispatchers.Main)
    private val selectedCards = mutableListOf<ImageView>()
    private var remainingPairs = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(applicationContext)
        layout.orientation = LinearLayout.VERTICAL
        val gap = (4 * resources.displayMetrics.density).toInt()
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        params.setMargins(gap, gap, gap, gap)

        val faces = listOf(
            R.drawable.squarecat, R.drawable.card_circle, R.drawable.card_triangle,
            R.drawable.card_square, R.drawable.card_diamond, R.drawable.card_star,
            R.drawable.card_heart, R.drawable.card_moon
        )
        val deck = faces.flatMap { face -> listOf(face, face) }.shuffled()
        remainingPairs = faces.size

        val catViews = ArrayList<ImageView>()
        for (face in deck) {
            catViews.add(
                ImageView(applicationContext).apply {
                    setImageResource(R.drawable.card_back)
                    setBackgroundResource(R.drawable.card_surface)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(gap * 2, gap * 2, gap * 2, gap * 2)
                    layoutParams = LinearLayout.LayoutParams(params)
                    tag = face
                    setOnClickListener(colorListener)
                })
        }

        val rows = Array(4) { LinearLayout(applicationContext) }

        var count = 0
        for (view in catViews) {
            val row = count / 4
            rows[row].addView(view)
            count++
        }
        for (row in rows) {
            layout.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(layout)
        ViewCompat.setOnApplyWindowInsetsListener(layout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun closeCards() {
        selectedCards.forEach { card ->
            card.isSelected = false
            card.setImageResource(R.drawable.card_back)
        }
        selectedCards.clear()
    }

    private suspend fun openCards() {
        val match = selectedCards[0].tag == selectedCards[1].tag
        if (match) {
            delay(400L)
            for (card in selectedCards) {
                card.visibility = View.INVISIBLE
                card.isClickable = false
            }
            selectedCards.clear()
            remainingPairs--
            if (remainingPairs == 0) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.game_won_title)
                    .setMessage(R.string.game_won_message)
                    .setPositiveButton(R.string.play_again) { _, _ -> recreate() }
                    .setCancelable(false)
                    .show()
            }
        } else {
            delay(1000L)
            closeCards()
        }
    }

    private val colorListener = View.OnClickListener { view ->
        if (selectedCards.size == 2) return@OnClickListener

        val card = view as ImageView
        if (card in selectedCards) {
            closeCards()
            return@OnClickListener
        }

        card.isSelected = true
        card.setImageResource(card.tag as Int)
        selectedCards.add(card)
        if (selectedCards.size == 2) {
            cardScope.launch { openCards() }
        }
    }

    override fun onDestroy() {
        cardScope.cancel()
        super.onDestroy()
    }
}
