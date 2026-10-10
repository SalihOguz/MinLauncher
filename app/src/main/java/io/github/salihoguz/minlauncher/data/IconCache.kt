package io.github.salihoguz.minlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * App icons as small bitmaps. The drawer uses one-colour icons: apps that ship a themed
 * (monochrome) icon layer, Android 13+, get it drawn in white; the rest get a grey version of
 * their normal icon. Library uses the full-colour icon.
 */
object IconCache {

    // Sized in bytes: about 16 MB, enough for every icon of a full phone in both styles.
    private val cache = object : LruCache<String, ImageBitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

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

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val monochrome = if (!color && Build.VERSION.SDK_INT >= 33 && icon is AdaptiveIconDrawable) icon.monochrome?.mutate() else null
        if (color) {
            icon.setBounds(0, 0, sizePx, sizePx)
            icon.draw(canvas)
        } else if (monochrome != null) {
            // Adaptive icon layers are 108 dp; the visible icon is the middle 72 dp.
            val inset = sizePx / 4
            monochrome.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
            monochrome.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            monochrome.draw(canvas)
        } else {
            icon.setBounds(0, 0, sizePx, sizePx)
            icon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            icon.draw(canvas)
        }
        return bitmap.asImageBitmap().also { cache.put(cacheKey, it) }
    }
}
