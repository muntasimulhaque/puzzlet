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
import kotlin.math.exp

/**
 * From-scratch icon directions, drawn from the product truth, not from the
 * rejected takes. Same palette, no text, nothing alive. Folder:
 * play-store/candidates/icon-v3. Shipped icons stay pinned until one wins.
 */
object V3 {
    const val TEAL = 0xFF0C7A64.toInt()
    const val DEEP = 0xFF085949.toInt()
    const val PAPER = 0xFFFAF6EF.toInt()
}

private fun v3begin(image: BufferedImage): Graphics2D {
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    return g
}

private fun v3blur(src: BufferedImage, radius: Int): BufferedImage {
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

private fun v3tile(size: Int): BufferedImage {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = v3begin(image)
    val r = size * 0.19
    g.paint = GradientPaint(0f, 0f, Color(V3.TEAL), 0f, size.toFloat(), Color(V3.DEEP))
    g.fill(RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), r * 2, r * 2))
    g.dispose()
    return image
}

private fun v3clip(size: Int): RoundRectangle2D.Double {
    val r = size * 0.19
    return RoundRectangle2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), r * 2, r * 2)
}

private fun v3paint(g: Graphics2D, shape: Area, cx: Double, cy: Double, span: Double, argb: Int) {
    val bounds = shape.bounds2D
    val s = span / maxOf(bounds.width, bounds.height)
    val t = AffineTransform.getTranslateInstance(cx - bounds.centerX * s, cy - bounds.centerY * s)
    t.concatenate(AffineTransform.getScaleInstance(s, s))
    val g2 = g.create() as Graphics2D
    g2.color = Color(argb, true)
    g2.fill(shape.createTransformedArea(t))
    g2.dispose()
}

/** Soft shadow of a shape at a position, drawn under later paint. */
private fun v3shadow(g: Graphics2D, shape: Area, cx: Double, cy: Double, span: Double, size: Int, alpha: Float) {
    val layer = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val gs = v3begin(layer)
    v3paint(gs, shape, cx, cy, span, V3.DEEP)
    gs.dispose()
    val soft = v3blur(layer, maxOf(3, size / 56))
    val keep = g.composite
    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
    g.drawImage(soft, 0, 0, null)
    g.composite = keep
}

/** Top light clipped to a shape, for one shared sun. */
private fun v3light(g: Graphics2D, shape: Area, cx: Double, cy: Double, span: Double, size: Int) {
    val bounds = shape.bounds2D
    val s = span / maxOf(bounds.width, bounds.height)
    val t = AffineTransform.getTranslateInstance(cx - bounds.centerX * s, cy - bounds.centerY * s)
    t.concatenate(AffineTransform.getScaleInstance(s, s))
    val keep = g.clip
    g.clip = shape.createTransformedArea(t)
    g.paint = GradientPaint(0f, size * 0.15f, Color(255, 255, 255, 30), 0f, size * 0.55f, Color(255, 255, 255, 0))
    g.fillRect(0, 0, size, size)
    g.clip = keep
}

private const val VBODY = 0.42
private const val VBX = 0.29
private const val VCR = 0.095
private const val VNECK = 0.092
private const val VNLEN = 0.078
private const val VHEAD = 0.080

private fun v3body(): Area {
    return Area(RoundRectangle2D.Double(VBX, VBX, VBODY, VBODY, VCR, VCR))
}

private fun Area.v3outUp(cx: Double, edgeY: Double) {
    val n = VNECK / 2.0
    add(Area(Rectangle2D.Double(cx - n, edgeY - VNLEN, VNECK, VNLEN + 0.01)))
    add(Area(Ellipse2D.Double(cx - VHEAD, edgeY - VNLEN - VHEAD * 1.35, VHEAD * 2, VHEAD * 2)))
}

private fun Area.v3outDown(cx: Double, edgeY: Double) {
    val n = VNECK / 2.0
    add(Area(Rectangle2D.Double(cx - n, edgeY - 0.01, VNECK, VNLEN + 0.01)))
    add(Area(Ellipse2D.Double(cx - VHEAD, edgeY + VNLEN - VHEAD * 0.65, VHEAD * 2, VHEAD * 2)))
}

private fun Area.v3inRight(edgeX: Double, cy: Double) {
    val n = VNECK / 2.0
    subtract(Area(Rectangle2D.Double(edgeX - VNLEN, cy - n, VNLEN + 0.02, VNECK)))
    subtract(Area(Ellipse2D.Double(edgeX - VNLEN - VHEAD * 0.9, cy - VHEAD, VHEAD * 2, VHEAD * 2)))
}

private fun Area.v3inLeft(edgeX: Double, cy: Double) {
    val n = VNECK / 2.0
    subtract(Area(Rectangle2D.Double(edgeX - 0.02, cy - n, VNLEN + 0.02, VNECK)))
    subtract(Area(Ellipse2D.Double(edgeX + VNLEN - VHEAD * 1.1, cy - VHEAD, VHEAD * 2, VHEAD * 2)))
}

/** One honest piece: outies up and down, innies left and right. */
fun v3piece(): Area {
    val a = v3body()
    val e = VBX + VBODY
    a.v3outUp(0.5, VBX)
    a.v3outDown(0.5, e)
    a.v3inRight(e, 0.5)
    a.v3inLeft(VBX, 0.5)
    return a
}

/**
 * Take H, the hover: the piece floats above its own slot, larger because
 * nearer, its shadow falling into the slot. The snap, about to happen.
 */
fun v3hover(size: Int): BufferedImage {
    val tile = v3tile(size)
    val piece = v3piece()
    val slotC = doubleArrayOf(size / 2.0, size * 0.66)
    val slotSpan = size * 0.44
    val pieceC = doubleArrayOf(size / 2.0, size * 0.35)
    val pieceSpan = size * 0.56
    val g = v3begin(tile)
    v3paint(g, piece, slotC[0], slotC[1], slotSpan, V3.DEEP)
    run {
        val bounds = piece.bounds2D
        val s = slotSpan / maxOf(bounds.width, bounds.height)
        val t = AffineTransform.getTranslateInstance(slotC[0] - bounds.centerX * s, slotC[1] - bounds.centerY * s)
        t.concatenate(AffineTransform.getScaleInstance(s, s))
        val keepClip = g.clip
        g.clip = piece.createTransformedArea(t)
        g.paint = GradientPaint(0f, size * 0.42f, Color(0x08, 0x59, 0x49, 130), 0f, size * 0.62f, Color(0x08, 0x59, 0x49, 0))
        g.fillRect(0, 0, size, size)
        g.clip = keepClip
    }
    v3shadow(g, piece, slotC[0], slotC[1] + size * 0.012, pieceSpan, size, 0.50f)
    v3paint(g, piece, pieceC[0], pieceC[1], pieceSpan, V3.PAPER)
    v3light(g, piece, pieceC[0], pieceC[1], pieceSpan, size)
    g.dispose()
    return tile
}

/**
 * Take S, the seam: two pieces cropped by the frame, paper against deep,
 * divided only by their interlock curve. Macro, two tone, no small parts.
 */
fun v3seam(size: Int): BufferedImage {
    val tile = v3tile(size)
    val g = v3begin(tile)
    g.clip = v3clip(size)
    val edgeX = size * 0.56
    val cy = size * 0.52
    val neck = size * 0.088
    val headR = size * 0.135
    val len = size * 0.11
    val left = Area(Rectangle2D.Double(-size * 0.1, -size * 0.1, edgeX + size * 0.1, size * 1.2))
    left.add(Area(Rectangle2D.Double(edgeX - 0.5, cy - neck / 2.0, len + 0.5, neck)))
    left.add(Area(Ellipse2D.Double(edgeX + len - headR * 0.55, cy - headR, headR * 2, headR * 2)))
    val shade = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val gs = v3begin(shade)
    gs.color = Color(V3.DEEP, true)
    gs.fill(left)
    gs.dispose()
    val soft = v3blur(shade, maxOf(3, size / 48))
    val keep = g.composite
    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f)
    g.drawImage(soft, size / 64, size / 90, null)
    g.composite = keep
    g.color = Color(V3.PAPER, true)
    g.fill(left)
    g.stroke = java.awt.BasicStroke(size * 0.009f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
    g.color = Color(V3.DEEP, true)
    g.draw(left)
    g.dispose()
    return tile
}

/**
 * Take W, the well: the board itself, one empty slot, one piece just
 * lifted with its shadow on the board. The whole game in one tile.
 */
fun v3well(size: Int): BufferedImage {
    val tile = v3tile(size)
    val piece = v3piece()
    val m = size * 0.17
    val side = size - 2 * m
    val board = Area(RoundRectangle2D.Double(m, m, side.toDouble(), side.toDouble(), size * 0.07, size * 0.07))
    val g = v3begin(tile)
    val bs = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val gb = v3begin(bs)
    gb.color = Color(V3.DEEP, true)
    gb.fill(board)
    gb.dispose()
    val keep = g.composite
    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f)
    g.drawImage(v3blur(bs, maxOf(3, size / 56)), 0, size / 70, null)
    g.composite = keep
    g.color = Color(V3.PAPER, true)
    g.fill(board)
    val bcx = size / 2.0
    val bcy = size / 2.0
    v3paint(g, piece, bcx - side * 0.13, bcy + side * 0.14, side * 0.40, V3.DEEP)
    val lx = bcx + side * 0.16
    val ly = bcy - side * 0.17
    v3shadow(g, piece, lx, ly + side * 0.02, side * 0.40, size, 0.42f)
    v3paint(g, piece, lx, ly, side * 0.40, V3.PAPER)
    v3light(g, piece, lx, ly, side * 0.40, size)
    g.dispose()
    return tile
}

private fun v3label(g: Graphics2D, text: String, x: Float, y: Float) {
    g.font = Font("SansSerif", Font.BOLD, 34)
    g.color = Color(0xFF1F2B28.toInt())
    g.drawString(text, x, y)
}

fun v3sheet(): BufferedImage {
    val t = 384
    val small = 48
    val show = small * 3
    val w = t * 3 + 64
    val h = t + show + 140
    val sheet = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = v3begin(sheet)
    g.color = Color(V3.PAPER)
    g.fill(Rectangle2D.Double(0.0, 0.0, w.toDouble(), h.toDouble()))
    val takes = listOf(v3hover(512) to "H hover", v3seam(512) to "S seam", v3well(512) to "W well")
    for ((i, take) in takes.withIndex()) {
        val x = 16 + i * (t + 16)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(take.first, x, 16, t, t, null)
        v3label(g, take.second, x.toFloat() + 8, (t + 56).toFloat())
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g.drawImage(take.first, x + 8, t + 72, x + 8 + show, t + 72 + show, 0, 0, 512, 512, null)
    }
    g.dispose()
    return sheet
}

fun writeV3(rootDir: File) {
    val out = File(rootDir, "play-store/candidates/icon-v3")
    out.mkdirs()
    ImageIO.write(v3hover(512), "png", File(out, "icon-H-hover.png"))
    ImageIO.write(v3seam(512), "png", File(out, "icon-S-seam.png"))
    ImageIO.write(v3well(512), "png", File(out, "icon-W-well.png"))
    ImageIO.write(v3sheet(), "png", File(out, "sheet-all.png"))
}

fun main(args: Array<String>) {
    writeV3(File(args[0]))
    println("makeCandidatesV3: wrote 4 takes under play-store/candidates/icon-v3")
}
