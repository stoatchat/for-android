package chat.stoat.composables.vectorassets

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val NotificationRationale: ImageVector
    @Composable
    get() {
        if (_NotificationRationale != null) {
            return _NotificationRationale!!
        }
        _NotificationRationale = ImageVector.Builder(
            name = "NotificationRationale",
            defaultWidth = 512.dp,
            defaultHeight = 512.dp,
            viewportWidth = 132.29f,
            viewportHeight = 132.29f
        ).apply {
            path(
                fill = SolidColor(MaterialTheme.colorScheme.secondary),
                strokeLineWidth = 1.34108f
            ) {
                moveToRelative(103.09f, 21.16f)
                lineToRelative(2.77f, 11.75f)
                curveToRelative(0.34f, 1.44f, -0.56f, 2.89f, -2f, 3.23f)
                lineToRelative(-18.27f, 4.31f)
                lineToRelative(-3.99f, 6.45f)
                lineToRelative(-5.54f, -23.5f)
                curveToRelative(-0.34f, -1.44f, 0.56f, -2.89f, 2f, -3.23f)
                lineToRelative(13.18f, -3.11f)
                curveToRelative(-0.04f, 0.42f, 0.08f, 0.94f, 0.18f, 1.34f)
                curveToRelative(0.86f, 3.65f, 4.41f, 5.85f, 8.07f, 4.99f)
                curveToRelative(1.44f, -0.34f, 2.75f, -1.2f, 3.61f, -2.23f)
                moveToRelative(-9.06f, -3.37f)
                curveToRelative(0.52f, 2.22f, 2.62f, 3.52f, 4.84f, 2.99f)
                curveToRelative(2.22f, -0.52f, 3.52f, -2.62f, 2.99f, -4.84f)
                curveToRelative(-0.52f, -2.22f, -2.62f, -3.52f, -4.84f, -2.99f)
                curveToRelative(-2.22f, 0.52f, -3.52f, 2.62f, -2.99f, 4.84f)
                close()
            }
            path(
                fill = SolidColor(MaterialTheme.colorScheme.primary),
                strokeLineWidth = 2.45499f
            ) {
                moveToRelative(56.15f, 88.62f)
                verticalLineToRelative(2.46f)
                horizontalLineTo(11.96f)
                verticalLineToRelative(-2.46f)
                lineToRelative(4.91f, -4.91f)
                verticalLineTo(68.98f)
                curveToRelative(0f, -7.61f, 4.98f, -14.31f, 12.28f, -16.47f)
                curveToRelative(0f, -0.25f, 0f, -0.47f, 0f, -0.71f)
                arcToRelative(
                    4.91f,
                    4.91f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    4.91f,
                    -4.91f
                )
                arcToRelative(
                    4.91f,
                    4.91f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    4.91f,
                    4.91f
                )
                curveToRelative(0f, 0.25f, 0f, 0.47f, 0f, 0.71f)
                curveToRelative(7.29f, 2.16f, 12.28f, 8.86f, 12.28f, 16.47f)
                verticalLineTo(83.71f)
                lineToRelative(4.91f, 4.91f)
                moveToRelative(-17.19f, 4.91f)
                arcToRelative(
                    4.91f,
                    4.91f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -4.91f,
                    4.91f
                )
                arcToRelative(
                    4.91f,
                    4.91f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -4.91f,
                    -4.91f
                )
                moveToRelative(23.94f, -43.72f)
                lineToRelative(-3.49f, 3.49f)
                curveToRelative(4.2f, 4.15f, 6.55f, 9.8f, 6.55f, 15.69f)
                horizontalLineToRelative(4.91f)
                curveToRelative(0f, -7.19f, -2.85f, -14.12f, -7.98f, -19.17f)
                moveTo(7.05f, 68.98f)
                horizontalLineToRelative(4.91f)
                curveToRelative(0f, -5.89f, 2.36f, -11.54f, 6.55f, -15.69f)
                lineTo(15.02f, 49.81f)
                curveTo(9.89f, 54.87f, 7.05f, 61.79f, 7.05f, 68.98f)
                close()
            }
            path(
                fill = SolidColor(MaterialTheme.colorScheme.tertiary),
                strokeLineWidth = 0.735562f
            ) {
                moveToRelative(117.49f, 66.53f)
                curveToRelative(-1.27f, -0.59f, -2.83f, -0.03f, -3.42f, 1.24f)
                curveToRelative(-0.59f, 1.27f, -0.03f, 2.83f, 1.24f, 3.42f)
                curveToRelative(1.27f, 0.59f, 2.83f, 0.03f, 3.42f, -1.24f)
                curveToRelative(0.59f, -1.27f, 0.03f, -2.83f, -1.24f, -3.42f)
                moveToRelative(-5.09f, 1.68f)
                lineToRelative(-6.13f, -2.88f)
                curveToRelative(-0.73f, -0.34f, -1.05f, -1.22f, -0.71f, -1.96f)
                lineToRelative(4.38f, -9.32f)
                curveToRelative(0.35f, -0.74f, 1.22f, -1.05f, 1.96f, -0.71f)
                lineToRelative(0.67f, 0.31f)
                lineToRelative(0.63f, -1.33f)
                lineToRelative(1.33f, 0.63f)
                lineToRelative(-0.63f, 1.33f)
                lineToRelative(5.33f, 2.5f)
                lineToRelative(0.63f, -1.33f)
                lineToRelative(1.33f, 0.63f)
                lineToRelative(-0.63f, 1.33f)
                lineToRelative(0.67f, 0.31f)
                curveToRelative(0.73f, 0.34f, 1.05f, 1.22f, 0.71f, 1.96f)
                lineToRelative(-2.88f, 6.13f)
                curveToRelative(-0.29f, -0.24f, -0.6f, -0.45f, -0.93f, -0.61f)
                curveToRelative(-0.11f, -0.05f, -0.22f, -0.1f, -0.34f, -0.14f)
                lineToRelative(1.88f, -4.02f)
                lineToRelative(-9.32f, -4.38f)
                lineToRelative(-3.44f, 7.32f)
                lineToRelative(6.01f, 2.82f)
                curveToRelative(-0.07f, 0.1f, -0.12f, 0.21f, -0.18f, 0.32f)
                curveToRelative(-0.16f, 0.33f, -0.26f, 0.69f, -0.33f, 1.06f)
                close()
            }
            path(
                fill = SolidColor(MaterialTheme.colorScheme.error),
                strokeLineWidth = 1.53082f
            ) {
                moveToRelative(112.36f, 116.79f)
                lineToRelative(-0.45f, 3.03f)
                lineToRelative(-18.17f, -2.73f)
                lineToRelative(0.45f, -3.03f)
                curveToRelative(0f, 0f, 0.91f, -6.06f, 9.99f, -4.69f)
                curveToRelative(9.08f, 1.36f, 8.17f, 7.42f, 8.17f, 7.42f)
                moveToRelative(-2.5f, -14.31f)
                arcToRelative(
                    4.59f,
                    4.59f,
                    8.54f,
                    isMoreThanHalf = true,
                    isPositiveArc = false,
                    -5.22f,
                    3.86f
                )
                arcToRelative(
                    4.59f,
                    4.59f,
                    8.54f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    5.22f,
                    -3.86f
                )
                moveToRelative(3.69f, 8.39f)
                arcToRelative(
                    8.57f,
                    8.57f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    1.83f,
                    6.37f
                )
                lineToRelative(-0.45f, 3.03f)
                lineToRelative(4.54f, 0.68f)
                lineToRelative(0.45f, -3.03f)
                curveToRelative(0f, 0f, 0.78f, -5.22f, -6.37f, -7.06f)
                moveToRelative(0.01f, -12.47f)
                arcToRelative(
                    4.45f,
                    4.45f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    -1.38f,
                    0.01f
                )
                arcToRelative(
                    7.65f,
                    7.65f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    -1.3f,
                    8.66f
                )
                arcToRelative(
                    4.45f,
                    4.45f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    1.32f,
                    0.41f
                )
                arcToRelative(
                    4.59f,
                    4.59f,
                    8.54f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    1.36f,
                    -9.08f
                )
                moveToRelative(-16.27f, 5.3f)
                lineToRelative(-4.54f, -0.68f)
                lineToRelative(0.68f, -4.54f)
                lineToRelative(-3.03f, -0.45f)
                lineToRelative(-0.68f, 4.54f)
                lineToRelative(-4.54f, -0.68f)
                lineToRelative(-0.45f, 3.03f)
                lineToRelative(4.54f, 0.68f)
                lineToRelative(-0.68f, 4.54f)
                lineToRelative(3.03f, 0.45f)
                lineToRelative(0.68f, -4.54f)
                lineToRelative(4.54f, 0.68f)
                close()
            }
        }.build()

        return _NotificationRationale!!
    }

@Suppress("ObjectPropertyName")
private var _NotificationRationale: ImageVector? = null

@Preview(showBackground = true)
@Composable
fun NotificationRationalePreview() {
    Image(
        imageVector = NotificationRationale,
        contentDescription = null
    )
}
