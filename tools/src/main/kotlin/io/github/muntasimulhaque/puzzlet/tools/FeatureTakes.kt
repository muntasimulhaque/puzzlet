package io.github.muntasimulhaque.puzzlet.tools

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * Candidate banners for the feature-graphic redo (owner: the Play app
 * cropped the 1024 x 500 to a card and sliced the tail off the name). One
 * folder of takes, judged as images, then swept once the owner points at
 * one (the D-036 lesson; D-053 swept the last takes generator the same
 * way).
 *
 * Output goes to build/feature-takes (never committed): each take as the
 * full 1024 x 500, its 4:3 card crop and its 3:1 banner crop, and one
 * labeled sheet with the safe box drawn on the full view. Every take
 * passes the box law in StoreArt.kt, checked here, and the current shipped
 * banner rides along as the reference.
 */

fun main(args: Array<String>) {
    val rootDir = File(args.firstOrNull() ?: ".").absoluteFile
    val outDir = File(rootDir, "build/feature-takes")
    check(outDir.isDirectory || outDir.mkdirs()) { "Could not create $outDir" }
    val takes = listOf<Pair<String, (File) -> BufferedImage>>(
        "take-A-row" to { MakeArt.featureGraphic(it) },
        "take-B-piece-above" to ::pieceAboveTake,
        "take-C-name-first" to ::nameFirstTake,
        "take-D-name-only" to ::nameOnlyTake,
    )
    for ((id, take) in takes) {
        val image = take(rootDir)
        checkInsideBox(image, id)
        ImageIO.write(image, "png", File(outDir, "$id-full.png"))
        ImageIO.write(crop43(image), "png", File(outDir, "$id-crop43.png"))
        ImageIO.write(crop31(image), "png", File(outDir, "$id-crop31.png"))
        ImageIO.write(sheet(id, image, rootDir), "png", File(outDir, "sheet-$id.png"))
        println("$id: ink ${bannerInkRect(image).joinToString("..")} on 1024 x 500")
    }
    println("Wrote ${takes.size} takes (full, the 4:3 and 3:1 crops, and one sheet each) to $outDir")
}

// ------------------------------------------------------------------ takes

/**
 * The mark above, the name under it, the line under that: the mark opens
 * the picture and the name lands under it. The piece stays small so the
 * name, the thing a parent reads, can be the biggest word in the box.
 */
private fun pieceAboveTake(rootDir: File): BufferedImage {
    val piece = markArt(170.0)
    val name = textArt(rootDir, STORE_NAME, STORE_EXTRA, 122f, STORE_PAPER)
    val line = textArt(rootDir, STORE_LINE, STORE_BOLD, 30f, STORE_SOFT)
    val gap = 20
    val layer = transparentCanvas()
    val g = graphics(layer)
    val top = (STORE_H - (piece.height + gap + name.height + gap + line.height)) / 2
    drawAt(g, piece, STORE_W / 2, top + piece.height / 2)
    drawAt(g, name, STORE_W / 2, top + piece.height + gap + name.height / 2)
    drawAt(g, line, STORE_W / 2, top + piece.height + gap + name.height + gap + line.height / 2)
    g.dispose()
    return composeBanner(layer)
}

/**
 * The name first, the line under it, the piece below: the message leads,
 * the mark anchors. The same narrow lockup as the piece-above take, read
 * the other way round.
 */
private fun nameFirstTake(rootDir: File): BufferedImage {
    val name = textArt(rootDir, STORE_NAME, STORE_EXTRA, 122f, STORE_PAPER)
    val line = textArt(rootDir, STORE_LINE, STORE_BOLD, 30f, STORE_SOFT)
    val piece = markArt(150.0)
    val gap = 20
    val layer = transparentCanvas()
    val g = graphics(layer)
    val top = (STORE_H - (name.height + gap + line.height + gap + piece.height)) / 2
    drawAt(g, name, STORE_W / 2, top + name.height / 2)
    drawAt(g, line, STORE_W / 2, top + name.height + gap + line.height / 2)
    drawAt(g, piece, STORE_W / 2, top + name.height + gap + line.height + gap + piece.height / 2)
    g.dispose()
    return composeBanner(layer)
}

/**
 * The piece left, the name right, no line: the store page already carries
 * the short description, so the name takes the whole right side and can be
 * the biggest word of any take while the mark stays the size of a piece in
 * hand.
 */
private fun nameOnlyTake(rootDir: File): BufferedImage {
    val piece = markArt(200.0)
    val name = textArt(rootDir, STORE_NAME, STORE_EXTRA, 122f, STORE_PAPER)
    val gap = 52
    val layer = transparentCanvas()
    val g = graphics(layer)
    val left = (STORE_W - (piece.width + gap + name.width)) / 2
    drawAt(g, piece, left + piece.width / 2, STORE_H / 2)
    drawAt(g, name, left + piece.width + gap + name.width / 2, STORE_H / 2)
    g.dispose()
    return composeBanner(layer)
}

// ------------------------------------------------------- sheet and crops

/**
 * One take on a review sheet: the full banner with the safe box drawn on
 * it, the 4:3 card crop, and the 3:1 banner crop, each labeled, so the
 * margins can be judged at a glance.
 */
private fun sheet(id: String, image: BufferedImage, rootDir: File): BufferedImage {
    val c43 = crop43(image)
    val c31 = crop31(image)
    val pad = 24
    val label = 34
    val sheetH = pad + label + STORE_H + pad + label + STORE_H + pad + label + c31.height + pad
    val sheet = BufferedImage(STORE_W, sheetH, BufferedImage.TYPE_INT_RGB)
    val g = graphics(sheet)
    g.color = Color(0x1F2B28, true)
    g.fill(Rectangle2D.Double(0.0, 0.0, sheet.width.toDouble(), sheet.height.toDouble()))
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    var y = pad
    heading(g, rootDir, "$id   full 1024 x 500, box drawn: the box is what always survives", pad, y + label)
    y += label
    g.drawImage(image, pad, y, null)
    val (x0, x1, y0, y1) = safeBox().toList()
    g.color = Color(STORE_PAPER, true)
    g.stroke = BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(8f, 6f), 0f)
    g.drawRect(pad + x0, y + y0, x1 - x0, y1 - y0)
    y += STORE_H + pad
    heading(g, rootDir, "$id   the 4:3 card: 17.4 percent off each side", pad, y + label)
    y += label
    g.drawImage(c43, pad, y, null)
    y += STORE_H + pad
    heading(g, rootDir, "$id   the 3:1 banner: 15.9 percent off top and bottom", pad, y + label)
    y += label
    g.drawImage(c31, pad, y, null)
    g.dispose()
    return sheet
}

private fun heading(g: Graphics2D, rootDir: File, text: String, x: Int, y: Int) {
    drawCleanString(g, text, STORE_BOLD, 30f, STORE_PAPER, x.toFloat(), y.toFloat(), rootDir)
}

/** The 4:3 card: full height, 17.4 percent off each side. */
private fun crop43(image: BufferedImage): BufferedImage {
    val w = (STORE_H * 4.0 / 3.0).roundToInt()
    return image.getSubimage((STORE_W - w) / 2, 0, w, STORE_H)
}

/** The 3:1 banner: full width, 15.9 percent off top and bottom. */
private fun crop31(image: BufferedImage): BufferedImage {
    val h = (STORE_W / 3.0).roundToInt()
    return image.getSubimage(0, (STORE_H - h) / 2, STORE_W, h)
}
