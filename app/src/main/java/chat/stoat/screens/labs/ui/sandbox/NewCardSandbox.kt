package chat.stoat.screens.labs.ui.sandbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import chat.stoat.api.StoatAPI
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.core.designsystem.theme.FragmentMono
import chat.stoat.platform.LocalToaster
import chat.stoat.platform.show
import chat.stoat.settings.dsl.SettingsPage
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.tmp_card_asset_tl
import org.jetbrains.compose.resources.painterResource
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewCardSandboxScreen(navController: NavController) {
    var uidInput by remember { mutableStateOf(StoatAPI.selfId ?: "") }
    var activeUid by remember { mutableStateOf(StoatAPI.selfId) }
    val activeUser = remember(activeUid) {
        StoatAPI.userCache[activeUid]!!
    }

    val toaster = LocalToaster.current

    val date = remember {
        DateFormat
            .getDateInstance(
                DateFormat.MEDIUM,
                Locale.getDefault()
            )
            .format(Date())
    }

    SettingsPage(
        navController,
        title = {
            Text(
                text = "New Card",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    ) {
        TextField(
            value = uidInput,
            onValueChange = { uidInput = it },
            label = { Text("User ID") },
            placeholder = { Text("Enter user ID") },
            keyboardActions = KeyboardActions(
                onDone = {
                    if (uidInput.isNotBlank() && StoatAPI.userCache.containsKey(uidInput)) {
                        activeUid = uidInput
                        uidInput = ""
                    } else if (!StoatAPI.userCache.containsKey(uidInput)) {
                        toaster.show("User id not found in cache")
                    }
                }
            ),
            singleLine = true,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10))
                .background(Color.Red)
                .padding(16.dp)
        ) {
            val (logoAsset, cardIssuedLabel, cardIssued, tag, avatar) = createRefs()

            Image(
                painter = painterResource(Res.drawable.tmp_card_asset_tl),
                contentDescription = "XY Uppercase Text",
                modifier = Modifier
                    .constrainAs(logoAsset) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
                    .fillMaxWidth(1f / 3f)
            )
            Text(
                text = "Card Issued",
                modifier = Modifier.constrainAs(cardIssuedLabel) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = date,
                modifier = Modifier.constrainAs(cardIssued) {
                    top.linkTo(cardIssuedLabel.bottom)
                    end.linkTo(parent.end)
                },
                fontFamily = FragmentMono,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${activeUser.username}#${activeUser.discriminator}",
                modifier = Modifier
                    .constrainAs(tag) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    }
                    .fillMaxWidth(5f / 6f),
                fontWeight = FontWeight.ExtraBold,
                autoSize = TextAutoSize.StepBased(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            UserAvatar(
                username = activeUser.username ?: "",
                userId = activeUser.id ?: "",
                avatar = activeUser.avatar,
                shape = MaterialShapes.Circle.toShape(),
                size = 84.dp,
                modifier = Modifier
                    .constrainAs(avatar) {
                        bottom.linkTo(tag.top, margin = 8.dp)
                        start.linkTo(tag.start)
                    }
            )
        }
    }
}