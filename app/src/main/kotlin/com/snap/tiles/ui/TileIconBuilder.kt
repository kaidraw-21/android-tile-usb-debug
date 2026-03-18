package com.snap.tiles.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat

fun buildTileIconBitmap(context: Context, @DrawableRes iconRes: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (108 * density).toInt()
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val squareSize = sizePx * 0.56f
    val cornerRadius = squareSize * 0.20f
    val shift = sizePx * 0.09f

    // Yellow square — upper-left (mirrors app logo)
    paint.color = android.graphics.Color.parseColor("#FBD928")
    canvas.drawRoundRect(
        RectF(cx - squareSize / 2f - shift, cy - squareSize / 2f - shift,
            cx + squareSize / 2f - shift, cy + squareSize / 2f - shift),
        cornerRadius, cornerRadius, paint
    )

    // Gray square — lower-right
    paint.color = android.graphics.Color.parseColor("#ABABAB")
    canvas.drawRoundRect(
        RectF(cx - squareSize / 2f + shift, cy - squareSize / 2f + shift,
            cx + squareSize / 2f + shift, cy + squareSize / 2f + shift),
        cornerRadius, cornerRadius, paint
    )

    // Tile icon — white, centered
    val iconPx = (sizePx * 0.36f).toInt()
    val offset = (sizePx - iconPx) / 2
    val drawable = ContextCompat.getDrawable(context, iconRes)
        ?.constantState?.newDrawable(context.resources)?.mutate()
    drawable?.setBounds(offset, offset, offset + iconPx, offset + iconPx)
    drawable?.colorFilter = android.graphics.PorterDuffColorFilter(
        android.graphics.Color.WHITE,
        android.graphics.PorterDuff.Mode.SRC_IN
    )
    drawable?.draw(canvas)

    return bitmap
}

fun buildShortcutIcon(context: Context, @DrawableRes iconRes: Int): IconCompat =
    IconCompat.createWithAdaptiveBitmap(buildTileIconBitmap(context, iconRes))
