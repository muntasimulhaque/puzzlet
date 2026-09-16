package io.github.muntasimulhaque.puzzlet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.puzzlet.core.CircleSpec
import io.github.muntasimulhaque.puzzlet.core.EllipseSpec
import io.github.muntasimulhaque.puzzlet.core.PolygonSpec
import io.github.muntasimulhaque.puzzlet.core.RingSpec
import io.github.muntasimulhaque.puzzlet.core.RoundRectSpec
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import kotlin.math.ceil

/**
 * The one renderer for scenes: board, gallery cards, thumbnails, celebration
 * all draw through here, so a picture always looks like itself at any size.
 * Scene geometry is unit-square Doubles; [side] is the on-screen side length.
 * A zero side is a real first-measure pass, not an error: draw nothing.
 */
fun DrawScope.drawScene(spec: SceneSpec, side: Double) {
    if (side <= 0.0) return
    for (shape in spec.shapes) {
        when (shape) {
            is CircleSpec -> drawCircleSpec(shape, side)
            is EllipseSpec -> drawEllipseSpec(shape, side)
            is RoundRectSpec -> drawRoundRectSpec(shape, side)
            is PolygonSpec -> drawPath(polygonPath(shape, side), Color(shape.argb))
            is RingSpec -> drawRingSpec(shape, side)
        }
    }
}

private fun px(v: Double, side: Double): Float = (v * side).toFloat()

private fun DrawScope.drawCircleSpec(spec: CircleSpec, side: Double) {
    drawCircle(
        Color(spec.argb),
        radius = px(spec.radius, side).coerceAtLeast(0.5f),
        center = Offset(px(spec.center.x, side), px(spec.center.y, side)),
    )
}

private fun DrawScope.drawEllipseSpec(spec: EllipseSpec, side: Double) {
    val c = Offset(px(spec.center.x, side), px(spec.center.y, side))
    val rx = px(spec.rx, side).coerceAtLeast(0.5f)
    val ry = px(spec.ry, side).coerceAtLeast(0.5f)
    withTransform({
        if (spec.angleDeg != 0.0) rotate(spec.angleDeg.toFloat(), pivot = c)
        scale(1f, ry / rx, pivot = c)
    }) {
        drawCircle(Color(spec.argb), rx, c)
    }
}

private fun DrawScope.drawRoundRectSpec(spec: RoundRectSpec, side: Double) {
    val tl = Offset(px(spec.x, side), px(spec.y, side))
    val sz = Size(px(spec.w, side), px(spec.h, side))
    val pivot = Offset(tl.x + sz.width / 2f, tl.y + sz.height / 2f)
    withTransform({
        if (spec.angleDeg != 0.0) rotate(spec.angleDeg.toFloat(), pivot = pivot)
    }) {
        drawRoundRect(Color(spec.argb), tl, sz, CornerRadius(px(spec.cornerRadius, side)))
    }
}

private fun DrawScope.drawRingSpec(spec: RingSpec, side: Double) {
    val c = Offset(px(spec.center.x, side), px(spec.center.y, side))
    val rx = px(spec.rx, side).coerceAtLeast(1f)
    val ry = px(spec.ry, side).coerceAtLeast(1f)
    val shrink = ((spec.rx - spec.thickness) / spec.rx).toFloat()
    val innerRx = (rx * shrink).coerceIn(rx * 0.25f, rx * 0.9f)
    val innerRy = ry * (innerRx / rx)
    val ring = Path().apply {
        fillType = PathFillType.EvenOdd
        addOval(c, rx, ry)
        addOval(c, innerRx, innerRy)
    }
    withTransform({
        if (spec.angleDeg != 0.0) rotate(spec.angleDeg.toFloat(), pivot = c)
    }) {
        drawPath(ring, Color(spec.argb))
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

/**
 * A square scene picture, clipped to rounded corners. The scene is rastered
 * once per real draw size through [sceneRaster] and every later frame draws
 * that one image, so a scroll moves pictures instead of redrawing a few
 * hundred shapes per card. The store is size-keyed and bounded; the play
 * field itself still draws vectors (see SceneRaster.kt).
 */
@Composable
fun ScenePicture(
    spec: SceneSpec,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                // The scene's side is the width; the tile is square by
                // layout, so width and height agree. Round up: the raster
                // then always covers the layout box, and the hair of
                // overdraw is clipped by the card's own rounded corners.
                val side = ceil(size.width.toDouble()).toInt().coerceAtLeast(1)
                val image = sceneRaster(spec, side)
                onDrawBehind { drawImage(image = image, dstSize = IntSize(side, side)) }
            },
    ) { }
}
