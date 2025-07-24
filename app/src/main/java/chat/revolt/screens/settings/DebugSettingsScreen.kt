package chat.revolt.screens.settings

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.routes.push.subscribePush
import chat.revolt.dialogs.NotificationRationaleDialog
import chat.revolt.persistence.Database
import chat.revolt.persistence.KVStorage
import chat.revolt.persistence.SqlStorage
import chat.revolt.ui.theme.FragmentMono
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugSettingsScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage
) : ViewModel() {
    fun forgetAllSparks() {
        forgetPhysicalKeyboardSpark()
        forgetEarlyAccessSpark()
        forgetSwipeToReplySpark()
    }

    fun forgetPhysicalKeyboardSpark() {
        viewModelScope.launch {
            kvStorage.remove("spark/physicalKeyboard/dismissed")
        }
    }

    fun forgetEarlyAccessSpark() {
        viewModelScope.launch {
            kvStorage.remove("spark/earlyAccess/dismissed")
        }
    }

    fun forgetSwipeToReplySpark() {
        viewModelScope.launch {
            kvStorage.remove("spark/swipeToReply/dismissed")
        }
    }

    fun forgetLatestChangelog() {
        viewModelScope.launch {
            kvStorage.remove("latestChangelogRead")
        }
    }

    val serverQueries = Database(SqlStorage.driver).serverQueries.selectAll().executeAsList()
    val channelQueries = Database(SqlStorage.driver).channelQueries.selectAll().executeAsList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsScreen(
    navController: NavController,
    viewModel: DebugSettingsScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showNotificationsRationaleDialogue by remember { mutableStateOf(false) }
    val askNotificationsPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(
                    OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.e(
                                "DebugSettingsScreen",
                                "Fetching FCM registration token failed",
                                task.exception
                            )
                            return@OnCompleteListener
                        }

                        scope.launch {
                            subscribePush(auth = task.result)
                        }
                    })
            }
        }
    var showC2dmDataDialogue by remember { mutableStateOf(false) }
    var fcmToken by remember { mutableStateOf("") }
    var playServicesAvailable by remember { mutableStateOf(false) }

    if (showNotificationsRationaleDialogue) {
        NotificationRationaleDialog(
            onSelected = { accepted ->
                if (accepted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        askNotificationsPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Toast.makeText(
                            context,
                            "Will not ask for permission on pre-Tiramisu devices!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = {
                showNotificationsRationaleDialogue = false
            }
        )
    }

    if (showC2dmDataDialogue) {
        AlertDialog(
            onDismissRequest = {
                showC2dmDataDialogue = false
            },
            title = { Text("Notification Properties") },
            text = {
                Column {
                    Text("FCM token", style = MaterialTheme.typography.headlineSmall)
                    SelectionContainer {
                        Text(fcmToken, fontFamily = FragmentMono)
                    }
                    Text(
                        "Google Play Services available",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(if (playServicesAvailable) "Yes" else "No")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showC2dmDataDialogue = false
                }) {
                    Text("OK")
                }
            })
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = "Debug",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.icn_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Sparks",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    ElevatedButton(onClick = { viewModel.forgetPhysicalKeyboardSpark() }) {
                        Text("Forget physical keyboard spark")
                    }
                    ElevatedButton(onClick = { viewModel.forgetEarlyAccessSpark() }) {
                        Text("Forget early access spark")
                    }
                    ElevatedButton(onClick = { viewModel.forgetSwipeToReplySpark() }) {
                        Text("Forget swipe to reply spark")
                    }
                    Button(onClick = { viewModel.forgetAllSparks() }) {
                        Text("Forget all sparks")
                    }
                }

                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    ElevatedButton(onClick = { showNotificationsRationaleDialogue = true }) {
                        Text("Show notification rationale dialog")
                    }

                    ElevatedButton(onClick = {
                        playServicesAvailable = GoogleApiAvailabilityLight.getInstance()
                            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
                        FirebaseMessaging.getInstance().token.addOnCompleteListener(
                            OnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    Log.e(
                                        "DebugSettingsScreen",
                                        "Fetching FCM registration token failed",
                                        task.exception
                                    )
                                    fcmToken = "Fetching FCM registration token failed!"
                                    showC2dmDataDialogue = true
                                    return@OnCompleteListener
                                }

                                fcmToken = task.result
                                showC2dmDataDialogue = true
                            })
                    }) {
                        Text("Show Notification Properties")
                    }
                }

                Text(
                    text = "Changelogs",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    ElevatedButton(onClick = { viewModel.forgetLatestChangelog() }) {
                        Text("Mark latest changelog as unread")
                    }
                }

                Text(
                    text = "Database",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Text(
                    text = "Servers: ${viewModel.serverQueries.size}",
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(viewModel.serverQueries.size) { index ->
                        Text(
                            text = viewModel.serverQueries[index].toString(),
                            style = LocalTextStyle.current.copy(
                                fontFamily = FragmentMono
                            )
                        )
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    }
                }
                Text(
                    text = "Channels: ${viewModel.channelQueries.size}",
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(viewModel.channelQueries.size) { index ->
                        Text(
                            text = viewModel.channelQueries[index].toString(),
                            style = LocalTextStyle.current.copy(
                                fontFamily = FragmentMono
                            )
                        )
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    }
                }
            }
        }
    }
}
