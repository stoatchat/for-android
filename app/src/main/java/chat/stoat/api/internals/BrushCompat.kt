package chat.stoat.api.internals

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

private const val MAX_COLOUR_LENGTH = 4096
private const val MAX_GRADIENT_STOPS = 64

internal data class GradientStop(
    val colour: String,
    val position: Float?
)

internal fun parseGradientStop(part: String): GradientStop {
    val trimmedPart = part.trim()
    var openParenthesesCount = 0

    for (index in trimmedPart.indices) {
        when (trimmedPart[index]) {
            '(' -> openParenthesesCount++
            ')' -> openParenthesesCount--
            else -> {
                if (trimmedPart[index].isWhitespace() && openParenthesesCount == 0) {
                    val colour = trimmedPart.substring(0, index).trim()
                    val positionText = trimmedPart.substring(index).trim()
                    val position = positionText
                        .removeSuffix("%")
                        .toFloatOrNull()
                        ?.takeIf(Float::isFinite)
                        ?.div(100f)

                    return GradientStop(colour, position)
                }
            }
        }
    }

    return GradientStop(trimmedPart, null)
}

internal fun parseGradientParts(gradient: String): List<String>? {
    if (gradient.length > MAX_COLOUR_LENGTH) return null

    val parts = mutableListOf<String>()
    var startIndex = 0
    var openParenthesesCount = 0

    for (index in gradient.indices) {
        when (gradient[index]) {
            '(' -> openParenthesesCount++
            ')' -> {
                if (openParenthesesCount == 0) return null
                openParenthesesCount--
            }

            ',' -> {
                if (openParenthesesCount == 0) {
                    val part = gradient.substring(startIndex, index).trim()
                    if (part.isEmpty() || parts.size == MAX_GRADIENT_STOPS) return null
                    parts.add(part)
                    startIndex = index + 1
                }
            }
        }
    }

    if (openParenthesesCount != 0) return null

    val lastPart = gradient.substring(startIndex).trim()
    if (lastPart.isEmpty() || parts.size == MAX_GRADIENT_STOPS) return null
    parts.add(lastPart)

    return parts
}

private val RADIAL_GRADIENT_DESCRIPTOR_TOKENS = setOf(
    "at",
    "circle",
    "ellipse",
    "closest-corner",
    "closest-side",
    "farthest-corner",
    "farthest-side"
)
private val RADIAL_GRADIENT_SIZE_TOKEN = Regex(
    "[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:%|[a-z]+)",
    RegexOption.IGNORE_CASE
)

internal fun radialGradientColourParts(parts: List<String>): List<String> {
    val firstToken = parts.firstOrNull()
        ?.lowercase()
        ?.split(Regex("\\s+"))
        ?.firstOrNull()
        ?: return parts

    val hasDescriptor = firstToken in RADIAL_GRADIENT_DESCRIPTOR_TOKENS ||
        RADIAL_GRADIENT_SIZE_TOKEN.matches(firstToken)

    return if (hasDescriptor) {
        parts.drop(1)
    } else {
        parts
    }
}

internal fun <T> normalizeGradientStops(stops: List<Pair<Float?, T>>): List<Pair<Float, T>> {
    if (stops.isEmpty()) return emptyList()

    var previousPosition = 0f
    return stops.mapIndexed { index, (position, value) ->
        val defaultPosition = index.toFloat() / (stops.size - 1).coerceAtLeast(1)
        val safePosition = position
            ?.takeIf(Float::isFinite)
            ?.coerceIn(0f, 1f)
            ?: defaultPosition
        val orderedPosition = safePosition.coerceAtLeast(previousPosition)
        previousPosition = orderedPosition
        orderedPosition to value
    }
}

internal fun parseCssFunctionColour(colourString: String): Color? {
    val cleanedString = colourString.trim()
    if (cleanedString.length > MAX_COLOUR_LENGTH) return null

    val hasAlpha = cleanedString.startsWith("rgba(")
    if ((!hasAlpha && !cleanedString.startsWith("rgb(")) || !cleanedString.endsWith(")")) {
        return null
    }

    val colourParts = cleanedString.substringAfter('(')
        .dropLast(1)
        .split(',')
        .map(String::trim)
    if (colourParts.size != if (hasAlpha) 4 else 3) return null

    val channels = colourParts.take(3).map { channel ->
        channel.toIntOrNull()?.coerceIn(0, 255) ?: return null
    }
    val alpha = if (hasAlpha) {
        val alphaText = colourParts[3]
        val alphaValue = alphaText.removeSuffix("%").toFloatOrNull()
            ?.takeIf(Float::isFinite)
            ?: return null
        val normalizedAlpha = if (alphaText.endsWith('%') || alphaValue > 1f) {
            alphaValue / 100f
        } else {
            alphaValue
        }
        normalizedAlpha.coerceIn(0f, 1f)
    } else {
        1f
    }

    return Color(
        red = channels[0] / 255f,
        green = channels[1] / 255f,
        blue = channels[2] / 255f,
        alpha = alpha
    )
}

object BrushCompat {
    @Composable
    private fun parseLinearGradient(gradient: String): Brush {
        val stops = mutableListOf<Pair<Float?, Color>>()
        val parts = parseGradientParts(gradient)
            ?: return Brush.solidColor(LocalContentColor.current)

        val colourParts = parts.filterNot {
            it.startsWith("to ") || it.removeSuffix("deg").toFloatOrNull()?.isFinite() == true
        }
        colourParts.forEach { part ->
            val gradientStop = parseGradientStop(part)
            val colourPart = gradientStop.colour
            val colour = when {
                colourPart.startsWith("var(") -> {
                    parseVarToColour(
                        colourPart.substringAfter("var(").substringBeforeLast(")")
                    )
                }

                else -> parseFunctionColour(colourPart) ?: parseColourName(colourPart)
            }

            stops.add(gradientStop.position to colour)
        }

        if (stops.size < 2) {
            return Brush.solidColor(stops.firstOrNull()?.second ?: LocalContentColor.current)
        }

        return linearGradient(
            colorStops = normalizeGradientStops(stops).toTypedArray()
        )
    }

    @Composable
    private fun parseRadialGradient(gradient: String): Brush {
        val stops = mutableListOf<Pair<Float?, Color>>()
        val parts = parseGradientParts(gradient)
            ?: return Brush.solidColor(LocalContentColor.current)

        val colourParts = radialGradientColourParts(parts)
        colourParts.forEach { part ->
            val gradientStop = parseGradientStop(part)
            val colorPart = gradientStop.colour
            val color = when {
                colorPart.startsWith("var(") -> {
                    parseVarToColour(
                        colorPart.substringAfter("var(").substringBeforeLast(")")
                    )
                }

                else -> parseFunctionColour(colorPart) ?: parseColourName(colorPart)
            }

            stops.add(gradientStop.position to color)
        }

        if (stops.size < 2) {
            return Brush.solidColor(stops.firstOrNull()?.second ?: LocalContentColor.current)
        }

        return Brush.radialGradient(
            colorStops = normalizeGradientStops(stops).toTypedArray()
        )
    }

    fun parseFunctionColour(colourString: String): Color? {
        return parseCssFunctionColour(colourString)
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
            val cssColour = CSSColours[colour.lowercase()]
            if (cssColour != null) {
                return cssColour
            }

            Color(colour.toColorInt())
        } catch (e: RuntimeException) {
            LocalContentColor.current
        }
    }

    @Composable
    fun parseColour(colour: String): Brush {
        val cleanedColour = colour.trim()
        if (cleanedColour.isEmpty()) {
            return Brush.solidColor(Color.Unspecified)
        }
        if (cleanedColour.length > MAX_COLOUR_LENGTH) {
            return Brush.solidColor(LocalContentColor.current)
        }

        when {
            cleanedColour.startsWith("var(") && cleanedColour.endsWith(")") -> {
                return parseVar(
                    cleanedColour.substringAfter("var(").substringBeforeLast(")")
                )
            }

            (
                cleanedColour.startsWith("linear-gradient(") ||
                    cleanedColour.startsWith("repeating-linear-gradient(")
                ) && cleanedColour.endsWith(")") -> {
                return parseLinearGradient(
                    cleanedColour
                        .substringAfter("repeating-")
                        .substringAfter("linear-gradient(")
                        .substringBeforeLast(")")
                )
            }

            (
                cleanedColour.startsWith("radial-gradient(") ||
                    cleanedColour.startsWith("repeating-radial-gradient(")
                ) && cleanedColour.endsWith(")") -> {
                return parseRadialGradient(
                    cleanedColour
                        .substringAfter("repeating-")
                        .substringAfter("radial-gradient(")
                        .substringBeforeLast(")")
                )
            }


            else -> {
                return Brush.solidColor(parseColourName(cleanedColour))
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
        val stops = mutableListOf<Pair<Float?, Color>>()
        val parts = parseGradientParts(gradient) ?: return Brush.solidColor(defaultColour)

        val colourParts = parts.filterNot {
            it.startsWith("to ") || it.removeSuffix("deg").toFloatOrNull()?.isFinite() == true
        }
        colourParts.forEach { part ->
            val gradientStop = parseGradientStop(part)
            val colourPart = gradientStop.colour
            val colour = when {
                colourPart.startsWith("var(") -> {
                    parseVarToColour(
                        colourPart.substringAfter("var(").substringBeforeLast(")")
                    )
                }

                else -> parseFunctionColour(colourPart) ?: parseColourName(colourPart)
            }

            stops.add(gradientStop.position to colour)
        }

        if (stops.size < 2) {
            return Brush.solidColor(stops.firstOrNull()?.second ?: defaultColour)
        }

        return linearGradient(
            colorStops = normalizeGradientStops(stops).toTypedArray()
        )
    }

    private fun parseRadialGradient(gradient: String): Brush {
        val stops = mutableListOf<Pair<Float?, Color>>()
        val parts = parseGradientParts(gradient) ?: return Brush.solidColor(defaultColour)

        val colourParts = radialGradientColourParts(parts)
        colourParts.forEach { part ->
            val gradientStop = parseGradientStop(part)
            val colorPart = gradientStop.colour
            val color = when {
                colorPart.startsWith("var(") -> {
                    parseVarToColour(
                        colorPart.substringAfter("var(").substringBeforeLast(")")
                    )
                }

                else -> parseFunctionColour(colorPart) ?: parseColourName(colorPart)
            }

            stops.add(gradientStop.position to color)
        }

        if (stops.size < 2) {
            return Brush.solidColor(stops.firstOrNull()?.second ?: defaultColour)
        }

        return Brush.radialGradient(
            colorStops = normalizeGradientStops(stops).toTypedArray()
        )
    }

    fun parseFunctionColour(colourString: String): Color? {
        return parseCssFunctionColour(colourString)
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
            val cssColour = CSSColours[colour.lowercase()]
            if (cssColour != null) {
                return cssColour
            }

            Color(colour.toColorInt())
        } catch (e: RuntimeException) {
            defaultColour
        }
    }

    fun parseColour(colour: String): Brush {
        val cleanedColour = colour.trim()
        if (cleanedColour.isEmpty()) {
            return Brush.solidColor(Color.Unspecified)
        }
        if (cleanedColour.length > MAX_COLOUR_LENGTH) {
            return Brush.solidColor(defaultColour)
        }

        when {
            cleanedColour.startsWith("var(") && cleanedColour.endsWith(")") -> {
                return parseVar(
                    cleanedColour.substringAfter("var(").substringBeforeLast(")")
                )
            }

            (
                cleanedColour.startsWith("linear-gradient(") ||
                    cleanedColour.startsWith("repeating-linear-gradient(")
                ) && cleanedColour.endsWith(")") -> {
                return parseLinearGradient(
                    cleanedColour
                        .substringAfter("repeating-")
                        .substringAfter("linear-gradient(")
                        .substringBeforeLast(")")
                )
            }

            (
                cleanedColour.startsWith("radial-gradient(") ||
                    cleanedColour.startsWith("repeating-radial-gradient(")
                ) && cleanedColour.endsWith(")") -> {
                return parseRadialGradient(
                    cleanedColour
                        .substringAfter("repeating-")
                        .substringAfter("radial-gradient(")
                        .substringBeforeLast(")")
                )
            }


            else -> {
                return Brush.solidColor(parseColourName(cleanedColour))
            }
        }
    }
}
