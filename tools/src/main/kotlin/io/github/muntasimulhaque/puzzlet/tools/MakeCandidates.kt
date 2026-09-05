package io.github.muntasimulhaque.puzzlet.tools

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.exp

/**
 * Candidate marks for the owner to point at (D-036 lesson: render takes,
 * put them in one folder, let the owner point). Nothing here overwrites
 * the shipped icon or art; checkIcons and the store stay pinned until a
 * take is chosen and MakeIcons plus MakeArt are updated to match it.
 *
 * All takes keep the chosen palette (lagoon, paper, honey once) and Baloo,
 * and all subjects stay inanimate. Folder: play-store/candidates/icon-v2.
 */
object Candidates {
    const val TEAL = 0xFF0C7A64.toInt()
    const val DEEP = 0xFF085949.toInt()
    const val PAPER = 0xFFFAF6EF.toInt()
    const val HONEY = 0xFFF0B429.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()

    const val BODY = 0.40
    const val BODY_X = 0.30
    const val CORNER = 0.085
    const val NECK_W = 0.088
    const val NECK_L = 0.075
    const val HEAD_R = 0.078
    const val INECK_W = 0.070
    const val INECK_L = 0.058
    const val INHEAD_R = 0.060
}

private fun begin(image: BufferedImage): Graphics2D {
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    return g
}

/** Soft shadow kernel: small Gaussian, plain Java2D, no libraries. */
private fun blur(src: BufferedImage, radius: Int): BufferedImage {
    val size = radius * 2 + 1
    val data = FloatArray(size * size)
    var sum = 0.0
    for (y in 0 until size) for (x in 0 until size) {
        val dx = (x - radius).toDouble()
        val dy = (y - radius).toDouble()
        val v = exp(-(dx * dx + dy * dy) / (2.0 * radius * radius / 4.0))
        data[y * size + x] = v.toFloat()
        sum += v
    }
    for (i in data.indices) data[i] = (data[i] / sum).toFloat()
    return ConvolveOp(Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null).filter(src, null)
}

/** Teal tile with a calm vertical falloff, same hues, only light changes. */
private fun tileBase(size: Int): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    val r = size * 0.19
    g.paint = GradientPaint(0f, 0f, Color(Candidates.TEAL), 0f, size.toFloat(), Color(Candidates.DEEP))
    g.fill(RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), r * 2, r * 2))
    g.dispose()
    return image
}

/** One mushroom outie pointing up from edge y, centred at cx. */
private fun Area.addOutieUp(cx: Double, edgeY: Double) {
    val n = Candidates.NECK_W / 2.0
    add(Area(Rectangle2D.Double(cx - n, edgeY - Candidates.NECK_L, Candidates.NECK_W, Candidates.NECK_L + 0.01)))
    add(Area(Ellipse2D.Double(cx - Candidates.HEAD_R, edgeY - Candidates.NECK_L - Candidates.HEAD_R * 1.35, Candidates.HEAD_R * 2, Candidates.HEAD_R * 2)))
}

/** One mushroom outie pointing down, right, or left (mirrors of up). */
private fun Area.addOutieDown(cx: Double, edgeY: Double) {
    val n = Candidates.NECK_W / 2.0
    add(Area(Rectangle2D.Double(cx - n, edgeY - 0.01, Candidates.NECK_W, Candidates.NECK_L + 0.01)))
    add(Area(Ellipse2D.Double(cx - Candidates.HEAD_R, edgeY + Candidates.NECK_L - Candidates.HEAD_R * 0.65, Candidates.HEAD_R * 2, Candidates.HEAD_R * 2)))
}

private fun Area.addOutieRight(edgeX: Double, cy: Double) {
    val n = Candidates.NECK_W / 2.0
    add(Area(Rectangle2D.Double(edgeX - 0.01, cy - n, Candidates.NECK_L + 0.01, Candidates.NECK_W)))
    add(Area(Ellipse2D.Double(edgeX + Candidates.NECK_L - Candidates.HEAD_R * 0.65, cy - Candidates.HEAD_R, Candidates.HEAD_R * 2, Candidates.HEAD_R * 2)))
}

private fun Area.addOutieLeft(edgeX: Double, cy: Double) {
    val n = Candidates.NECK_W / 2.0
    add(Area(Rectangle2D.Double(edgeX - Candidates.NECK_L, cy - n, Candidates.NECK_L + 0.01, Candidates.NECK_W)))
    add(Area(Ellipse2D.Double(edgeX - Candidates.NECK_L - Candidates.HEAD_R * 1.35, cy - Candidates.HEAD_R, Candidates.HEAD_R * 2, Candidates.HEAD_R * 2)))
}

/** One innie cut into the right edge, centred at cy. */
private fun Area.cutInnieRight(edgeX: Double, cy: Double) {
    val n = Candidates.INECK_W / 2.0
    subtract(Area(Rectangle2D.Double(edgeX - Candidates.INECK_L, cy - n, Candidates.INECK_L + 0.02, Candidates.INECK_W)))
    subtract(Area(Ellipse2D.Double(edgeX - Candidates.INECK_L - Candidates.INHEAD_R * 0.9, cy - Candidates.INHEAD_R, Candidates.INHEAD_R * 2, Candidates.INHEAD_R * 2)))
}

private fun Area.cutInnieLeft(edgeX: Double, cy: Double) {
    val n = Candidates.INECK_W / 2.0
    subtract(Area(Rectangle2D.Double(edgeX - 0.02, cy - n, Candidates.INECK_L + 0.02, Candidates.INECK_W)))
    subtract(Area(Ellipse2D.Double(edgeX + Candidates.INECK_L - Candidates.INHEAD_R * 1.1, cy - Candidates.INHEAD_R, Candidates.INHEAD_R * 2, Candidates.INHEAD_R * 2)))
}

private fun body(): Area {
    val b = Candidates.BODY_X
    return Area(RoundRectangle2D.Double(b, b, Candidates.BODY, Candidates.BODY, Candidates.CORNER, Candidates.CORNER))
}

/** Take A: four mushroom outies, thick necks, big heads, same calm symmetry. */
fun candidatePieceA(): Area {
    val a = body()
    val b = Candidates.BODY_X
    val e = b + Candidates.BODY
    a.addOutieUp(0.5, b)
    a.addOutieDown(0.5, e)
    a.addOutieRight(e, 0.5)
    a.addOutieLeft(b, 0.5)
    return a
}

/** Take B: classic interlock, top and bottom outie, left and right innie. */
fun candidatePieceB(): Area {
    val a = body()
    val b = Candidates.BODY_X
    val e = b + Candidates.BODY
    a.addOutieUp(0.5, b)
    a.addOutieDown(0.5, e)
    a.cutInnieRight(e, 0.5)
    a.cutInnieLeft(b, 0.5)
    return a
}

/**
 * Take A2: option A outer shape, plus the four bottom holes joined in the
 * middle. Four pieces in a pinwheel, each bottom edge facing inward, each
 * outer knob facing out. The lobes overlap into one smooth clover, fully
 * enclosed, fourfold symmetric, no seams to vanish at small sizes.
 */
fun candidatePieceA2(): Area {
    val a = candidatePieceA()
    val d = 0.043
    val r = 0.051
    val lobes = listOf(0.0 to -d, d to 0.0, 0.0 to d, -d to 0.0)
    for ((dx, dy) in lobes) {
        a.subtract(Area(Ellipse2D.Double(0.5 + dx - r, 0.5 + dy - r, r * 2.0, r * 2.0)))
    }
    return a
}

/** Paint an area centred, spanning span px, in argb. */
private fun paintAt(g: Graphics2D, shape: Area, cx: Double, cy: Double, span: Double, argb: Int) {
    val bounds = shape.bounds2D
    val s = span / maxOf(bounds.width, bounds.height)
    val tx = cx - (bounds.centerX * s)
    val ty = cy - (bounds.centerY * s)
    val g2 = g.create() as Graphics2D
    g2.translate(tx, ty)
    g2.scale(s, s)
    g2.color = Color(argb, true)
    g2.fill(shape)
    g2.dispose()
}

/** Tile plus soft contact shadow plus piece plus top light. */
fun iconTile(size: Int, piece: Area, span: Double): BufferedImage {
    val tile = tileBase(size)
    val shadow = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val gs = begin(shadow)
    paintAt(gs, piece, size / 2.0, size / 2.0 + size * 0.022, span, Candidates.DEEP)
    gs.dispose()
    val soft = blur(shadow, maxOf(3, size / 64))
    val g = begin(tile)
    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f)
    g.drawImage(soft, 0, 0, null)
    g.composite = AlphaComposite.SrcOver
    paintAt(g, piece, size / 2.0, size / 2.0, span, Candidates.PAPER)
    val bounds = piece.bounds2D
    val s = span / maxOf(bounds.width, bounds.height)
    val t = AffineTransform.getTranslateInstance(
        size / 2.0 - bounds.centerX * s,
        size / 2.0 - bounds.centerY * s,
    )
    t.concatenate(AffineTransform.getScaleInstance(s, s))
    g.clip(piece.createTransformedArea(t))
    g.paint = GradientPaint(0f, size * 0.2f, Color(255, 255, 255, 34), 0f, size * 0.55f, Color(255, 255, 255, 0))
    g.fillRect(0, 0, size, size)
    g.dispose()
    return tile
}

/** Take C: two pieces about to click, outie facing innie across a gap. */
fun iconC(size: Int): BufferedImage {
    val tile = tileBase(size)
    val left = body()
    left.addOutieUp(0.5, Candidates.BODY_X)
    left.addOutieDown(0.5, Candidates.BODY_X + Candidates.BODY)
    left.addOutieRight(Candidates.BODY_X + Candidates.BODY, 0.5)
    left.addOutieLeft(Candidates.BODY_X, 0.5)
    val right = body()
    right.addOutieUp(0.5, Candidates.BODY_X)
    right.addOutieDown(0.5, Candidates.BODY_X + Candidates.BODY)
    right.cutInnieLeft(Candidates.BODY_X, 0.5)
    right.addOutieRight(Candidates.BODY_X + Candidates.BODY, 0.5)
    val span = size * 0.30
    val shadow = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val gs = begin(shadow)
    paintAt(gs, left, size * 0.36, size * 0.52, span, Candidates.DEEP)
    paintAt(gs, right, size * 0.64, size * 0.52, span, Candidates.DEEP)
    gs.dispose()
    val g = begin(tile)
    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f)
    g.drawImage(blur(shadow, maxOf(3, size / 64)), 0, size / 64, null)
    g.composite = AlphaComposite.SrcOver
    paintAt(g, left, size * 0.36, size * 0.50, span, Candidates.PAPER)
    paintAt(g, right, size * 0.64, size * 0.50, span, Candidates.PAPER)
    g.dispose()
    return tile
}

/** Take D: a 2 by 2 board, three seated, one cell open, one piece lifted. */
fun iconD(size: Int): BufferedImage {
    val tile = tileBase(size)
    val g = begin(tile)
    val m = size * 0.22
    val side = size - 2 * m
    g.color = Color(Candidates.PAPER)
    g.fill(RoundRectangle2D.Double(m, m * 1.15, side.toDouble(), side.toDouble(), size * 0.05, size * 0.05))
    g.color = Color(Candidates.DEEP)
    val gx = m + side / 2.0
    g.fill(Rectangle2D.Double(gx - size * 0.006, m * 1.15, size * 0.012, side.toDouble()))
    g.fill(Rectangle2D.Double(m, m * 1.15 + side / 2.0 - size * 0.006, side.toDouble(), size * 0.012))
    val openX = m + side * 0.75
    val openY = m * 1.15 + side * 0.25
    g.fill(RoundRectangle2D.Double(openX - side * 0.20, openY - side * 0.20, side * 0.40, side * 0.40, size * 0.03, size * 0.03))
    g.dispose()
    val lifted = candidatePieceB()
    val g2 = begin(tile)
    paintAt(g2, lifted, openX, openY - side * 0.06, side * 0.44, Candidates.PAPER)
    g2.dispose()
    return tile
}

private fun balooV2(rootDir: File, file: String, size: Float): Font =
    Font.createFont(Font.TRUETYPE_FONT, File(rootDir, "app/src/main/res/font/$file")).deriveFont(size)

/**
 * Baloo gaps above 96pt under Java2D (the shipped graphic has them),
 * so words render at 96pt and scale up. Same font, same shapes, clean.
 */
private fun drawCleanStringV2(g: Graphics2D, text: String, file: String, target: Float, argb: Int, x: Float, y: Float, rootDir: File) {
    val base = 96f
    val k = target / base
    val font = balooV2(rootDir, file, base)
    val tmp = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
    val g0 = begin(tmp)
    val bounds = font.getStringBounds(text, g0.fontRenderContext)
    g0.dispose()
    val sx = (-bounds.x + 4.0)
    val sy = (-bounds.y + 4.0)
    val small = BufferedImage(ceil(bounds.width + 8.0).toInt(), ceil(bounds.height + 8.0).toInt(), BufferedImage.TYPE_INT_ARGB)
    val g1 = begin(small)
    g1.font = font
    g1.color = Color(argb, true)
    g1.drawString(text, sx.toFloat(), sy.toFloat())
    g1.dispose()
    val bigW = ceil(small.width * k).toInt()
    val bigH = ceil(small.height * k).toInt()
    val big = BufferedImage(bigW, bigH, BufferedImage.TYPE_INT_ARGB)
    val g2 = begin(big)
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g2.drawImage(small, 0, 0, bigW, bigH, null)
    g2.dispose()
    g.drawImage(big, (x - sx * k).toInt(), (y - sy * k).toInt(), null)
}

/** Feature take FB: big interlock piece, calm type, no lattice behind words. */
fun featureB(rootDir: File): BufferedImage {
    val w = 1024
    val h = 500
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    g.paint = GradientPaint(0f, 0f, Color(Candidates.TEAL), 0f, h.toFloat(), Color(Candidates.DEEP))
    g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
    g.dispose()
    val piece = iconTile(360, candidatePieceB(), 360 * 0.62)
    val g2 = begin(image)
    g2.drawImage(piece, 96, 70, null)
    drawCleanStringV2(g2, "Puzzlet", "baloo2_extrabold.ttf", 132f, Candidates.PAPER, 500f, 272f, rootDir)
    g2.color = Color(Candidates.HONEY)
    g2.fill(RoundRectangle2D.Double(504.0, 298.0, 150.0, 13.0, 6.5, 6.5))
    drawCleanStringV2(g2, "A calm jigsaw for small hands.", "baloo2_bold.ttf", 36f, 0xE6FAF6EF.toInt(), 504f, 360f, rootDir)
    g2.dispose()
    return image
}

/** Feature take FD: the play moment, mini board left, words right. */
fun featureD(rootDir: File): BufferedImage {
    val w = 1024
    val h = 500
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = begin(image)
    g.paint = GradientPaint(0f, 0f, Color(Candidates.TEAL), 0f, h.toFloat(), Color(Candidates.DEEP))
    g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
    g.color = Color(Candidates.PAPER)
    g.fill(RoundRectangle2D.Double(96.0, 90.0, 320.0, 320.0, 36.0, 36.0))
    g.color = Color(Candidates.DEEP)
    g.fill(Rectangle2D.Double(252.0, 90.0, 8.0, 320.0))
    g.fill(Rectangle2D.Double(96.0, 246.0, 320.0, 8.0))
    g.fill(RoundRectangle2D.Double(276.0, 114.0, 116.0, 116.0, 20.0, 20.0))
    g.dispose()
    val g2 = begin(image)
    paintAt(g2, candidatePieceB(), 334.0, 158.0, 122.0, Candidates.PAPER)
    drawCleanStringV2(g2, "Puzzlet", "baloo2_extrabold.ttf", 124f, Candidates.PAPER, 470f, 268f, rootDir)
    g2.color = Color(Candidates.HONEY)
    g2.fill(RoundRectangle2D.Double(474.0, 292.0, 140.0, 12.0, 6.0, 6.0))
    drawCleanStringV2(g2, "Tray, drag, snap. No rush.", "baloo2_bold.ttf", 34f, 0xE6FAF6EF.toInt(), 474f, 352f, rootDir)
    g2.dispose()
    return image
}

fun writeCandidates(rootDir: File) {
    val out = File(rootDir, "play-store/candidates/icon-v2")
    out.mkdirs()
    val size = 512
    val span = size * 0.62
    ImageIO.write(iconTile(size, candidatePieceA(), span), "png", File(out, "icon-A-four-outie.png"))
    ImageIO.write(iconTile(size, candidatePieceA2(), span), "png", File(out, "icon-A2-clover.png"))
    ImageIO.write(iconTile(size, candidatePieceB(), span), "png", File(out, "icon-B-interlock.png"))
    ImageIO.write(iconC(size), "png", File(out, "icon-C-click.png"))
    ImageIO.write(iconD(size), "png", File(out, "icon-D-board.png"))
    ImageIO.write(featureB(rootDir), "png", File(out, "feature-B-calm.png"))
    ImageIO.write(featureD(rootDir), "png", File(out, "feature-D-play.png"))
    ImageIO.write(sheetA2(size), "png", File(out, "sheet-A-vs-A2.png"))
}

/** Side by side A against A2 at tile size and at true 48 px legibility. */
fun sheetA2(size: Int): BufferedImage {
    val sheet = BufferedImage(size * 2 + 48, size + 200, BufferedImage.TYPE_INT_ARGB)
    val g = begin(sheet)
    g.color = Color(Candidates.PAPER)
    g.fill(Rectangle2D.Double(0.0, 0.0, sheet.width.toDouble(), sheet.height.toDouble()))
    g.dispose()
    val left = iconTile(size, candidatePieceA(), size * 0.62)
    val right = iconTile(size, candidatePieceA2(), size * 0.62)
    val g2 = begin(sheet)
    g2.drawImage(left, 16, 16, null)
    g2.drawImage(right, size + 32, 16, null)
    val tiny = 48
    val tl = iconTile(tiny, candidatePieceA(), tiny * 0.66)
    val tr = iconTile(tiny, candidatePieceA2(), tiny * 0.66)
    g2.drawImage(tl, 16, size + 48, tiny * 3, tiny * 3, null)
    g2.drawImage(tr, size + 32, size + 48, tiny * 3, tiny * 3, null)
    g2.dispose()
    return sheet
}

fun main(args: Array<String>) {
    writeCandidates(File(args[0]))
    println("makeCandidates: wrote 8 takes under play-store/candidates/icon-v2")
}
