package chat.stoat.screens.services

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.stoat.composables.screens.services.DiscoverView
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.back
import chat.stoat.shared.resources.discover
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.discover),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = StoatIcons.Back.painter(),
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                }
            )
        }
    ) { pv ->
        Column(
            Modifier
                .padding(pv)
                .fillMaxSize()
        ) {
            DiscoverView()
        }
    }
}