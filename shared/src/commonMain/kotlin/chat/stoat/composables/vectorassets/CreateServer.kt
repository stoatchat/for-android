package chat.stoat.composables.vectorassets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val CreateServer: ImageVector
    @Composable
    get() {
        if (_CreateServer != null) {
            return _CreateServer!!
        }
        _CreateServer = ImageVector.Builder(
            name = "CreateServer",
            defaultWidth = 605.dp,
            defaultHeight = 604.dp,
            viewportWidth = 605f,
            viewportHeight = 604f
        ).apply {
            path(fill = SolidColor(MaterialTheme.colorScheme.primaryContainer)) {
                moveTo(270.33f, 36.02f)
                curveTo(287.45f, 16.89f, 317.41f, 16.89f, 334.54f, 36.02f)
                lineTo(374.1f, 80.21f)
                curveTo(382.83f, 89.96f, 395.52f, 95.22f, 408.59f, 94.5f)
                lineTo(467.8f, 91.22f)
                curveTo(493.45f, 89.8f, 514.63f, 110.99f, 513.21f, 136.63f)
                lineTo(509.94f, 195.85f)
                curveTo(509.22f, 208.91f, 514.47f, 221.6f, 524.22f, 230.34f)
                lineTo(568.41f, 269.89f)
                curveTo(587.54f, 287.02f, 587.54f, 316.98f, 568.41f, 334.11f)
                lineTo(524.22f, 373.67f)
                curveTo(514.47f, 382.4f, 509.22f, 395.08f, 509.94f, 408.15f)
                lineTo(513.21f, 467.37f)
                curveTo(514.63f, 493.01f, 493.45f, 514.2f, 467.8f, 512.78f)
                lineTo(408.59f, 509.5f)
                curveTo(395.52f, 508.78f, 382.83f, 514.04f, 374.1f, 523.79f)
                lineTo(334.54f, 567.97f)
                curveTo(317.41f, 587.11f, 287.45f, 587.11f, 270.33f, 567.97f)
                lineTo(230.77f, 523.79f)
                curveTo(222.04f, 514.04f, 209.35f, 508.78f, 196.28f, 509.5f)
                lineTo(137.07f, 512.78f)
                curveTo(111.42f, 514.2f, 90.24f, 493.01f, 91.66f, 467.37f)
                lineTo(94.93f, 408.15f)
                curveTo(95.65f, 395.08f, 90.4f, 382.4f, 80.64f, 373.67f)
                lineTo(36.46f, 334.11f)
                curveTo(17.32f, 316.98f, 17.32f, 287.02f, 36.46f, 269.89f)
                lineTo(80.64f, 230.34f)
                curveTo(90.4f, 221.6f, 95.65f, 208.91f, 94.93f, 195.85f)
                lineTo(91.66f, 136.63f)
                curveTo(90.24f, 110.99f, 111.42f, 89.8f, 137.07f, 91.22f)
                lineTo(196.28f, 94.5f)
                curveTo(209.35f, 95.22f, 222.04f, 89.96f, 230.77f, 80.21f)
                lineTo(270.33f, 36.02f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onPrimaryContainer)) {
                moveTo(285.02f, 404.6f)
                verticalLineTo(199.4f)
                horizontalLineTo(319.85f)
                verticalLineTo(404.6f)
                horizontalLineTo(285.02f)
                close()
                moveTo(199.83f, 319.41f)
                verticalLineTo(284.59f)
                horizontalLineTo(405.03f)
                verticalLineTo(319.41f)
                horizontalLineTo(199.83f)
                close()
            }
        }.build()

        return _CreateServer!!
    }

@Suppress("ObjectPropertyName")
private var _CreateServer: ImageVector? = null