package com.example.pokinfo.adapter.home.detail.evolution

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.pokinfo.data.models.database.pokemon.EvolutionStage
import com.google.android.material.color.MaterialColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


/**  EvolutionView is a custom Android view designed to
 * visually represent the evolutionary stages of a Pokémon.
 * This view dynamically calculates the necessary space based on the
 * Pokémon's evolution chain and displays each stage along with transition
 * arrows indicating the evolution path. */
class EvolutionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var pokemon: EvolutionStage? = null
    private val imageLoader = ImageLoader(context)
    private val images = mutableMapOf<String, Drawable>()

    private val textHeight = 40f
    private val textPadding = 20f
    private val totalTextHeight = textHeight + textPadding
    private val imageHeight = 150
    private val additionalBottomPadding = 50
    private var currentViewHeight = 0

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color =
            MaterialColors.getColor(rootView, com.google.android.material.R.attr.colorOnBackground)
        textSize = textHeight
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    // Updates the view height based on the current evolutions
    private fun updateViewHeight() {
        pokemon?.let {
            val requiredHeight = calculateRequiredHeight(it)
            if (layoutParams.height != requiredHeight) {
                layoutParams.height = requiredHeight
                requestLayout()
            }
        }
    }

    // calculates the required height of the view based on the evolution lines
    private fun calculateRequiredHeight(pokemon: EvolutionStage, currentDepth: Int = 0): Int {
        val totalHeight = imageHeight + totalTextHeight.toInt()

        if (pokemon.nextEvolutions.isEmpty()) {
            return totalHeight + additionalBottomPadding
        }

        val evolutionHeights = pokemon.nextEvolutions.map {
            calculateRequiredHeight(it, currentDepth + 1)
        }

        return when {
            pokemon.nextEvolutions.size == 1 -> totalHeight + evolutionHeights.maxOrNull()!! + 330

            pokemon.nextEvolutions.size > 4 -> {
                val topRowHeight =
                    evolutionHeights.subList(0, pokemon.nextEvolutions.size / 2).maxOrNull() ?: 0
                val bottomRowHeight = evolutionHeights.subList(
                    pokemon.nextEvolutions.size / 2, pokemon.nextEvolutions.size
                ).maxOrNull() ?: 0
                totalHeight + topRowHeight + bottomRowHeight + additionalBottomPadding + totalTextHeight.toInt() + 100
            }

            else -> {
                totalHeight + evolutionHeights.maxOrNull()!! + 330
            }
        }
    }

    // sets the root pokemon which will start the evolution tree
    fun setPokemon(pokemon: EvolutionStage) {
        this.pokemon = pokemon
        loadImages(pokemon)
        invalidate()
        updateViewHeight()
    }


    // Loads images asynchronously and updates the view
    private fun loadImages(pokemon: EvolutionStage) {
        val request =
            ImageRequest.Builder(context).data(pokemon.pokemonInfos.imageUrl).target { result ->
                images[pokemon.pokemonInfos.name] = result
                invalidate()
            }.build()
        imageLoader.enqueue(request)

        for (evolution in pokemon.nextEvolutions) {
            loadImages(evolution)
        }
    }

    // main drawing method on start
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        currentViewHeight = 0
        pokemon?.let {
            val yOffset = if (it.nextEvolutions.size > 4) height / 2f else 100f
            drawPokemon(canvas, it, width / 2f, yOffset)
            updateViewHeight()
        }
    }

    // Draw a pokemon and its evolutions
    private fun drawPokemon(canvas: Canvas, pokemon: EvolutionStage, x: Float, y: Float) {
        images[pokemon.pokemonInfos.name]?.let { drawable ->
            val imageWidth = 150
            val imageX = x - imageWidth / 2
            val imageY = y - imageHeight / 2
            // will detect how the evolution tree will look like and draw it due to that
            handleEvolutions(canvas, pokemon, x, y + imageHeight + totalTextHeight)
            drawable.setBounds(
                imageX.toInt(),
                imageY.toInt(),
                (imageX + imageWidth).toInt(),
                (imageY + imageHeight).toInt()
            )
            drawable.draw(canvas)
            canvas.drawText(
                pokemon.pokemonInfos.name, x, y + imageHeight / 2 + totalTextHeight, textPaint
            )
        }
    }

    // Handles the drawing of evolution arrows and calls drawPokemon for each evolution
    private fun handleEvolutions(canvas: Canvas, pokemon: EvolutionStage, x: Float, startY: Float) {
        if (pokemon.nextEvolutions.isEmpty()) return

        val spacing = 300
        if (pokemon.nextEvolutions.size == 1) {
            val arrowText =
                pokemon.nextEvolutions.first().evolutionDetails?.minLevel?.toString() ?: ""
            val endY = startY + spacing
            drawArrow(canvas, x, startY - 40, x, endY - 80, arrowText)
            drawPokemon(canvas, pokemon.nextEvolutions.first(), x, endY)
        } else if (pokemon.nextEvolutions.size <= 4) {
            drawSingleRowEvolutions(canvas, pokemon.nextEvolutions, x, startY, startY + 60)
        } else {
            drawMultiLevelEvolutions(canvas, pokemon.nextEvolutions, x, height / 2f)
        }
    }

    // Mainly used for one Pokemon: "Eevee" which has 8 possible after evolutions
    // draws the first half of evolutions on top of the root pokemon the other half down below
    private fun drawMultiLevelEvolutions(
        canvas: Canvas, evolutions: List<EvolutionStage>, x: Float, y: Float
    ) {
        val dp8 = context.resources.displayMetrics.density * 8
        val sectionWidth = (width / 4.0f) - dp8

        // Calc positions for the top row of evolutions
        val halfIndex = (evolutions.size + 1) / 2
        val offsetXTop = x - ((halfIndex - 1) * (sectionWidth + dp8)) / 2

        // Draw top row and arrows + texts
        for (i in 0 until halfIndex) {
            val evolution = evolutions[i]
            val nextX = offsetXTop + i * (sectionWidth + dp8)
            val nextY = dp8 + 80
            val arrowText = evolution.evolutionDetails?.minLevel?.toString() ?: ""
            drawArrow(
                canvas,
                x,
                y - imageHeight / 2,
                nextX,
                nextY + imageHeight / 2 + totalTextHeight + 40,
                arrowText
            )
            drawPokemon(canvas, evolution, nextX, nextY)
        }

        // Calc positions for the bottom row
        val offsetXBottom = x - ((evolutions.size - halfIndex - 1) * (sectionWidth + dp8)) / 2
        // Draw bottom row + arrows + text
        for (i in halfIndex until evolutions.size) {
            val evolution = evolutions[i]
            val nextX = offsetXBottom + (i - halfIndex) * (sectionWidth + dp8)
            val nextY = height - dp8 - imageHeight - textHeight + 60
            val arrowText = evolution.evolutionDetails?.minLevel?.toString() ?: ""
            drawArrow(
                canvas, x, y + totalTextHeight + 20 + imageHeight / 2, nextX, nextY - 80, arrowText
            )
            drawPokemon(canvas, evolution, nextX, nextY - 20)
        }
    }

    // Draw an arrow between the 2 pokemon and if if evolves on a specific level shows the level next to the arrow
    private fun drawArrow(
        canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float, text: String
    ) {

        canvas.drawLine(startX, startY, endX, endY, paint)
        val levelText = if (text.isEmpty()) "" else "Level $text"

        // Calculate the direction (angle) of the line
        val dx = endX - startX
        val dy = endY - startY
        val angle = atan2(dy, dx)

        // Draw the arrow head
        for (sign in listOf(-1, 1)) {
            canvas.drawLine(
                endX,
                endY,
                endX - 20 * cos(angle - sign * PI / 4).toFloat(),
                endY - 20 * sin(angle - sign * PI / 4).toFloat(),
                paint
            )
        }

        // Calculate text position
        val textOffset = 20
        val textX = endX + textOffset * cos(angle)
        val textY = (startY + endY) / 2 - (textPaint.descent() + textPaint.ascent()) / 2

        // Draw the text with the calculated position
        canvas.drawText(levelText, textX + 100, textY, textPaint)
    }

    // Draws Pokemon Evolutions when a pokemon evolves to more than 1 but less than 4 pokemon
    // draws all after evolutions in one row
    private fun drawSingleRowEvolutions(
        canvas: Canvas, evolutions: List<EvolutionStage>, x: Float, startY: Float, initialY: Float
    ) {
        val numEvolutions = evolutions.size
        val sectionWidth = width / (numEvolutions + 1)

        for (i in 0 until numEvolutions) {
            val nextX = x + (i + 1) * sectionWidth - width / 2
            val endY = initialY + 200

            val evolutionDetails = evolutions[i].evolutionDetails?.minLevel?.toString() ?: ""

            drawArrow(
                canvas = canvas,
                startX = x,
                startY = startY,
                endX = nextX,
                endY = endY - imageHeight / 2,
                text = evolutionDetails
            )
            drawPokemon(canvas, evolutions[i], nextX, endY)
        }
    }
}