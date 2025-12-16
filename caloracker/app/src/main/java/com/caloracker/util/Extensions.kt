package com.caloracker.util

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Kotlin extension functions for common operations.
 */

/**
 * Show a toast message.
 *
 * @param message The message to display
 * @param duration Toast duration (default: Toast.LENGTH_SHORT)
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Show a toast message from a Fragment.
 *
 * @param message The message to display
 * @param duration Toast duration (default: Toast.LENGTH_SHORT)
 */
fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(message, duration)
}

/**
 * Make a view visible.
 */
fun View.visible() {
    visibility = View.VISIBLE
}

/**
 * Make a view invisible (still takes up space).
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Make a view gone (doesn't take up space).
 */
fun View.gone() {
    visibility = View.GONE
}

/**
 * Toggle view visibility.
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * Set view visibility based on condition.
 *
 * @param visible If true, view is visible; otherwise gone
 */
fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * Format double to specified decimal places.
 *
 * @param decimals Number of decimal places
 * @return Formatted string
 */
fun Double.format(decimals: Int = 1): String {
    return "%.${decimals}f".format(this)
}

/**
 * Round double to integer.
 *
 * @return Rounded integer value
 */
fun Double.roundToInt(): Int {
    return kotlin.math.round(this).toInt()
}
