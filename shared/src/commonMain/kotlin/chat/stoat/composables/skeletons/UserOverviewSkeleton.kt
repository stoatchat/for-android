package chat.stoat.composables.skeletons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.stoat.api.settings.LoadedSettings
import com.valentinilk.shimmer.shimmer

@Composable
fun UserOverviewSkeleton(internalPadding: Boolean, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.BottomStart, modifier = Modifier
            .shimmer()
            .padding(horizontal = if (internalPadding) 16.dp else 0.dp)
            .clip(MaterialTheme.shapes.large)
    ) {
        Box(
            modifier = Modifier
                .background(skeletonColourOnBackground())
                .height(128.dp)
                .fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(LoadedSettings.avatarRadius))
                    .size(48.dp)
                    .background(skeletonColourOnBackground())
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Box(
                    Modifier
                        .width(66.dp)
                        .height(16.dp)
                        .background(skeletonColourOnBackground())
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(97.dp)
                        .height(16.dp)
                        .background(skeletonColourOnBackground())
                )
            }
        }
    }
}

@Preview
@Composable
private fun UserOverviewSkeletonPreview() {
    UserOverviewSkeleton(internalPadding = false)
}