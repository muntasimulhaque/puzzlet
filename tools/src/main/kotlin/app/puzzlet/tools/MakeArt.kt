package app.puzzlet.tools

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Play Store art, drawn from the same design system as everything else:
 * lagoon teal, warm paper, the puzzle glyph, and Baloo 2 lettering (the same
 * font files the app bundles, so shelf, store and screen are one brand).
 *
 * Outputs (never hand-edited; regenerate with :tools:makeArt):
 *   play-store/feature-graphic-1024x500.png
 *   play-store/play-icon-512.png
 *
 * Content rules hold here too: no faces, no eyes, no creatures, no music
 * notes. The only texture behind the wordmark is a quiet eight-point star
 * lattice, geometry, nothing alive.
 */
object MakeArt {

    private const val TEAL = 0xFF0C7A64.toInt()
    private const val TEAL_DEEP = 0xFF085949.toInt()
    private const val PAPER = 0xFFFAF6EF.toInt()

    private fun font(rootDir: File, file: String, size: Float): Font =
        Font.createFont(
            Font.TRUETYPE_FONT,
            File(rootDir, "app/src/main/res/font/$file"),
        ).deriveFont(size)

    /** An n-point star path, the same proportions the app's confetti uses. */
    private fun starPath(cx: Double, cy: Double, rOuter: Double, rInner: Double, points: Int, rotationRad: Double): Path2D {
        val path = Path2D.Double()
        val step = Math.PI / points
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) rOuter else rInner
            val a = rotationRad + i * step
            val x = cx + r * cos(a)
            val y = cy + r * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.closePath()
        return path
    }

    /** The brand glyph, drawn large: body, two knobs, one blank, one flat. */
    private fun glyphPath(): Path2D {
        val piece = puzzlePiece()
        val bounds: Rectangle2D = piece.bounds2D
        val path = Path2D.Double(piece, AffineTransform())
        path.transform(AffineTransform.getTranslateInstance(-bounds.minX, -bounds.minY))
        return path
    }

    /** The 1024 x 500 feature graphic. */
    fun featureGraphic(rootDir: File): BufferedImage {
        val w = 1024
        val h = 500
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = begin(image)

        g.color = Color(TEAL)
        g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))

        // Quiet star lattice, half-dropped: geometry, not noise.
        g.color = Color(TEAL_DEEP)
        val step = 118.0
        for (row in -1..5) for (col in -1..9) {
            val cx = col * step + (if (row % 2 == 0) 0.0 else step / 2)
            val cy = row * step * 0.86
            g.fill(starPath(cx, cy, 26.0, 10.0, 8, Math.PI / 8))
        }

        // The glyph, held up at a slight tilt with a deep shadow.
        val glyph = glyphPath()
        val glyphBox = 236.0
        val s = glyphBox / 0.885
        val gx = 138.0
        val gy = (h - glyphBox) / 2.0
        val at = AffineTransform()
        at.translate(gx + 10.0, gy + 14.0)
        at.rotate(Math.toRadians(-8.0))
        at.scale(s, s)
        g.color = Color(TEAL_DEEP)
        g.transform(at)
        g.fill(glyph)
        g.dispose()

        val g2 = begin(image)
        val at2 = AffineTransform()
        at2.translate(gx, gy)
        at2.rotate(Math.toRadians(-8.0))
        at2.scale(s, s)
        g2.color = Color(PAPER)
        g2.transform(at2)
        g2.fill(glyph)
        g2.dispose()

        // The wordmark and the promise, in the bundled Baloo 2.
        val g3 = begin(image)
        val title = font(rootDir, "baloo2_extrabold.ttf", 132f)
        val tagline = font(rootDir, "baloo2_bold.ttf", 36f)
        g3.color = Color(PAPER)
        g3.font = title
        g3.drawString("Puzzlet", 430f, 292f)
        g3.font = tagline
        g3.color = Color(0xCCFAF6EF.toInt())
        g3.drawString("A calm jigsaw for small hands.", 436f, 352f)
        g3.dispose()
        return image
    }

    /** The 512 x 512 store icon: the launcher tile, full bleed. */
    fun storeIcon(): BufferedImage {
        val size = 512
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = begin(image)
        g.color = Color(TEAL)
        val r = size * 0.19
        g.fill(RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), r * 2, r * 2))
        paintGlyph(g, size.toDouble(), 0.60, IconDesign.PAPER)
        g.dispose()
        return image
    }

    private fun begin(image: BufferedImage): java.awt.Graphics2D {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        return g
    }

    private fun paintGlyph(g: java.awt.Graphics2D, canvas: Double, glyphFraction: Double, argb: Int) {
        val piece = glyphPath()
        val bounds = piece.bounds2D
        val box = canvas * glyphFraction
        val scale = box / bounds.height.coerceAtLeast(bounds.width)
        val tx = (canvas - bounds.width * scale) / 2.0
        val ty = (canvas - bounds.height * scale) / 2.0
        g.transform(AffineTransform(scale, 0.0, 0.0, scale, tx, ty))
        g.color = Color(argb, true)
        g.fill(piece)
    }
}

fun main(args: Array<String>) {
    val rootDir = File(args[0])
    val outDir = File(rootDir, "play-store")
    outDir.mkdirs()
    ImageIO.write(MakeArt.featureGraphic(rootDir), "png", File(outDir, "feature-graphic-1024x500.png"))
    ImageIO.write(MakeArt.storeIcon(), "png", File(outDir, "play-icon-512.png"))
    println("makeArt: wrote the feature graphic and the store icon under ${outDir.path}")
}
