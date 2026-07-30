package chat.stoat.api.internals

import android.content.Context
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language
import androidx.compose.ui.graphics.Brush as AndroidBrush

object SpecialUsers {
    const val JENNIFER = "01F1WKM5TK2V6KCZWR6DGBJDTZ"

    const val PLATFORM_MODERATION_USER = "01FC17E1WTM2BGE4F3ARN3FDAF"

    val TRUSTED_MODERATION_BOTS = listOf(
        "01GXBYCNQ52A9QYCQ99RBPXPAW", // AutoMod
        "01FCXRNNVW69AMSHBE61W1M5T3" // AutoMod Nightly
    )

    sealed class TeamMemberFlair {
        data class Brush(val brush: AndroidBrush) : TeamMemberFlair()
        data class AGSLShader(val shader: String, val fallback: AndroidBrush) :
            TeamMemberFlair()
    }

    @Language("AGSL")
    private val INSERT_SHADER = """
        uniform float value;
        
        half4 main(in float2 fragCoord) {
            return half4(fragCoord[0] / 1000.0, fragCoord[1] / 1000.0, sin(value), 1.0);
        }
    """.trimIndent()

    val TEAM_MEMBER_FLAIRS = mapOf(
        "01F1WKM5TK2V6KCZWR6DGBJDTZ" to TeamMemberFlair.Brush(
            AndroidBrush.linearGradient(
                listOf(
                    Color(0xFFD62900),
                    Color(0xFFFF9B55),
                    Color(0xFFFFFFFF),
                    Color(0xFFD461A6),
                    Color(0xFFA50062)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        ), // jen
        "01FDVES092RQR3YTY4JBGA0VCA" to TeamMemberFlair.Brush(
            AndroidBrush.verticalGradient(
                listOf(
                    Color(0xFFBB4681),
                    Color(0xFF9CA87F)
                )
            )
        ), // tom
        "01G6A2YMHED2TFTEMNKKMKFGH0" to TeamMemberFlair.Brush(
            AndroidBrush.linearGradient(
                listOf(
                    Color(0xFFE23672),
                    Color(0xFFFFAD20),
                    Color(0xFF58A551)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        ), // vale
        "01EX2NCWQ0CHS3QJF0FEQS1GR4" to TeamMemberFlair.AGSLShader(
            INSERT_SHADER,
            AndroidBrush.linearGradient(
                listOf(
                    Color(0xFF68224F),
                    Color(0xFFC68235)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        ), // insert
        "01FD58YK5W7QRV5H3D64KTQYX3" to TeamMemberFlair.Brush(
            AndroidBrush.verticalGradient(
                listOf(
                    Color(0xFF980000),
                    Color(0xFF1000AF)
                )
            )
        ), // zomatree
    )

    fun teamFlairAsBrush(context: Context, id: String): AndroidBrush? {
        return when (val flair = TEAM_MEMBER_FLAIRS[id]) {
            is TeamMemberFlair.Brush -> flair.brush

            is TeamMemberFlair.AGSLShader -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val shader = RuntimeShader(flair.shader)
                val valueUniform = (System.currentTimeMillis() % 2000) / 2000f
                shader.setFloatUniform("value", valueUniform)
                ShaderBrush(shader)
            } else {
                flair.fallback
            }

            null -> null
        }
    }
}
