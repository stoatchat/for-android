package chat.peptide.screens.labs.ui.sandbox

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.peptide.markdown.jbm.JBM
import chat.peptide.markdown.jbm.JBMRenderer
import chat.peptide.markdown.jbm.LocalJBMarkdownTreeState
import chat.peptide.settings.dsl.SettingsPage

@OptIn(JBM::class)
@Composable
fun JBMSandbox(navController: NavController) {
    var mdSource by remember { mutableStateOf("") }
    var submitMdSource by remember { mutableStateOf<String?>(null) }
    var isEmbedded by remember { mutableStateOf(false) }

    SettingsPage(
        navController = navController,
        title = {
            Text(
                text = "JB Markdown Sandbox",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    ) {
        Subcategory(
            title = { Text("Options", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isEmbedded,
                    onCheckedChange = { isEmbedded = it }
                )
                Spacer(Modifier.width(8.dp))
                Text("Embedded", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Subcategory(
            title = { Text("Source", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        ) {
            TextField(
                value = mdSource,
                onValueChange = { mdSource = it },
                label = { Text("Markdown source") },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(onClick = {
                submitMdSource = mdSource
            }) {
                Text("Submit")
            }
            TextButton(onClick = {
                submitMdSource = """# Full range of MD now supported!
1. Text with **bold**, *italics*, and ***both***!
2. You ~~can't see me~~.
3. [I'm a link to another website.](<https://revolt.chat>)
4. I'm a spoiler with ||**bold text inside it**||
    - I'm a sub-item on this list...
        - Let's go even deeper...

`Inline code`

```js
let x = "I'm a multi-line code block!";
```

> > ${'$'}${'$'}E = mc^2${'$'}${'$'}
> 
> — Albert Einstein

| Timestamp | Mention | Channel Link | Message Link |
|:-:|:-:|:-:|:-:|
| <t:1663846662:f> | <@01EX2NCWQ0CHS3QJF0FEQS1GR4> | <#01H73F4RAHTPBHKJ1XBQDXK3NQ> | https://revolt.chat/server/01F7ZSBSFHQ8TA81725KQCSDDP/channel/01F92C5ZXBQWQ8KY7J8KY917NM/01J25XZM9JXVVJDDKFPB7Q48HZ |"""
            }) {
                Text("Submit test document")
            }
        }
        Subcategory(
            title = { Text("Output", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        ) {
            CompositionLocalProvider(
                LocalJBMarkdownTreeState provides LocalJBMarkdownTreeState.current.copy(
                    embedded = isEmbedded
                )
            ) {
                submitMdSource?.let { JBMRenderer(it, Modifier) }
                    ?: Text("Submit some Markdown and see the output.")
            }
        }
    }
}