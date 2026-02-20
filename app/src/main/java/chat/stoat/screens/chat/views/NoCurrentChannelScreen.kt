package chat.stoat.screens.chat.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.internals.extensions.zero
import chat.stoat.screens.chat.LocalIsConnected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoCurrentChannelScreen(useDrawer: Boolean, onDrawerClicked: () -> Unit) {
    Scaffold(
        topBar = {
            Column {
                AnimatedVisibility(LocalIsConnected.current) {
                    Spacer(
                        Modifier
                            .height(
                                WindowInsets.statusBars.asPaddingValues()
                                    .calculateTopPadding()
                            )
                    )
                }
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        if (useDrawer) {
                            IconButton(onClick = {
                                onDrawerClicked()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_menu_24dp),
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                        }
                    },
                    windowInsets = WindowInsets.zero
                )
            }
        },
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .fillMaxSize()
                .padding(64.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.no_active_channel),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.no_active_channel_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
