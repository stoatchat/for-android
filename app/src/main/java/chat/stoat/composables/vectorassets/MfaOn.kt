package chat.stoat.composables.vectorassets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MfaOn: ImageVector
    @Composable
    get() {
        if (_MfaOn != null) {
            return _MfaOn!!
        }
        _MfaOn = ImageVector.Builder(
            name = "MfaOn",
            defaultWidth = 399.dp,
            defaultHeight = 321.dp,
            viewportWidth = 399f,
            viewportHeight = 321f
        ).apply {
            path(fill = SolidColor(MaterialTheme.colorScheme.surfaceContainer)) {
                moveTo(30f, 0f)
                lineTo(368.49f, 0f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = false, isPositiveArc = true, 398.49f, 30f)
                lineTo(398.49f, 290.14f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = false, isPositiveArc = true, 368.49f, 320.14f)
                lineTo(30f, 320.14f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 290.14f)
                lineTo(0f, 30f)
                arcTo(30f, 30f, 0f, isMoreThanHalf = false, isPositiveArc = true, 30f, 0f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                moveTo(60.61f, 225.32f)
                curveTo(60.61f, 218.69f, 65.99f, 213.32f, 72.61f, 213.32f)
                horizontalLineTo(100.73f)
                curveTo(102.94f, 213.32f, 104.73f, 215.11f, 104.73f, 217.32f)
                verticalLineTo(262.76f)
                curveTo(104.73f, 264.97f, 102.94f, 266.76f, 100.73f, 266.76f)
                horizontalLineTo(72.61f)
                curveTo(65.99f, 266.76f, 60.61f, 261.39f, 60.61f, 254.76f)
                verticalLineTo(225.32f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onSurface)) {
                moveTo(82.67f, 240.04f)
                moveToRelative(-10.23f, 0f)
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    20.47f,
                    0f
                )
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -20.47f,
                    0f
                )
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                moveTo(111.24f, 213.32f)
                lineTo(147.36f, 213.32f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 151.36f, 217.32f)
                lineTo(151.36f, 262.76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 147.36f, 266.76f)
                lineTo(111.24f, 266.76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 107.24f, 262.76f)
                lineTo(107.24f, 217.32f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 111.24f, 213.32f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onSurface)) {
                moveTo(129.3f, 240.04f)
                moveToRelative(-10.23f, 0f)
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    20.47f,
                    0f
                )
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -20.47f,
                    0f
                )
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                moveTo(157.88f, 213.32f)
                lineTo(193.99f, 213.32f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 197.99f, 217.32f)
                lineTo(197.99f, 262.76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 193.99f, 266.76f)
                lineTo(157.88f, 266.76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 153.88f, 262.76f)
                lineTo(153.88f, 217.32f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 157.88f, 213.32f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onSurface)) {
                moveTo(175.93f, 240.04f)
                moveToRelative(-10.23f, 0f)
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    20.47f,
                    0f
                )
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -20.47f,
                    0f
                )
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                moveTo(204.5f, 213.32f)
                lineTo(240.62f, 213.32f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 244.62f, 217.32f)
                lineTo(244.62f, 262.76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 240.62f, 266.76f)
                lineTo(204.5f, 266.76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 200.5f, 262.76f)
                lineTo(200.5f, 217.32f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 204.5f, 213.32f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onSurface)) {
                moveTo(222.56f, 240.04f)
                moveToRelative(-10.23f, 0f)
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    20.47f,
                    0f
                )
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -20.47f,
                    0f
                )
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                moveTo(251.13f, 213.32f)
                lineTo(287.25f, 213.32f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 291.25f, 217.32f)
                lineTo(291.25f, 262.76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 287.25f, 266.76f)
                lineTo(251.13f, 266.76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 247.13f, 262.76f)
                lineTo(247.13f, 217.32f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 251.13f, 213.32f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onSurface)) {
                moveTo(269.19f, 240.04f)
                moveToRelative(-10.23f, 0f)
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    20.47f,
                    0f
                )
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -20.47f,
                    0f
                )
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                moveTo(293.77f, 217.32f)
                curveTo(293.77f, 215.11f, 295.56f, 213.32f, 297.77f, 213.32f)
                horizontalLineTo(325.88f)
                curveTo(332.51f, 213.32f, 337.88f, 218.69f, 337.88f, 225.32f)
                verticalLineTo(254.76f)
                curveTo(337.88f, 261.39f, 332.51f, 266.76f, 325.88f, 266.76f)
                horizontalLineTo(297.77f)
                curveTo(295.56f, 266.76f, 293.77f, 264.97f, 293.77f, 262.76f)
                verticalLineTo(217.32f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onSurface)) {
                moveTo(315.82f, 240.04f)
                moveToRelative(-10.23f, 0f)
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    20.47f,
                    0f
                )
                arcToRelative(
                    10.23f,
                    10.23f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -20.47f,
                    0f
                )
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.primary)) {
                moveTo(197.91f, 157.26f)
                curveTo(186.63f, 154.42f, 177.31f, 147.95f, 169.96f, 137.84f)
                curveTo(162.61f, 127.73f, 158.94f, 116.5f, 158.94f, 104.16f)
                verticalLineTo(74.43f)
                lineTo(197.91f, 59.82f)
                lineTo(236.89f, 74.43f)
                verticalLineTo(104.16f)
                curveTo(236.89f, 104.97f, 236.87f, 105.78f, 236.83f, 106.59f)
                curveTo(236.79f, 107.4f, 236.73f, 108.22f, 236.65f, 109.03f)
                curveTo(235.92f, 108.87f, 235.17f, 108.74f, 234.4f, 108.66f)
                curveTo(233.63f, 108.58f, 232.83f, 108.54f, 232.02f, 108.54f)
                curveTo(225.28f, 108.54f, 219.54f, 110.9f, 214.79f, 115.61f)
                curveTo(210.03f, 120.32f, 207.66f, 126.08f, 207.66f, 132.9f)
                verticalLineTo(153.61f)
                curveTo(206.12f, 154.42f, 204.53f, 155.13f, 202.91f, 155.74f)
                curveTo(201.28f, 156.35f, 199.62f, 156.86f, 197.91f, 157.26f)
                close()
                moveTo(221.54f, 157.26f)
                curveTo(220.41f, 157.26f, 219.43f, 156.86f, 218.62f, 156.05f)
                curveTo(217.81f, 155.24f, 217.4f, 154.26f, 217.4f, 153.12f)
                verticalLineTo(137.04f)
                curveTo(217.4f, 135.91f, 217.81f, 134.93f, 218.62f, 134.12f)
                curveTo(219.43f, 133.31f, 220.41f, 132.9f, 221.54f, 132.9f)
                horizontalLineTo(222.28f)
                verticalLineTo(128.03f)
                curveTo(222.28f, 125.35f, 223.23f, 123.06f, 225.14f, 121.15f)
                curveTo(227.05f, 119.24f, 229.34f, 118.29f, 232.02f, 118.29f)
                curveTo(234.7f, 118.29f, 236.99f, 119.24f, 238.9f, 121.15f)
                curveTo(240.81f, 123.06f, 241.77f, 125.35f, 241.77f, 128.03f)
                verticalLineTo(132.9f)
                horizontalLineTo(242.5f)
                curveTo(243.63f, 132.9f, 244.61f, 133.31f, 245.42f, 134.12f)
                curveTo(246.23f, 134.93f, 246.64f, 135.91f, 246.64f, 137.04f)
                verticalLineTo(153.12f)
                curveTo(246.64f, 154.26f, 246.23f, 155.24f, 245.42f, 156.05f)
                curveTo(244.61f, 156.86f, 243.63f, 157.26f, 242.5f, 157.26f)
                horizontalLineTo(221.54f)
                close()
                moveTo(227.15f, 132.9f)
                horizontalLineTo(236.89f)
                verticalLineTo(128.03f)
                curveTo(236.89f, 126.65f, 236.43f, 125.49f, 235.49f, 124.56f)
                curveTo(234.56f, 123.63f, 233.4f, 123.16f, 232.02f, 123.16f)
                curveTo(230.64f, 123.16f, 229.48f, 123.63f, 228.55f, 124.56f)
                curveTo(227.62f, 125.49f, 227.15f, 126.65f, 227.15f, 128.03f)
                verticalLineTo(132.9f)
                close()
            }
        }.build()

        return _MfaOn!!
    }

@Suppress("ObjectPropertyName")
private var _MfaOn: ImageVector? = null
