package com.simple.raceremote.utils

import kotlin.math.roundToInt

/**
 * @return значение от 0 до 180
 * */
fun Float.toServoDegrees(): Int = (this + 1).times(90).roundToInt()

//TODO сделать, когда будет ясно в какие единицы конвертировать
fun Float.toSpeed() = Unit