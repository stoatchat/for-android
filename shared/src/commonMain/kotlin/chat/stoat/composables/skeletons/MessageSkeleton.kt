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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.stoat.api.settings.LoadedSettings

enum class MessageSkeletonVariant {
    One,
    Two,
    Three
}

@Composable
fun MessageSkeleton(variant: MessageSkeletonVariant, modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
    ) {
        Column {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(LoadedSettings.avatarRadius))
                    .size(40.dp)
                    .background(skeletonColourOnBackground())
            )
        }
        Column(modifier = Modifier.padding(start = 10.dp, top = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(
                            when (variant) {
                                // Chaotic widths. Boo!
                                MessageSkeletonVariant.One -> 66.dp
                                MessageSkeletonVariant.Two -> 29.dp
                                MessageSkeletonVariant.Three -> 97.dp
                            }
                        )
                        .height(16.dp)
                        .background(skeletonColourOnBackground())
                )

                Spacer(modifier = Modifier.width(5.dp))

                Box(
                    Modifier
                        .width(42.dp)
                        .height(13.dp)
                        .background(skeletonColourOnBackground())
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth(
                        1 / when (variant) {
                            MessageSkeletonVariant.One -> 1.5f
                            MessageSkeletonVariant.Two -> 1.2f
                            MessageSkeletonVariant.Three -> 1.8f
                        }
                    )
                    .height(16.dp)
                    .background(skeletonColourOnBackground())
            )
        }
    }
}

@Preview
@Composable
private fun MessageSkeletonOnePreview() {
    MessageSkeleton(MessageSkeletonVariant.One)
}

@Preview
@Composable
private fun MessageSkeletonTwoPreview() {
    MessageSkeleton(MessageSkeletonVariant.Two)
}

@Preview
@Composable
private fun MessageSkeletonThreePreview() {
    MessageSkeleton(MessageSkeletonVariant.Three)
}