package com.simple.raceremote.screens.remote_control.presentation

interface ISteeringWheelMapper {
    fun mapToSteeringWheel(value: Float): Int
}

class SteeringWheelMapper : ISteeringWheelMapper {

    private var x = 0

    /**
     * Значение для поворота колёс (руля).
     * @param value - значение от -1f до 1f.
     *
     * В случае если значение меньше, чем -1f будет возвращено 0.
     * В случае если значение больше, чем 1f будет возвращено 180.
     *
     * @return значение от 0 до 180
     * */
    override fun mapToSteeringWheel(value: Float): Int {
        //y - значение value, x - вычисляемый параметр
        // k = 1/90, m = -1
        // y = x / 90 - 1
        // x = (y + 1) * 90
        x = ((value + 1) * 90).toInt()

        return minOf(maxOf(0, x), 180)
    }

}