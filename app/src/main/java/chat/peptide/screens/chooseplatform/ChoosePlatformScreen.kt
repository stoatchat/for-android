package chat.peptide.screens.chooseplatform

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.peptide.R
import chat.peptide.api.ApplicationPlatform
import chat.peptide.api.RevoltAPI
import chat.peptide.composables.generic.SquareButton

@Composable
fun ChoosePlatformScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val apiUrlValue = remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .safeDrawingPadding()
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.padding(vertical = 24.dp),
            painter = painterResource(id = R.drawable.pep_logo_wide),
            contentDescription = "Pep Wide Logo"
        )

        Image(
            modifier = Modifier.padding(vertical = 24.dp),
            painter = painterResource(id = R.drawable.choose_platform_hligh_hand_img),
            contentDescription = "Choose Platform Highlight Image"
        )

        Column(
            modifier = Modifier
                .padding(vertical = 32.dp)
        ) {
            Text(
                text = "Welcome to PepChat!",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .fillMaxWidth()
            )

            Text(
                text = "Choose your preferred platform!",
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.5f
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            )
        }
        Row(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(72.dp)
                        .clickable {
                            apiUrlValue.value = "https://peptide.chat/api"
                        },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "PEP Logo"
                        )
                    }
                }
                Text(
                    text = "PepChat",
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.5f
                    ),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal
                    ),
                )
            }
            Spacer(modifier = Modifier.padding(horizontal = 24.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(72.dp)
                        .clickable {
                            apiUrlValue.value = "https://api.revolt.chat"
                        },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_revolt_r),
                            contentDescription = "Revolt Logo"
                        )
                    }
                }
                Text(
                    text = "Revolt",
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.5f
                    ),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal
                    ),
                )
            }
        }
        OutlinedTextField(
            value = apiUrlValue.value,
            onValueChange = { apiUrlValue.value = it },
            placeholder = {
                Text(
                    text = "Enter custom API url...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_double_connect),
                    contentDescription = "connect icon",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        SquareButton(
            onClick = {
                if(apiUrlValue.value.isNotEmpty()){
                    when(apiUrlValue.value){
                        ApplicationPlatform.PEP.baseUrl -> RevoltAPI.setPlatform(
                            ApplicationPlatform.PEP
                        )
                        ApplicationPlatform.REVOLT.baseUrl -> RevoltAPI.setPlatform(
                            ApplicationPlatform.REVOLT
                        )
                        else -> return@SquareButton
                    }
                }
                navController.navigate("login/greeting")
            },
            modifier = Modifier
                .padding(top = 32.dp, bottom = 16.dp)
                .fillMaxWidth()
                .testTag("confirm_platform_button"),
        ) {
            Text(text = stringResource(R.string.confirm))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PepChat v1.0",
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.5f
                ),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
            )
        }
    }
}