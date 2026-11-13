package chat.stoat.composables.generic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Weblink(text: String, url: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    AnyLink(text = text, action = { uriHandler.openUri(url) }, modifier = modifier)
}

@Composable
fun AnyLink(text: String, action: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground.copy(
            alpha = 0.5f
        ),
        style = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp
        ),
        modifier = modifier
            .padding(horizontal = 2.5.dp, vertical = 3.dp)
            .clickable(
                onClick = action
            )
    )
}

@Preview(showBackground = true)
@Composable
fun WeblinkPreview() {
    Weblink(text = "https://stoat.chat", url = "https://stoat.chat")
}

@Preview(showBackground = true)
@Composable
fun AnyLinkPreview() {
    val clicked = remember {
        mutableIntStateOf(0)
    }

    AnyLink(text = "Click me! #${clicked.intValue}", action = {
        clicked.intValue++
    })
}
