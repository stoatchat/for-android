package chat.peptide.composables.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.DpSize

@Composable
fun SizeMeasured(
    viewToMeasure: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (DpSize) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val measuredSize = subcompose("viewToMeasure") {
            viewToMeasure()
        }[0].measure(constraints)
            .let {
                DpSize(
                    width = it.width.toDp(),
                    height = it.height.toDp()
                )
            }

        val contentPlaceable = subcompose("content") {
            content(measuredSize)
        }.firstOrNull()?.measure(constraints)

        layout(contentPlaceable?.width ?: 0, contentPlaceable?.height ?: 0) {
            contentPlaceable?.place(0, 0)
        }
    }
}