package chat.stoat.api.internals

import android.util.Log
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.core.graphics.toColorInt
import chat.stoat.api.internals.colour.CSSColours

fun Brush.Companion.solidColor(colour: Color) = SolidColor(colour)

object BrushCompat {
    @Composable
    private fun parseLinearGradient(gradient: String): Brush {
        val stops = mutableListOf<Pair<Float, Color>>()

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
                        parseVarToColour(
                            colourPart.substringAfter("var(").substringBeforeLast(")")
                        )
                    }

                    else -> parseFunctionColour(colourPart) ?: parseColourName(colourPart)
                }

                val stop = if (splitPart.size == 2) {
                    splitPart[1].removeSuffix("%").toFloat() / 100f
                } else {
                    index.toFloat() / (parts.size - 1)
                }

                stops.add(stop to colour)
            }
        }

        return linearGradient(
            colorStops = stops.toTypedArray()
        )
    }

    @Composable
    private fun parseRadialGradient(gradient: String): Brush {
        val stops = mutableListOf<Pair<Float, Color>>()

        val parts = mutableListOf<String>()
        var startIndex = 0
        var openParenthesesCount = 0

        // Split the gradient string into individual components
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

        // Parse color stops
        parts.drop(1).forEachIndexed { index, part ->
            val splitPart = part.split(" ")
            val colorPart = splitPart[0]
            val color = when {
                colorPart.startsWith("var(") -> {
                    parseVarToColour(
                        colorPart.substringAfter("var(").substringBeforeLast(")")
                    )
                }

                else -> parseFunctionColour(colorPart) ?: parseColourName(colorPart)
            }

            val stop = if (splitPart.size == 2) {
                splitPart[1].removeSuffix("%").toFloat() / 100f
            } else {
                index.toFloat() / (parts.size - 2)
            }

            stops.add(stop to color)
        }

        return Brush.radialGradient(
            colorStops = stops.toTypedArray()
        )
    }

    fun parseFunctionColour(colourString: String): Color? {
        val cleanedString = colourString.trim()

        return try {
            if (cleanedString.startsWith("rgb(")) {
                parseRGBColour(cleanedString)
            } else if (cleanedString.startsWith("rgba(")) {
                parseRGBAColour(cleanedString)
            } else {
                throw IllegalArgumentException("Invalid colour format: $colourString")
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRGBColour(rgbString: String): Color {
        val colourParts = rgbString.removePrefix("rgb(")
            .removeSuffix(")")
            .split(",")
            .map { it.trim().toInt() }

        val red = colourParts[0] / 255.0f
        val green = colourParts[1] / 255.0f
        val blue = colourParts[2] / 255.0f

        return Color(red, green, blue)
    }

    private fun parseRGBAColour(rgbaString: String): Color {
        val colourParts = rgbaString.removePrefix("rgba(")
            .removeSuffix(")")
            .split(",")
            .map { it.trim() }

        val red = colourParts[0].toInt() / 255.0f
        val green = colourParts[1].toInt() / 255.0f
        val blue = colourParts[2].toInt() / 255.0f
        val alpha = colourParts[3].removeSuffix("%").toFloat() / 100.0f

        return Color(red, green, blue, alpha)
    }

    @Composable
    private fun parseVarToColour(varName: String): Color {
        return when (varName) {
            "--accent" -> MaterialTheme.colorScheme.primary
            "--foreground" -> MaterialTheme.colorScheme.onBackground
            "--background" -> MaterialTheme.colorScheme.background
            "--error" -> MaterialTheme.colorScheme.error
            else -> LocalContentColor.current
        }
    }

    @Composable
    private fun parseVar(varName: String): Brush {
        return Brush.solidColor(parseVarToColour(varName))
    }

    @Composable
    private fun parseColourName(colour: String): Color {
        return try {
            val cssColour = CSSColours[colour]
            if (cssColour != null) {
                return cssColour
            }

            Color(colour.toColorInt())
        } catch (e: IllegalArgumentException) {
            Log.d(
                "BrushCompat",
                "Failed to parse colour $colour, falling back to LocalContentColor.current"
            )
            LocalContentColor.current
        }
    }

    @Composable
    fun parseColour(colour: String): Brush {
        if (colour.isEmpty()) {
            return Brush.solidColor(Color.Unspecified)
        }

        when {
            colour.startsWith("var(") -> {
                Log.d(
                    "BrushCompat",
                    "Parsing variable $colour"
                )
                return parseVar(
                    colour.substringAfter("var(").substringBeforeLast(")")
                )
            }

            colour.startsWith("linear-gradient(") || colour.startsWith("repeating-linear-gradient(") -> {
                return parseLinearGradient(
                    colour
                        .substringAfter("repeating-")
                        .substringAfter("linear-gradient(")
                        .substringBeforeLast(")")
                )
            }

            colour.startsWith("radial-gradient(") || colour.startsWith("repeating-radial-gradient(") -> {
                return parseRadialGradient(
                    colour
                        .substringAfter("repeating-")
                        .substringAfter("radial-gradient(")
                        .substringBeforeLast(")")
                )
            }


            else -> {
                return Brush.solidColor(parseColourName(colour))
            }
        }
    }
}

/**
 * Like [BrushCompat] but does not require `@Composable` scope.
 * Instead you must initialise it with the colours you want to use.
 */
class InstancedBrushCompat(
    val defaultColour: Color,
    val primaryColour: Color,
    val onBackgroundColour: Color,
    val backgroundColour: Color,
    val errorColour: Color
) {
    private fun parseLinearGradient(gradient: String): Brush {
        val stops = mutableListOf<Pair<Float, Color>>()

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
                        parseVarToColour(
                            colourPart.substringAfter("var(").substringBeforeLast(")")
                        )
                    }

                    else -> parseFunctionColour(colourPart) ?: parseColourName(colourPart)
                }

                val stop = if (splitPart.size == 2) {
                    splitPart[1].removeSuffix("%").toFloat() / 100f
                } else {
                    index.toFloat() / (parts.size - 1)
                }

                stops.add(stop to colour)
            }
        }

        return linearGradient(
            colorStops = stops.toTypedArray()
        )
    }

    private fun parseRadialGradient(gradient: String): Brush {
        val stops = mutableListOf<Pair<Float, Color>>()

        val parts = mutableListOf<String>()
        var startIndex = 0
        var openParenthesesCount = 0

        // Split the gradient string into individual components
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

        // Parse color stops
        parts.drop(1).forEachIndexed { index, part ->
            val splitPart = part.split(" ")
            val colorPart = splitPart[0]
            val color = when {
                colorPart.startsWith("var(") -> {
                    parseVarToColour(
                        colorPart.substringAfter("var(").substringBeforeLast(")")
                    )
                }

                else -> parseFunctionColour(colorPart) ?: parseColourName(colorPart)
            }

            val stop = if (splitPart.size == 2) {
                splitPart[1].removeSuffix("%").toFloat() / 100f
            } else {
                index.toFloat() / (parts.size - 2)
            }

            stops.add(stop to color)
        }

        return Brush.radialGradient(
            colorStops = stops.toTypedArray()
        )
    }

    fun parseFunctionColour(colourString: String): Color? {
        val cleanedString = colourString.trim()

        return try {
            if (cleanedString.startsWith("rgb(")) {
                parseRGBColour(cleanedString)
            } else if (cleanedString.startsWith("rgba(")) {
                parseRGBAColour(cleanedString)
            } else {
                throw IllegalArgumentException("Invalid colour format: $colourString")
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRGBColour(rgbString: String): Color {
        val colourParts = rgbString.removePrefix("rgb(")
            .removeSuffix(")")
            .split(",")
            .map { it.trim().toInt() }

        val red = colourParts[0] / 255.0f
        val green = colourParts[1] / 255.0f
        val blue = colourParts[2] / 255.0f

        return Color(red, green, blue)
    }

    private fun parseRGBAColour(rgbaString: String): Color {
        val colourParts = rgbaString.removePrefix("rgba(")
            .removeSuffix(")")
            .split(",")
            .map { it.trim() }

        val red = colourParts[0].toInt() / 255.0f
        val green = colourParts[1].toInt() / 255.0f
        val blue = colourParts[2].toInt() / 255.0f
        val alpha = colourParts[3].removeSuffix("%").toFloat() / 100.0f

        return Color(red, green, blue, alpha)
    }

    private fun parseVarToColour(varName: String): Color {
        return when (varName) {
            "--accent" -> primaryColour
            "--foreground" -> onBackgroundColour
            "--background" -> backgroundColour
            "--error" -> errorColour
            else -> defaultColour
        }
    }

    private fun parseVar(varName: String): Brush {
        return SolidColor(parseVarToColour(varName))
    }

    private fun parseColourName(colour: String): Color {
        return try {
            val cssColour = CSSColours[colour]
            if (cssColour != null) {
                return cssColour
            }

            Color(colour.toColorInt())
        } catch (e: IllegalArgumentException) {
            Log.d(
                "BrushCompat",
                "Failed to parse colour $colour, falling back to LocalContentColor.current"
            )
            defaultColour
        }
    }

    fun parseColour(colour: String): Brush {
        if (colour.isEmpty()) {
            return Brush.solidColor(Color.Unspecified)
        }

        when {
            colour.startsWith("var(") -> {
                Log.d(
                    "BrushCompat",
                    "Parsing variable $colour"
                )
                return parseVar(
                    colour.substringAfter("var(").substringBeforeLast(")")
                )
            }

            colour.startsWith("linear-gradient(") || colour.startsWith("repeating-linear-gradient(") -> {
                return parseLinearGradient(
                    colour
                        .substringAfter("repeating-")
                        .substringAfter("linear-gradient(")
                        .substringBeforeLast(")")
                )
            }

            colour.startsWith("radial-gradient(") || colour.startsWith("repeating-radial-gradient(") -> {
                return parseRadialGradient(
                    colour
                        .substringAfter("repeating-")
                        .substringAfter("radial-gradient(")
                        .substringBeforeLast(")")
                )
            }


            else -> {
                return Brush.solidColor(parseColourName(colour))
            }
        }
    }
}
