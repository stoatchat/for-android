package chat.stoat.composables.vectorassets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeoGateUX: ImageVector
    @Composable
    get() {
        if (_GeoGateUX != null) {
            return _GeoGateUX!!
        }
        _GeoGateUX = ImageVector.Builder(
            name = "GeoGate",
            defaultWidth = 282.dp,
            defaultHeight = 342.dp,
            viewportWidth = 282f,
            viewportHeight = 342f
        ).apply {
            path(fill = SolidColor(MaterialTheme.colorScheme.onBackground)) {
                moveTo(173.19f, 30.41f)
                lineTo(207.56f, 26.67f)
                lineTo(214.24f, 88.06f)
                lineTo(171.27f, 92.73f)
                lineTo(167.48f, 80.72f)
                lineTo(133.1f, 84.46f)
                lineTo(136.83f, 118.76f)
                curveTo(148.6f, 118.88f, 160.25f, 121.25f, 171.13f, 125.76f)
                curveTo(182.29f, 130.38f, 192.44f, 137.16f, 200.98f, 145.71f)
                curveTo(209.53f, 154.25f, 216.31f, 164.4f, 220.93f, 175.56f)
                curveTo(225.56f, 186.73f, 227.94f, 198.69f, 227.94f, 210.78f)
                curveTo(227.94f, 235.19f, 218.24f, 258.59f, 200.98f, 275.85f)
                curveTo(183.72f, 293.11f, 160.32f, 302.8f, 135.91f, 302.8f)
                curveTo(123.83f, 302.8f, 111.86f, 300.42f, 100.69f, 295.8f)
                curveTo(89.53f, 291.17f, 79.38f, 284.4f, 70.84f, 275.85f)
                curveTo(53.58f, 258.59f, 43.88f, 235.19f, 43.88f, 210.78f)
                curveTo(43.88f, 186.37f, 53.58f, 162.96f, 70.84f, 145.71f)
                curveTo(79.38f, 137.16f, 89.53f, 130.38f, 100.69f, 125.76f)
                curveTo(108.32f, 122.6f, 116.32f, 120.49f, 124.48f, 119.47f)
                lineTo(114.15f, 24.41f)
                lineTo(169.4f, 18.4f)
                lineTo(173.19f, 30.41f)
                close()
                moveTo(64.22f, 194.31f)
                curveTo(63.03f, 199.55f, 62.29f, 205.07f, 62.29f, 210.78f)
                curveTo(62.29f, 248.03f, 89.92f, 278.76f, 125.86f, 283.64f)
                lineTo(126.71f, 265.99f)
                curveTo(121.83f, 265.99f, 117.14f, 264.05f, 113.69f, 260.6f)
                curveTo(110.24f, 257.15f, 108.3f, 252.47f, 108.3f, 247.59f)
                verticalLineTo(238.39f)
                lineTo(64.22f, 194.31f)
                close()
                moveTo(163.52f, 146.36f)
                curveTo(163.52f, 151.24f, 161.58f, 155.92f, 158.13f, 159.38f)
                curveTo(154.68f, 162.83f, 149.99f, 164.77f, 145.11f, 164.77f)
                horizontalLineTo(126.71f)
                verticalLineTo(183.17f)
                curveTo(126.71f, 185.61f, 125.74f, 187.95f, 124.01f, 189.68f)
                curveTo(122.29f, 191.4f, 119.95f, 192.37f, 117.51f, 192.37f)
                horizontalLineTo(99.1f)
                verticalLineTo(210.78f)
                horizontalLineTo(154.32f)
                curveTo(156.76f, 210.78f, 159.1f, 211.75f, 160.82f, 213.48f)
                curveTo(162.55f, 215.2f, 163.52f, 217.54f, 163.52f, 219.98f)
                verticalLineTo(247.59f)
                horizontalLineTo(172.72f)
                curveTo(180.91f, 247.59f, 187.81f, 253.02f, 190.21f, 260.38f)
                curveTo(198.23f, 251.63f, 204.01f, 241.07f, 207.06f, 229.59f)
                curveTo(210.1f, 218.12f, 210.33f, 206.07f, 207.7f, 194.5f)
                curveTo(205.08f, 182.92f, 199.69f, 172.15f, 191.99f, 163.11f)
                curveTo(184.3f, 154.07f, 174.53f, 147.03f, 163.52f, 142.59f)
                verticalLineTo(146.36f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.error)) {
                moveTo(281.1f, 314.38f)
                lineTo(246.16f, 341.74f)
                lineTo(0.5f, 28.08f)
                lineTo(35.43f, 0.72f)
                lineTo(281.1f, 314.38f)
                close()
            }
        }.build()

        return _GeoGateUX!!
    }

@Suppress("ObjectPropertyName")
private var _GeoGateUX: ImageVector? = null
