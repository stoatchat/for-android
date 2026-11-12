package chat.stoat.composables.vectorassets

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val NewServer: ImageVector
    @Composable
    get() {
        if (_NewServer != null) {
            return _NewServer!!
        }
        _NewServer = ImageVector.Builder(
            name = "NewServer",
            defaultWidth = 570.dp,
            defaultHeight = 508.dp,
            viewportWidth = 570f,
            viewportHeight = 508f
        ).apply {
            path(fill = SolidColor(MaterialTheme.colorScheme.primaryContainer)) {
                moveTo(131.17f, 166.37f)
                lineTo(131.17f, 166.37f)
                arcTo(
                    131.03f,
                    131.03f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    262.2f,
                    297.4f
                )
                lineTo(262.2f, 297.4f)
                arcTo(
                    131.03f,
                    131.03f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    131.17f,
                    428.43f
                )
                lineTo(131.17f, 428.43f)
                arcTo(
                    131.03f,
                    131.03f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    0.14f,
                    297.4f
                )
                lineTo(0.14f, 297.4f)
                arcTo(
                    131.03f,
                    131.03f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    131.17f,
                    166.37f
                )
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onPrimaryContainer)) {
                moveTo(87.36f, 357.23f)
                lineTo(92.08f, 330.64f)
                horizontalLineTo(65.48f)
                lineTo(67.81f, 317.34f)
                horizontalLineTo(94.4f)
                lineTo(101.45f, 277.45f)
                horizontalLineTo(74.86f)
                lineTo(77.18f, 264.15f)
                horizontalLineTo(103.78f)
                lineTo(108.5f, 237.56f)
                horizontalLineTo(121.79f)
                lineTo(117.07f, 264.15f)
                horizontalLineTo(156.96f)
                lineTo(161.68f, 237.56f)
                horizontalLineTo(174.98f)
                lineTo(170.26f, 264.15f)
                horizontalLineTo(196.85f)
                lineTo(194.53f, 277.45f)
                horizontalLineTo(167.93f)
                lineTo(160.89f, 317.34f)
                horizontalLineTo(187.48f)
                lineTo(185.15f, 330.64f)
                horizontalLineTo(158.56f)
                lineTo(153.84f, 357.23f)
                horizontalLineTo(140.54f)
                lineTo(145.26f, 330.64f)
                horizontalLineTo(105.37f)
                lineTo(100.65f, 357.23f)
                horizontalLineTo(87.36f)
                close()
                moveTo(114.75f, 277.45f)
                lineTo(107.7f, 317.34f)
                horizontalLineTo(147.59f)
                lineTo(154.64f, 277.45f)
                horizontalLineTo(114.75f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.secondaryContainer)) {
                moveTo(369.52f, 314.28f)
                lineTo(369.52f, 314.28f)
                arcTo(
                    96.66f,
                    96.66f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    466.18f,
                    410.94f
                )
                lineTo(466.18f, 410.94f)
                arcTo(
                    96.66f,
                    96.66f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    369.52f,
                    507.6f
                )
                lineTo(369.52f, 507.6f)
                arcTo(
                    96.66f,
                    96.66f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    272.86f,
                    410.94f
                )
                lineTo(272.86f, 410.94f)
                arcTo(
                    96.66f,
                    96.66f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    369.52f,
                    314.28f
                )
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onSecondaryContainer)) {
                moveTo(389.14f, 441.79f)
                verticalLineTo(451.6f)
                horizontalLineTo(320.48f)
                verticalLineTo(441.79f)
                curveTo(320.48f, 441.79f, 320.48f, 422.18f, 354.81f, 422.18f)
                curveTo(389.14f, 422.18f, 389.14f, 441.79f, 389.14f, 441.79f)
                close()
                moveTo(371.97f, 395.2f)
                curveTo(371.97f, 391.81f, 370.97f, 388.49f, 369.08f, 385.67f)
                curveTo(367.2f, 382.84f, 364.51f, 380.64f, 361.38f, 379.35f)
                curveTo(358.24f, 378.05f, 354.79f, 377.71f, 351.46f, 378.37f)
                curveTo(348.13f, 379.03f, 345.07f, 380.67f, 342.67f, 383.07f)
                curveTo(340.27f, 385.47f, 338.64f, 388.52f, 337.97f, 391.86f)
                curveTo(337.31f, 395.18f, 337.65f, 398.64f, 338.95f, 401.77f)
                curveTo(340.25f, 404.91f, 342.45f, 407.59f, 345.27f, 409.48f)
                curveTo(348.1f, 411.36f, 351.41f, 412.37f, 354.81f, 412.37f)
                curveTo(359.36f, 412.37f, 363.73f, 410.56f, 366.95f, 407.34f)
                curveTo(370.17f, 404.12f, 371.97f, 399.76f, 371.97f, 395.2f)
                close()
                moveTo(388.84f, 422.18f)
                curveTo(391.86f, 424.51f, 394.33f, 427.48f, 396.07f, 430.86f)
                curveTo(397.82f, 434.25f, 398.8f, 437.98f, 398.95f, 441.79f)
                verticalLineTo(451.6f)
                horizontalLineTo(418.57f)
                verticalLineTo(441.79f)
                curveTo(418.57f, 441.79f, 418.57f, 423.99f, 388.84f, 422.18f)
                close()
                moveTo(384.23f, 378.04f)
                curveTo(380.86f, 378.02f, 377.56f, 379.03f, 374.77f, 380.93f)
                curveTo(377.75f, 385.09f, 379.35f, 390.08f, 379.35f, 395.2f)
                curveTo(379.35f, 400.32f, 377.75f, 405.31f, 374.77f, 409.48f)
                curveTo(377.56f, 411.38f, 380.86f, 412.38f, 384.23f, 412.37f)
                curveTo(388.79f, 412.37f, 393.15f, 410.56f, 396.37f, 407.34f)
                curveTo(399.59f, 404.12f, 401.4f, 399.76f, 401.4f, 395.2f)
                curveTo(401.4f, 390.65f, 399.59f, 386.29f, 396.37f, 383.07f)
                curveTo(393.15f, 379.85f, 388.79f, 378.04f, 384.23f, 378.04f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.errorContainer)) {
                moveTo(202.3f, 40.46f)
                lineTo(202.3f, 40.46f)
                arcTo(
                    59.91f,
                    59.91f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    262.2f,
                    100.36f
                )
                lineTo(262.2f, 100.36f)
                arcTo(
                    59.91f,
                    59.91f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    202.3f,
                    160.27f
                )
                lineTo(202.3f, 160.27f)
                arcTo(
                    59.91f,
                    59.91f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    142.39f,
                    100.36f
                )
                lineTo(142.39f, 100.36f)
                arcTo(
                    59.91f,
                    59.91f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    202.3f,
                    40.46f
                )
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onErrorContainer)) {
                moveTo(202.3f, 67.27f)
                curveTo(203.91f, 67.27f, 205.46f, 67.91f, 206.6f, 69.05f)
                curveTo(207.74f, 70.19f, 208.38f, 71.73f, 208.38f, 73.34f)
                curveTo(208.38f, 75.59f, 207.16f, 77.57f, 205.34f, 78.6f)
                verticalLineTo(82.46f)
                horizontalLineTo(208.38f)
                curveTo(214.02f, 82.46f, 219.43f, 84.71f, 223.42f, 88.7f)
                curveTo(227.41f, 92.69f, 229.65f, 98.1f, 229.65f, 103.74f)
                horizontalLineTo(232.69f)
                curveTo(233.5f, 103.74f, 234.27f, 104.06f, 234.84f, 104.63f)
                curveTo(235.41f, 105.2f, 235.73f, 105.97f, 235.73f, 106.78f)
                verticalLineTo(115.9f)
                curveTo(235.73f, 116.71f, 235.41f, 117.48f, 234.84f, 118.05f)
                curveTo(234.27f, 118.62f, 233.5f, 118.94f, 232.69f, 118.94f)
                horizontalLineTo(229.65f)
                verticalLineTo(121.98f)
                curveTo(229.65f, 123.59f, 229.01f, 125.14f, 227.87f, 126.28f)
                curveTo(226.73f, 127.42f, 225.19f, 128.06f, 223.57f, 128.06f)
                horizontalLineTo(181.02f)
                curveTo(179.41f, 128.06f, 177.86f, 127.42f, 176.72f, 126.28f)
                curveTo(175.58f, 125.14f, 174.94f, 123.59f, 174.94f, 121.98f)
                verticalLineTo(118.94f)
                horizontalLineTo(171.9f)
                curveTo(171.1f, 118.94f, 170.32f, 118.62f, 169.75f, 118.05f)
                curveTo(169.18f, 117.48f, 168.86f, 116.71f, 168.86f, 115.9f)
                verticalLineTo(106.78f)
                curveTo(168.86f, 105.97f, 169.18f, 105.2f, 169.75f, 104.63f)
                curveTo(170.32f, 104.06f, 171.1f, 103.74f, 171.9f, 103.74f)
                horizontalLineTo(174.94f)
                curveTo(174.94f, 98.1f, 177.18f, 92.69f, 181.17f, 88.7f)
                curveTo(185.16f, 84.71f, 190.57f, 82.46f, 196.22f, 82.46f)
                horizontalLineTo(199.26f)
                verticalLineTo(78.6f)
                curveTo(197.43f, 77.57f, 196.22f, 75.59f, 196.22f, 73.34f)
                curveTo(196.22f, 71.73f, 196.86f, 70.19f, 198f, 69.05f)
                curveTo(199.14f, 67.91f, 200.68f, 67.27f, 202.3f, 67.27f)
                close()
                moveTo(188.62f, 100.7f)
                curveTo(186.6f, 100.7f, 184.67f, 101.5f, 183.25f, 102.93f)
                curveTo(181.82f, 104.35f, 181.02f, 106.29f, 181.02f, 108.3f)
                curveTo(181.02f, 110.32f, 181.82f, 112.25f, 183.25f, 113.67f)
                curveTo(184.67f, 115.1f, 186.6f, 115.9f, 188.62f, 115.9f)
                curveTo(190.63f, 115.9f, 192.57f, 115.1f, 193.99f, 113.67f)
                curveTo(195.42f, 112.25f, 196.22f, 110.32f, 196.22f, 108.3f)
                curveTo(196.22f, 106.29f, 195.42f, 104.35f, 193.99f, 102.93f)
                curveTo(192.57f, 101.5f, 190.63f, 100.7f, 188.62f, 100.7f)
                close()
                moveTo(215.98f, 100.7f)
                curveTo(213.96f, 100.7f, 212.03f, 101.5f, 210.6f, 102.93f)
                curveTo(209.18f, 104.35f, 208.38f, 106.29f, 208.38f, 108.3f)
                curveTo(208.38f, 110.32f, 209.18f, 112.25f, 210.6f, 113.67f)
                curveTo(212.03f, 115.1f, 213.96f, 115.9f, 215.98f, 115.9f)
                curveTo(217.99f, 115.9f, 219.92f, 115.1f, 221.35f, 113.67f)
                curveTo(222.77f, 112.25f, 223.57f, 110.32f, 223.57f, 108.3f)
                curveTo(223.57f, 106.29f, 222.77f, 104.35f, 221.35f, 102.93f)
                curveTo(219.92f, 101.5f, 217.99f, 100.7f, 215.98f, 100.7f)
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.tertiaryContainer)) {
                moveTo(421.36f, 0.4f)
                lineTo(421.36f, 0.4f)
                arcTo(
                    148.5f,
                    148.5f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    569.86f,
                    148.9f
                )
                lineTo(569.86f, 148.9f)
                arcTo(
                    148.5f,
                    148.5f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    421.36f,
                    297.4f
                )
                lineTo(421.36f, 297.4f)
                arcTo(
                    148.5f,
                    148.5f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    272.86f,
                    148.9f
                )
                lineTo(272.86f, 148.9f)
                arcTo(
                    148.5f,
                    148.5f,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    421.36f,
                    0.4f
                )
                close()
            }
            path(fill = SolidColor(MaterialTheme.colorScheme.onTertiaryContainer)) {
                moveTo(421.36f, 171.5f)
                curveTo(427.47f, 171.5f, 432.67f, 169.24f, 437.26f, 164.8f)
                curveTo(441.71f, 160.2f, 443.97f, 155f, 443.97f, 148.9f)
                curveTo(443.97f, 142.8f, 441.71f, 137.6f, 437.26f, 133f)
                curveTo(432.67f, 128.56f, 427.47f, 126.3f, 421.36f, 126.3f)
                curveTo(415.26f, 126.3f, 410.06f, 128.56f, 405.47f, 133f)
                curveTo(401.02f, 137.6f, 398.76f, 142.8f, 398.76f, 148.9f)
                curveTo(398.76f, 155f, 401.02f, 160.2f, 405.47f, 164.8f)
                curveTo(410.06f, 169.24f, 415.26f, 171.5f, 421.36f, 171.5f)
                close()
                moveTo(421.36f, 73.56f)
                curveTo(442.08f, 73.56f, 459.79f, 81.09f, 474.48f, 95.78f)
                curveTo(489.17f, 110.47f, 496.71f, 128.18f, 496.71f, 148.9f)
                verticalLineTo(159.83f)
                curveTo(496.71f, 167.36f, 494.07f, 173.76f, 489.17f, 179.04f)
                curveTo(483.9f, 184.09f, 477.87f, 186.57f, 470.34f, 186.57f)
                curveTo(461.3f, 186.57f, 453.84f, 182.81f, 448.19f, 175.27f)
                curveTo(440.65f, 182.81f, 431.76f, 186.57f, 421.36f, 186.57f)
                curveTo(411.04f, 186.57f, 402.15f, 182.81f, 394.69f, 175.57f)
                curveTo(387.46f, 168.11f, 383.69f, 159.3f, 383.69f, 148.9f)
                curveTo(383.69f, 138.58f, 387.46f, 129.69f, 394.69f, 122.23f)
                curveTo(402.15f, 115f, 411.04f, 111.23f, 421.36f, 111.23f)
                curveTo(431.76f, 111.23f, 440.58f, 115f, 448.04f, 122.23f)
                curveTo(455.27f, 129.69f, 459.04f, 138.58f, 459.04f, 148.9f)
                verticalLineTo(159.83f)
                curveTo(459.04f, 162.91f, 460.24f, 165.63f, 462.5f, 167.96f)
                curveTo(464.76f, 170.3f, 467.4f, 171.5f, 470.34f, 171.5f)
                curveTo(473.5f, 171.5f, 476.14f, 170.3f, 478.4f, 167.96f)
                curveTo(480.66f, 165.63f, 481.64f, 162.91f, 481.64f, 159.83f)
                verticalLineTo(148.9f)
                curveTo(481.64f, 132.4f, 475.84f, 118.24f, 463.93f, 106.33f)
                curveTo(452.03f, 94.43f, 437.86f, 88.62f, 421.36f, 88.62f)
                curveTo(404.86f, 88.62f, 390.7f, 94.43f, 378.8f, 106.33f)
                curveTo(366.89f, 118.24f, 361.09f, 132.4f, 361.09f, 148.9f)
                curveTo(361.09f, 165.4f, 366.89f, 179.57f, 378.8f, 191.47f)
                curveTo(390.7f, 203.38f, 404.86f, 209.18f, 421.36f, 209.18f)
                horizontalLineTo(459.04f)
                verticalLineTo(224.24f)
                horizontalLineTo(421.36f)
                curveTo(400.64f, 224.24f, 382.94f, 216.71f, 368.25f, 202.02f)
                curveTo(353.55f, 187.33f, 346.02f, 169.62f, 346.02f, 148.9f)
                curveTo(346.02f, 128.18f, 353.55f, 110.47f, 368.25f, 95.78f)
                curveTo(382.94f, 81.09f, 400.64f, 73.56f, 421.36f, 73.56f)
                close()
            }
        }.build()

        return _NewServer!!
    }

@Suppress("ObjectPropertyName")
private var _NewServer: ImageVector? = null


@Preview(showBackground = true)
@Composable
fun NewServerPreview() {
    Image(
        imageVector = NewServer,
        contentDescription = null
    )
}
