package com.simple.raceremote.screens.remote_control.presentation

interface ISteeringWheelMapper {
    fun map(value: Float): Int
}

class SteeringWheelMapper : ISteeringWheelMapper {

    private var x = 0

    override fun map(value: Float): Int{
        // k = 1/90, m = -1
        // y = x / 90 - 1
        // x = (y + 1) * 90
        x = ((value + 1) * 90).toInt()

        return minOf(maxOf(0, x), 180)
    }

}