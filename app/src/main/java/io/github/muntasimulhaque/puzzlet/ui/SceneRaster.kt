package io.github.muntasimulhaque.puzzlet.ui

import android.util.LruCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.muntasimulhaque.puzzlet.core.SceneSpec

/**
 * One raster per picture and pixel side, shared by every surface that shows
 * a scene picture: the shelf cards, the picture held up over the board, the
 * celebration plate, the cut chooser's tiles and the picture coin.
 *
 * A scene is a few hundred flat shapes. Re-recording and re-rendering them
 * on every scroll frame is what made the shelf stutter: measured on the
 * emulator before this store existed, the picture cards were the scroll
 * cost (a card drawn as a flat block instead of a scene cut the slow draw
 * commands and the jank down while everything else stood still). A picture
 * is rastered once at its real draw size now, and every later frame draws
 * one image.
 *
 * Nothing here can go stale: scene data is immutable and the key carries
 * the exact side, so a size change is a miss that rebuilds rather than a
 * stretch of an old picture. A bitmap is never recycled (the render thread
 * may still be reading one), so eviction only drops the reference and the
 * collector takes it.
 *
 * The budget is a slice of the app's own heap, the way an image loader
 * sizes its cache, so it can never crowd out the app itself. The point of
 * the number is that a whole shelf of sixteen cards fits inside it even on
 * a ten inch tablet (about 42 MB there), so a scroll pass never rebuilds
 * what it just passed. The play field is deliberately not here: its pieces
 * draw vectors, and a carry already redraws nothing at all.
 */
private val MAX_RASTER_BYTES: Int = run {
    val heap = Runtime.getRuntime().maxMemory()
    (heap / 6).coerceIn(16L * 1024 * 1024, 64L * 1024 * 1024).toInt()
}

private data class SceneRasterKey(val sceneId: String, val sidePx: Int)

private val sceneRasters = object : LruCache<SceneRasterKey, ImageBitmap>(MAX_RASTER_BYTES) {
    override fun sizeOf(key: SceneRasterKey, value: ImageBitmap): Int = key.sidePx * key.sidePx * 4
}

/**
 * The raster of [spec] at [sidePx], built and cached on first use. Called
 * from the draw phase, on the UI thread only: [LruCache] keeps its own
 * bookkeeping safe, and a raster is immutable once built.
 */
internal fun sceneRaster(spec: SceneSpec, sidePx: Int): ImageBitmap {
    val side = sidePx.coerceAtLeast(1)
    val key = SceneRasterKey(spec.id, side)
    sceneRasters.get(key)?.let { return it }
    val image = ImageBitmap(side, side)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(image),
        size = Size(side.toFloat(), side.toFloat()),
    ) {
        drawScene(spec, side.toDouble())
    }
    sceneRasters.put(key, image)
    return image
}
