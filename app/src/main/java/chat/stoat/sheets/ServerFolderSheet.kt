package chat.stoat.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import chat.stoat.R
import chat.stoat.api.settings.ServerFolders
import chat.stoat.composables.generic.SheetButton
import kotlinx.coroutines.launch

@Composable
fun ServerFolderSheet(
    folderId: String,
    onHideSheet: suspend () -> Unit,
    onChangeColour: suspend () -> Unit,
) {
    val folder = ServerFolders.folders.firstOrNull { it.id == folderId } ?: return
    val coroutineScope = rememberCoroutineScope()
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        var name by remember { mutableStateOf(folder.name) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.server_folder_sheet_rename)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.server_folder_edit_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        coroutineScope.launch {
                            onHideSheet()
                            ServerFolders.edit(folder.id, name, folder.colour)
                        }
                    }
                ) {
                    Text(stringResource(R.string.server_folder_edit_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column {
        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(stringResource(R.string.server_folder_sheet_rename))
            },
            onClick = { showRenameDialog = true }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_palette_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(stringResource(R.string.server_folder_sheet_colour))
            },
            onClick = {
                coroutineScope.launch { onChangeColour() }
            }
        )

        SheetButton(
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_24dp),
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(stringResource(R.string.server_folder_sheet_delete))
            },
            dangerous = true,
            onClick = {
                coroutineScope.launch {
                    onHideSheet()
                    ServerFolders.remove(folder.id)
                }
            }
        )
    }
}
