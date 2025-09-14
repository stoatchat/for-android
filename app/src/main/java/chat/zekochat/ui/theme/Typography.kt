package chat.zekochat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import chat.zekochat.R


private val Inter = FontFamily(
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
    Font(R.font.inter_black, FontWeight.Black),
    Font(R.font.inter_thin_italic, FontWeight.Thin, FontStyle.Italic),
    Font(R.font.inter_extralight_italic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.inter_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.inter_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.inter_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.inter_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.inter_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.inter_black_italic, FontWeight.Black, FontStyle.Italic)
)
private val InterDisplay = FontFamily(
    Font(R.font.inter_display_thin, FontWeight.Thin),
    Font(R.font.inter_display_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_display_light, FontWeight.Light),
    Font(R.font.inter_display_regular, FontWeight.Normal),
    Font(R.font.inter_display_medium, FontWeight.Medium),
    Font(R.font.inter_display_semibold, FontWeight.SemiBold),
    Font(R.font.inter_display_bold, FontWeight.Bold),
    Font(R.font.inter_display_extrabold, FontWeight.ExtraBold),
    Font(R.font.inter_display_black, FontWeight.Black),
    Font(R.font.inter_display_thin_italic, FontWeight.Thin, FontStyle.Italic),
    Font(R.font.inter_display_extralight_italic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.inter_display_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.inter_display_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.inter_display_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.inter_display_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.inter_display_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.inter_display_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.inter_display_black_italic, FontWeight.Black, FontStyle.Italic)
)
val FragmentMono = FontFamily(
    Font(R.font.fragmentmono_regular, FontWeight.Normal),
    Font(R.font.fragmentmono_italic, FontWeight.Normal, FontStyle.Italic)
)

val PeptideTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),

    titleLarge = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),

    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
)
