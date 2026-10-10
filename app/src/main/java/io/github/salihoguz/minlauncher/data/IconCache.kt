package io.github.salihoguz.minlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * App icons as small bitmaps, all in the same rounded-square shape. The drawer uses grey
 * versions; Library uses full colour.
 */
object IconCache {

    // Sized in bytes: about 16 MB, enough for every icon of a full phone in both styles.
    private val cache = object : LruCache<String, ImageBitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    /** Corner radius as a share of the icon's size. */
    private const val CORNER = 0.3f

    /** Old-style icons sit on a white tile at this share of its size, like Android does it. */
    private const val LEGACY_SCALE = 0.78f

    private val grey = Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }
    private val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }

    fun peek(key: String, color: Boolean = false): ImageBitmap? = cache.get(cacheKey(key, color))

    private fun cacheKey(key: String, color: Boolean) = if (color) "$key#color" else key

    /** Drops cached icons of a package that was installed, updated or removed. */
    fun forget(packageName: String) {
        cache.snapshot().keys.filter { it.startsWith("$packageName/") }.forEach { cache.remove(it) }
    }

    /** Loads (or reuses) the icon. Call off the main thread. */
    fun load(context: Context, app: InstalledApp, sizePx: Int, color: Boolean = false): ImageBitmap? {
        val cacheKey = cacheKey(app.key, color)
        cache.get(cacheKey)?.let { return it }
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
        val icon = runCatching { launcherApps.resolveActivity(intent, app.user)?.getIcon(0)?.mutate() }.getOrNull()
            ?: return null
        val bitmap = runCatching { draw(icon, sizePx, color) }.getOrNull() ?: return null
        return bitmap.asImageBitmap().also { cache.put(cacheKey, it) }
    }

    private fun draw(icon: Drawable, sizePx: Int, color: Boolean): Bitmap {
        val size = sizePx.toFloat()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Everything drawn inside this layer turns grey when it's put down (unless in colour).
        val layer = canvas.saveLayer(0f, 0f, size, size, if (color) null else grey)
        if (icon is AdaptiveIconDrawable) {
            // Layers are 108 dp with the visible icon in the middle 72 dp: draw them bigger and
            // let the mask below trim them.
            val bleed = sizePx / 4
            listOfNotNull(icon.background, icon.foreground).forEach { layerDrawable ->
                layerDrawable.setBounds(-bleed, -bleed, sizePx + bleed, sizePx + bleed)
                layerDrawable.draw(canvas)
            }
        } else if (fillsItsSquare(icon, sizePx)) {
            icon.setBounds(0, 0, sizePx, sizePx)
            icon.draw(canvas)
        } else {
            // Round or odd-shaped old icons: centred on a white tile.
            canvas.drawColor(Color.WHITE)
            val inset = ((1f - LEGACY_SCALE) / 2f * size).toInt()
            icon.setBounds(inset, inset, sizePx - inset, sizePx - inset)
            icon.draw(canvas)
        }
        // Trim to the shared shape, with smooth edges.
        canvas.drawRoundRect(0f, 0f, size, size, size * CORNER, size * CORNER, mask)
        canvas.restoreToCount(layer)
        return bitmap
    }

    /** True for old-style icons that are a full square: their corners are solid. */
    private fun fillsItsSquare(icon: Drawable, sizePx: Int): Boolean {
        val probe = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, sizePx, sizePx)
        icon.draw(Canvas(probe))
        val near = (sizePx * 0.1f).toInt()
        val far = sizePx - 1 - near
        val corners = listOf(near to near, far to near, near to far, far to far)
        val solid = corners.all { (x, y) -> Color.alpha(probe.getPixel(x, y)) > 200 }
        probe.recycle()
        return solid
    }
}
