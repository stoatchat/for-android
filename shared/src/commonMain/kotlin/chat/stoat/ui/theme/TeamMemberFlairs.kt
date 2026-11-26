package chat.stoat.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object TeamMemberFlairs {
    val TEAM_MEMBER_FLAIRS: Map<String, Brush> = mapOf(
        "01F1WKM5TK2V6KCZWR6DGBJDTZ" to Brush.linearGradient(
            listOf(
                Color(0xFFD62900),
                Color(0xFFFF9B55),
                Color(0xFFFFFFFF),
                Color(0xFFD461A6),
                Color(0xFFA50062)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        ), // jen
        "01FDVES092RQR3YTY4JBGA0VCA" to Brush.horizontalGradient(
            listOf(
                Color(0xFFBB4681),
                Color(0xFF9CA87F)
            )
        ), // tom
        "01G6A2YMHED2TFTEMNKKMKFGH0" to Brush.linearGradient(
            listOf(
                Color(0xFFE23672),
                Color(0xFFFFAD20),
                Color(0xFF58A551)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        ), // vale
        "01EX2NCWQ0CHS3QJF0FEQS1GR4" to Brush.linearGradient(
            listOf(
                Color(0xFF68224F),
                Color(0xFFC68235)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        ), // insert
        "01FD58YK5W7QRV5H3D64KTQYX3" to Brush.horizontalGradient(
            listOf(
                Color(0xFF980000),
                Color(0xFF1000AF)
            )
        ), // zomatree
    )

    fun teamFlairAsBrush(id: String): Brush? = TEAM_MEMBER_FLAIRS[id]
}
