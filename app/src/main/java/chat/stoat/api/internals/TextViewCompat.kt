package chat.stoat.api.internals

import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.Log
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import chat.stoat.api.internals.colour.CSSColours
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.SurfaceColors

object TextViewCompat {
    private fun tryParseDirectColour(colour: String): Int {
        val cssColour = CSSColours[colour]
        if (cssColour != null) {
            return cssColour.toArgb()
        }

        return colour.toColorInt()
    }

    private fun tryParseVariable(tv: TextView, varName: String): Int {
        return when (varName) {
            "--accent" -> MaterialColors.getColor(
                tv,
                androidx.appcompat.R.attr.colorPrimary
            )

            "--foreground" -> MaterialColors.getColor(
                tv,
                com.google.android.material.R.attr.colorOnBackground
            )

            "--background" -> SurfaceColors.SURFACE_0.getColor(tv.context)

            "--error" -> MaterialColors.getColor(
                tv,
                androidx.appcompat.R.attr.colorError
            )

            else -> tv.currentTextColor
        }
    }

    private fun tryParseSetLinearGradient(tv: TextView, gradient: String): Pair<Shader, Int> {
        val stops = mutableListOf<Pair<Float, Int>>()

        val parts = mutableListOf<String>()
        var startIndex = 0
        var openParenthesesCount = 0

        for (i in gradient.indices) {
            when (gradient[i]) {
                '(' -> openParenthesesCount++
                ')' -> openParenthesesCount--
                ',' -> {
                    if (openParenthesesCount == 0) {
                        val part = gradient.substring(startIndex, i).trim()
                        parts.add(part)
                        startIndex = i + 1
                    }
                }
            }
        }

        val lastPart = gradient.substring(startIndex).trim()
        if (lastPart.isNotEmpty()) {
            parts.add(lastPart)
        }

        parts.forEachIndexed { index, part ->
            if (part.startsWith("to") || part.endsWith("deg")) {
                // we don't support any other direction / blocked on compose supporting them
                // TODO could probably emulate this by swapping the values around
            } else {
                val splitPart = part.split(" ")

                val colourPart = splitPart[0]
                val colour = when {
                    colourPart.startsWith("var(") -> {
                        tryParseVariable(
                            tv,
                            colourPart.substringAfter("var(").substringBeforeLast(")")
                        )
                    }

                    else -> BrushCompat.parseFunctionColour(colourPart)?.toArgb()
                        ?: tryParseDirectColour(colourPart)
                }

                val stop = if (splitPart.size == 2) {
                    splitPart[1].removeSuffix("%").toFloat() / 100f
                } else {
                    index.toFloat() / (parts.size - 1)
                }

                stops.add(stop to colour)
            }
        }

        val width = tv.paint.measureText(tv.text.toString())

        return LinearGradient(
            0f,
            0f,
            width,
            0f,
            stops.map { it.second }.toIntArray(),
            stops.map { it.first }.toFloatArray(),
            Shader.TileMode.CLAMP
        ) to stops.first().second
    }

    fun setColourFromRoleColour(tv: TextView, colour: String) {
        when {
            colour.startsWith("var(") -> {
                val varName = colour.substringAfter("var(").substringBeforeLast(")")
                val parsedColour = tryParseVariable(tv, varName)
                tv.setTextColor(parsedColour)
            }

            colour.startsWith("linear-gradient(") || colour.startsWith("repeating-linear-gradient(") -> {
                val gradient = colour.substringAfter("(").substringBeforeLast(")")
                val shader = tryParseSetLinearGradient(tv, gradient)
                tv.paint.shader = shader.first
            }

            else -> {
                try {
                    val directColour = tryParseDirectColour(colour)
                    tv.setTextColor(directColour)
                } catch (e: IllegalArgumentException) {
                    Log.d(
                        "TextViewCompat",
                        "Failed to parse colour $colour, not setting colour"
                    )
                }
            }
        }
    }
}