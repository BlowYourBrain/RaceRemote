package com.simple.raceremote.screens.remote_control.presentation.mapper

interface ISteeringWheelMapper {
    fun mapToSteeringWheel(value: Float): Int
}

class SteeringWheelMapper : ISteeringWheelMapper {

    private var y= 0

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
        //x - значение value, y - вычисляемый параметр
        // k = -90, m = 90
        // y = -90 * x + 90
        y = (-(90 * value) + 90).toInt()

        return minOf(maxOf(0, y), 180)
    }

}