package app.puzzlet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.puzzlet.core.CircleSpec
import app.puzzlet.core.EllipseSpec
import app.puzzlet.core.PolygonSpec
import app.puzzlet.core.RingSpec
import app.puzzlet.core.RoundRectSpec
import app.puzzlet.core.SceneSpec

/**
 * The one renderer for scenes: board, gallery cards, thumbnails, celebration
 * all draw through here, so a picture always looks like itself at any size.
 * Scene geometry is unit-square Doubles; [side] is the on-screen side length.
 */
fun DrawScope.drawScene(spec: SceneSpec, side: Double) {
    require(side > 0.0) { "Scene needs positive side" }
    for (shape in spec.shapes) {
        val color = Color(shape.argb)
        fun px(v: Double) = (v * side).toFloat()
        when (shape) {
            is CircleSpec -> drawCircle(
                color,
                radius = px(shape.radius).coerceAtLeast(0.5f),
                center = Offset(px(shape.center.x), px(shape.center.y)),
            )
            is EllipseSpec -> {
                val c = Offset(px(shape.center.x), px(shape.center.y))
                val rx = px(shape.rx).coerceAtLeast(0.5f)
                val ry = px(shape.ry).coerceAtLeast(0.5f)
                withTransform({
                    if (shape.angleDeg != 0.0) rotate(shape.angleDeg.toFloat(), pivot = c)
                    scale(1f, ry / rx, pivot = c)
                }) {
                    drawCircle(color, rx, c)
                }
            }
            is RoundRectSpec -> {
                val tl = Offset(px(shape.x), px(shape.y))
                val sz = Size(px(shape.w), px(shape.h))
                val pivot = Offset(tl.x + sz.width / 2f, tl.y + sz.height / 2f)
                withTransform({
                    if (shape.angleDeg != 0.0) rotate(shape.angleDeg.toFloat(), pivot = pivot)
                }) {
                    drawRoundRect(color, tl, sz, CornerRadius(px(shape.cornerRadius)))
                }
            }
            is PolygonSpec -> drawPath(polygonPath(shape, side), color)
            is RingSpec -> {
                val c = Offset(px(shape.center.x), px(shape.center.y))
                val rx = px(shape.rx).coerceAtLeast(1f)
                val ry = px(shape.ry).coerceAtLeast(1f)
                val shrink = ((shape.rx - shape.thickness) / shape.rx).toFloat()
                val innerRx = (rx * shrink).coerceIn(rx * 0.25f, rx * 0.9f)
                val innerRy = ry * (innerRx / rx)
                val ring = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addOval(c, rx, ry)
                    addOval(c, innerRx, innerRy)
                }
                withTransform({
                    if (shape.angleDeg != 0.0) rotate(shape.angleDeg.toFloat(), pivot = c)
                }) {
                    drawPath(ring, color)
                }
            }
        }
    }
}

private fun polygonPath(spec: PolygonSpec, side: Double): Path {
    val pts = spec.points
    require(pts.size >= 3) { "A polygon needs at least 3 points" }
    return Path().apply {
        moveTo((pts[0].x * side).toFloat(), (pts[0].y * side).toFloat())
        for (i in 1 until pts.size) {
            lineTo((pts[i].x * side).toFloat(), (pts[i].y * side).toFloat())
        }
        close()
    }
}

/** A full oval from four cubics, the kappa approximation. */
private fun Path.addOval(c: Offset, rx: Float, ry: Float) {
    val k = 0.552284749831f
    moveTo(c.x + rx, c.y)
    cubicTo(c.x + rx, c.y + k * ry, c.x + k * rx, c.y + ry, c.x, c.y + ry)
    cubicTo(c.x - k * rx, c.y + ry, c.x - rx, c.y + k * ry, c.x - rx, c.y)
    cubicTo(c.x - rx, c.y - k * ry, c.x - k * rx, c.y - ry, c.x, c.y - ry)
    cubicTo(c.x + k * rx, c.y - ry, c.x + rx, c.y - k * ry, c.x + rx, c.y)
}

/** A square scene picture, clipped to rounded corners. */
@Composable
fun ScenePicture(
    spec: SceneSpec,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        drawScene(spec, size.width.toDouble())
    }
}
