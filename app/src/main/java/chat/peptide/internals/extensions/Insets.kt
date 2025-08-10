package chat.peptide.internals.extensions

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable

val WindowInsets.Companion.zero: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsets(left = 0, right = 0, top = 0, bottom = 0)