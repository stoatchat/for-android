package chat.revolt.composables.profile

import android.graphics.Bitmap
import android.icu.text.DateFormat
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import chat.revolt.R
import chat.revolt.api.internals.ResourceLocations
import chat.revolt.api.internals.ULID
import chat.revolt.api.internals.UserQR
import chat.revolt.api.schemas.User
import chat.revolt.composables.generic.UserAvatar
import chat.revolt.ui.theme.FragmentMono
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalTextApi::class)
@Composable
fun UserCard(
    user: User? = null,
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var palette by remember { mutableStateOf<Palette?>(null) }
    LaunchedEffect(user) {
        val avatarUrl = ResourceLocations.userAvatarUrl(user)
        val bitmap = withContext(Dispatchers.IO) {
            Glide.with(context).load(avatarUrl).submit().get().toBitmap()
        }
        palette = Palette.from(bitmap).generate()
    }

    var qrCode by remember(user) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(user) {
        withContext(Dispatchers.IO) {
            try {
                if (user == null) throw NullPointerException("User is null")

                val matrix = QRCodeWriter().encode(
                    UserQR.contents(user),
                    BarcodeFormat.QR_CODE,
                    512,
                    512,
                    mapOf(EncodeHintType.MARGIN to "1")
                )

                val bitmap =
                    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)

                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        bitmap.setPixel(
                            x,
                            y,
                            if (matrix.get(
                                    x,
                                    y
                                )
                            ) Color.White.toArgb() else Color.Transparent.toArgb()
                        )
                    }
                }

                Log.d("UserCard", "Generated QR code for user ${user.id}")

                qrCode = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .aspectRatio(9 / 12f)
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
            .then(modifier)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            LocalTextStyle provides LocalTextStyle.current.copy(
                fontFamily = FontFamily(
                    Font(
                        R.font.revolt_usercard,
                        variationSettings = FontVariation.Settings(FontVariation.weight(200)),
                        weight = FontWeight.ExtraLight
                    ),
                    Font(
                        R.font.revolt_usercard,
                        variationSettings = FontVariation.Settings(FontVariation.weight(300)),
                        weight = FontWeight.Light
                    ),
                    Font(
                        R.font.revolt_usercard,
                        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
                        weight = FontWeight.Normal
                    ),
                    Font(
                        R.font.revolt_usercard,
                        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
                        weight = FontWeight.Medium
                    ),
                    Font(
                        R.font.revolt_usercard,
                        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
                        weight = FontWeight.Bold
                    ),
                    Font(
                        R.font.revolt_usercard,
                        variationSettings = FontVariation.Settings(FontVariation.weight(900)),
                        weight = FontWeight.Black
                    )
                )
            )
        ) {
            ConstraintLayout(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF150E2A),
                                Color(0xFF332354)
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite
                        )
                    )
                    .padding(9.dp)
                    .border(3.dp, palette?.mutedSwatch?.rgb?.let { Color(it) }
                        ?: Color(0xFFFF005C), shape = MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                val (heading, nameLabel, name, usernameLabel, username, joinDateLabel, joinDate, qrLabel, qr, photoLabel, photo, url) = createRefs()

                Image(
                    painter = painterResource(R.drawable.usercard_heading),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .constrainAs(heading) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                        }
                )

                Text(
                    stringResource(R.string.user_card_data_point_display_name, 1).uppercase(),
                    fontFamily = FragmentMono,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .constrainAs(nameLabel) {
                            bottom.linkTo(name.top)
                            start.linkTo(name.start)
                        }
                )

                Text(
                    user?.displayName ?: user?.username ?: stringResource(R.string.unknown),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 12.sp,
                        maxFontSize = 24.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .constrainAs(name) {
                            bottom.linkTo(usernameLabel.top, margin = 8.dp)
                            start.linkTo(usernameLabel.start)
                        }
                )

                Text(
                    stringResource(R.string.user_card_data_point_username, 2).uppercase(),
                    fontFamily = FragmentMono,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .constrainAs(usernameLabel) {
                            bottom.linkTo(username.top)
                            start.linkTo(username.start)
                        }
                )

                Text(
                    text = buildAnnotatedString {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Medium))
                        append(user?.username ?: stringResource(R.string.unknown))
                        pop()
                        pushStyle(SpanStyle(fontWeight = FontWeight.ExtraLight))
                        append("#${user?.discriminator ?: "0000"}")
                        pop()
                    },
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 12.sp,
                        maxFontSize = 24.sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .constrainAs(username) {
                            bottom.linkTo(joinDateLabel.top, margin = 8.dp)
                            start.linkTo(joinDateLabel.start)
                        }
                )

                Text(
                    stringResource(R.string.user_card_data_point_join_date, 3).uppercase(),
                    fontFamily = FragmentMono,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .constrainAs(joinDateLabel) {
                            bottom.linkTo(joinDate.top)
                            start.linkTo(joinDate.start)
                        }
                )

                Text(
                    DateFormat.getDateInstance().format(
                        Date.from(
                            Instant.ofEpochMilli(
                                ULID.asTimestamp(
                                    user?.id ?: "00000000000000000000000000"
                                )
                            )
                        )
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 12.sp,
                        maxFontSize = 24.sp,
                    ),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .constrainAs(joinDate) {
                            bottom.linkTo(qrLabel.top, margin = 8.dp)
                            start.linkTo(qrLabel.start)
                        }
                )

                Text(
                    stringResource(R.string.user_card_data_point_qrcode, 4).uppercase(),
                    fontFamily = FragmentMono,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .constrainAs(qrLabel) {
                            bottom.linkTo(qr.top)
                            start.linkTo(qr.start)
                        }
                )

                Image(
                    painter = qrCode?.let { BitmapPainter(it.asImageBitmap()) }
                        ?: painterResource(R.drawable.usercard_qr_placeholder),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                        .constrainAs(qr) {
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                        }
                )

                Text(
                    stringResource(R.string.user_card_data_point_photo, 5).uppercase(),
                    fontFamily = FragmentMono,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .constrainAs(photoLabel) {
                            top.linkTo(nameLabel.top)
                            start.linkTo(photo.start)
                        }
                )

                BoxWithConstraints(Modifier.constrainAs(photo) {
                    top.linkTo(photoLabel.bottom, margin = 8.dp)
                    end.linkTo(parent.end)
                }) {
                    UserAvatar(
                        username = user?.username ?: stringResource(R.string.unknown),
                        userId = user?.id ?: "00000000000000000000000000",
                        avatar = user?.avatar,
                        shape = CircleShape,
                        size = maxWidth / 3
                    )
                }

                Text(
                    "revolt.chat",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .constrainAs(url) {
                            bottom.linkTo(parent.bottom)
                            end.linkTo(parent.end)
                        }
                )
            }
        }
    }
}