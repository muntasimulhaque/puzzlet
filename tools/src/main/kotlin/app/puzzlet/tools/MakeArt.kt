package app.puzzlet.tools

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
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
 * lagoon teal, warm paper, the four-armed friendship piece (the owner's
 * mark: one jigsaw piece reaching out in all four directions), and Baloo 2
 * lettering (the same font files the app bundles, so shelf, store and
 * screen are one brand).
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
    private const val HONEY = 0xFFF0B429.toInt()

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
        g.dispose()

        // The friendship piece, centred in its own quiet space: four arms
        // reaching out. A deep shadow gives it weight on the lattice.
        val g2 = begin(image)
        paintBrandPiece(g2, 248.0, 260.0, 310.0, TEAL_DEEP)
        g2.dispose()

        val g3 = begin(image)
        paintBrandPiece(g3, 240.0, 250.0, 310.0, PAPER)
        g3.dispose()

        // The wordmark and the promise, in the bundled Baloo 2, with a
        // honey underline: the celebration accent, earned.
        val g4 = begin(image)
        val title = font(rootDir, "baloo2_extrabold.ttf", 132f)
        val tagline = font(rootDir, "baloo2_bold.ttf", 36f)
        g4.color = Color(PAPER)
        g4.font = title
        g4.drawString("Puzzlet", 460f, 280f)
        g4.color = Color(HONEY)
        g4.fill(RoundRectangle2D.Double(464.0, 306.0, 150.0, 13.0, 6.5, 6.5))
        g4.font = tagline
        g4.color = Color(0xCCFAF6EF.toInt())
        g4.drawString("A calm jigsaw for small hands.", 464f, 368f)
        g4.dispose()
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
        paintBrandPiece(g, size / 2.0, size / 2.0, size * 0.62, PAPER)
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
}

fun main(args: Array<String>) {
    val rootDir = File(args[0])
    val outDir = File(rootDir, "play-store")
    outDir.mkdirs()
    ImageIO.write(MakeArt.featureGraphic(rootDir), "png", File(outDir, "feature-graphic-1024x500.png"))
    ImageIO.write(MakeArt.storeIcon(), "png", File(outDir, "play-icon-512.png"))
    println("makeArt: wrote the feature graphic and the store icon under ${outDir.path}")
}
