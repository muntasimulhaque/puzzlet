package io.github.muntasimulhaque.puzzlet.tools

import io.github.muntasimulhaque.puzzlet.core.CircleSpec
import io.github.muntasimulhaque.puzzlet.core.EllipseSpec
import io.github.muntasimulhaque.puzzlet.core.PolygonSpec
import io.github.muntasimulhaque.puzzlet.core.RingSpec
import io.github.muntasimulhaque.puzzlet.core.RoundRectSpec
import io.github.muntasimulhaque.puzzlet.core.SceneSpec
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D

/**
 * The one Java2D renderer for scene data, shared by the picture sheet and
 * the wash sheet: every review image draws the game's own pictures, so the
 * owner judges the real thing, never a redraw of it.
 */
internal fun drawSceneJava2D(g: Graphics2D, spec: SceneSpec, ox: Double, oy: Double, side: Double) {
    for (shape in spec.shapes) {
        g.color = Color((shape.argb and 0xFFFFFFFFL).toInt(), true)
        when (shape) {
            is CircleSpec -> {
                val d = shape.radius * 2.0 * side
                g.fill(
                    Ellipse2D.Double(
                        ox + (shape.center.x - shape.radius) * side,
                        oy + (shape.center.y - shape.radius) * side,
                        d, d,
                    ),
                )
            }
            is EllipseSpec -> rotated(g, ox + shape.center.x * side, oy + shape.center.y * side, shape.angleDeg) {
                val d = shape.rx * 2.0 * side
                val h = shape.ry * 2.0 * side
                fill(Ellipse2D.Double(-d / 2.0, -h / 2.0, d, h))
            }
            is RoundRectSpec -> {
                val cx = ox + (shape.x + shape.w / 2.0) * side
                val cy = oy + (shape.y + shape.h / 2.0) * side
                rotated(g, cx, cy, shape.angleDeg) {
                    val w = shape.w * side
                    val h = shape.h * side
                    val r = shape.cornerRadius * side * 2.0
                    fill(RoundRectangle2D.Double(-w / 2.0, -h / 2.0, w, h, r, r))
                }
            }
            is PolygonSpec -> {
                val path = Path2D.Double()
                shape.points.forEachIndexed { i, p ->
                    val x = ox + p.x * side
                    val y = oy + p.y * side
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.closePath()
                g.fill(path)
            }
            is RingSpec -> {
                val cx = ox + shape.center.x * side
                val cy = oy + shape.center.y * side
                val shrink = ((shape.rx - shape.thickness) / shape.rx).coerceIn(0.25, 0.9)
                val path = Path2D.Double(Path2D.WIND_EVEN_ODD)
                path.append(oval(cx, cy, shape.rx * side, shape.ry * side, shape.angleDeg), false)
                path.append(oval(cx, cy, shape.rx * side * shrink, shape.ry * side * shrink, shape.angleDeg), false)
                g.fill(path)
            }
        }
    }
}

private fun oval(cx: Double, cy: Double, rx: Double, ry: Double, angleDeg: Double): java.awt.Shape {
    val e = Ellipse2D.Double(cx - rx, cy - ry, rx * 2.0, ry * 2.0)
    if (angleDeg == 0.0) return e
    return java.awt.geom.AffineTransform
        .getRotateInstance(Math.toRadians(angleDeg), cx, cy)
        .createTransformedShape(e)
}

private inline fun rotated(g: Graphics2D, cx: Double, cy: Double, angleDeg: Double, draw: Graphics2D.() -> Unit) {
    if (angleDeg == 0.0) {
        val saved = g.transform
        g.translate(cx, cy)
        g.draw()
        g.transform = saved
    } else {
        val saved = g.transform
        g.translate(cx, cy)
        g.rotate(Math.toRadians(angleDeg))
        g.draw()
        g.transform = saved
    }
}
