package chat.stoat.composables.vectorassets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val JoinServer: ImageVector
    @Composable
    get() {
        if (_JoinServer != null) {
            return _JoinServer!!
        }
        _JoinServer = ImageVector.Builder(
            name = "JoinServer",
            defaultWidth = 518.dp,
            defaultHeight = 338.dp,
            viewportWidth = 518f,
            viewportHeight = 338f
        ).apply {
            path(fill = SolidColor(MaterialTheme.colorScheme.secondaryContainer)) {
                moveTo(196.04f, 208.6f)
                curveTo(174.16f, 186.73f, 174.16f, 151.27f, 196.04f, 129.4f)
                lineTo(308.76f, 16.68f)
                curveTo(330.63f, -5.19f, 366.08f, -5.19f, 387.95f, 16.68f)
                lineTo(500.68f, 129.4f)
                curveTo(522.55f, 151.27f, 522.55f, 186.73f, 500.68f, 208.6f)
                lineTo(387.95f, 321.32f)
                curveTo(366.08f, 343.19f, 330.63f, 343.19f, 308.76f, 321.32f)
                lineTo(196.04f, 208.6f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onSurface)) {
                moveTo(85.98f, 250.61f)
                lineTo(67.19f, 231.99f)
                lineTo(116.5f, 182.68f)
                horizontalLineTo(0.96f)
                verticalLineTo(155.32f)
                horizontalLineTo(116.5f)
                lineTo(67.19f, 106.09f)
                lineTo(85.98f, 87.39f)
                lineTo(167.59f, 169f)
                lineTo(85.98f, 250.61f)
                close()
            }
        }.build()

        return _JoinServer!!
    }

@Suppress("ObjectPropertyName")
private var _JoinServer: ImageVector? = null