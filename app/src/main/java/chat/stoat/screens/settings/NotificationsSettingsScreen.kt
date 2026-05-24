package chat.stoat.screens.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.routes.push.subscribePush
import chat.stoat.api.routes.push.unsubscribePush
import chat.stoat.composables.generic.CenteredListItem
import chat.stoat.dialogs.NotificationRationaleDialog
import chat.stoat.persistence.KVStorage
import chat.stoat.settings.dsl.SettingsPage
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@SuppressLint("StaticFieldLeak")
class NotificationsSettingsScreenViewModel(
    private val kvStorage: KVStorage,
    private val context: Context
) : ViewModel() {
    var showRationale by mutableStateOf(false)
    var isPushEnabled by mutableStateOf(false)
        private set
    var isUpdating by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            isPushEnabled = checkPushEnabled()
        }
    }

    private suspend fun checkPushEnabled(): Boolean {
        val hasPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val hasToken = kvStorage.get("fcmToken") != null
        return hasPermission && hasToken
    }

    fun onEnableRequested() {
        showRationale = true
    }

    fun subscribeIfNeeded() {
        if (isUpdating) return
        isUpdating = true
        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    isUpdating = false
                    return@OnCompleteListener
                }
                val newToken = task.result
                viewModelScope.launch {
                    try {
                        val existingToken = kvStorage.get("fcmToken")
                        if (existingToken != newToken) {
                            subscribePush(auth = newToken)
                            kvStorage.set("fcmToken", newToken)
                        }
                        kvStorage.remove("pushNotificationsRejected")
                        isPushEnabled = checkPushEnabled()
                    } catch (e: Exception) {
                        // subscribe failed, leave state unchanged
                    } finally {
                        isUpdating = false
                    }
                }
            }
        )
    }

    fun disablePush() {
        if (isUpdating) return
        isUpdating = true
        viewModelScope.launch {
            try {
                val token = kvStorage.get("fcmToken")
                if (token != null) {
                    runCatching { unsubscribePush() }
                    kvStorage.remove("fcmToken")
                }
                kvStorage.set("pushNotificationsRejected", true)
                isPushEnabled = false
            } finally {
                isUpdating = false
            }
        }
    }
}

@Composable
fun NotificationsSettingsScreen(
    navController: NavController,
    viewModel: NotificationsSettingsScreenViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val askNotificationsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.subscribeIfNeeded()
    }

    if (viewModel.showRationale) {
        NotificationRationaleDialog(
            onSelected = { accepted ->
                if (accepted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        askNotificationsPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.subscribeIfNeeded()
                    }
                }
            },
            onDismiss = { viewModel.showRationale = false }
        )
    }

    SettingsPage(
        navController = navController,
        title = { Text(stringResource(R.string.settings_notifications)) }
    ) {
        CenteredListItem(
            headlineContent = { Text(stringResource(R.string.settings_notifications_push)) },
            supportingContent = { Text(stringResource(R.string.settings_notifications_push_description)) },
            trailingContent = {
                Switch(
                    checked = viewModel.isPushEnabled,
                    onCheckedChange = null,
                    enabled = !viewModel.isUpdating
                )
            },
            modifier = Modifier
                .semantics { role = Role.Switch }
                .clickable(enabled = !viewModel.isUpdating) {
                    if (viewModel.isPushEnabled) viewModel.disablePush()
                    else viewModel.onEnableRequested()
                }
        )
        CenteredListItem(
            headlineContent = { Text(stringResource(R.string.settings_notifications_system)) },
            supportingContent = { Text(stringResource(R.string.settings_notifications_system_description)) },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            },
            modifier = Modifier.clickable {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            }
        )
    }
}
