package chat.peptide.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.peptide.api.PeptideAPI
import chat.peptide.persistence.KVStorage
import kotlinx.coroutines.launch

// Internal: Untranslated
@Composable
fun ServerIdentityOptionsSheet(userId: String) {
    val user = PeptideAPI.userCache[userId]

    if (user == null) {
        Text("No such user")
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kv = remember { KVStorage(context) }

    var showUsernameDiscriminator by remember { mutableStateOf(false) }
    var ignoreServerAvatar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showUsernameDiscriminator = kv.getBoolean(
            "exp/serverIdentityOptions/$userId/showUsernameDiscriminator"
        ) == true
        ignoreServerAvatar = kv.getBoolean(
            "exp/serverIdentityOptions/$userId/ignoreServerAvatar"
        ) == true
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Identity Options",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${user.username}#${user.discriminator}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            ListItem(
                headlineContent = {
                    Text("Show Username#Tag next to nickname or display name")
                },
                trailingContent = {
                    Switch(
                        checked = showUsernameDiscriminator,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        kv.set(
                            "exp/serverIdentityOptions/$userId/showUsernameDiscriminator",
                            !showUsernameDiscriminator
                        )
                        showUsernameDiscriminator = !showUsernameDiscriminator
                    }
                },
                colors = ListItemDefaults.colors().copy(
                    containerColor = Color.Transparent,
                )
            )
            ListItem(
                headlineContent = {
                    Text("Ignore server avatar")
                },
                trailingContent = {
                    Switch(
                        checked = ignoreServerAvatar,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        kv.set(
                            "exp/serverIdentityOptions/$userId/ignoreServerAvatar",
                            !ignoreServerAvatar
                        )
                        ignoreServerAvatar = !ignoreServerAvatar
                    }
                },
                colors = ListItemDefaults.colors().copy(
                    containerColor = Color.Transparent,
                )
            )
        }
    }
}