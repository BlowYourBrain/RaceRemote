package com.simple.raceremote.utils

import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt

fun Spannable.setColor(@ColorInt color: Int): Spannable = apply {
    setSpan(
        ForegroundColorSpan(color),
        0,
        length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}