package com.simple.raceremote.features.remote_control.presentation.mapper

interface IEngineMapper {
    /**
     * @value - значение от -1f, до 1f.
     * В случае если значение меньше, чем -1f, то значение будет считаться как -1f.
     * В случае если значение больше, чем 1f, то значение будет считаться как 1f.
     *
     * @return значение от 0 до 511.
     * */
    fun mapToEngineValue(value: Float): Int
}

/**
 * Преобразует значения presentation слоя с в значения для двигателя удаленного устройства.
 * */
class EngineMapper : IEngineMapper {

    private var y = 0

    /**
     * @value - значение от -1f, до 1f.
     * В случае если значение меньше, чем -1f, то значение будет считаться как -1f.
     * В случае если значение больше, чем 1f, то значение будет считаться как 1f.
     *
     * @return значение от 0 до 511.
     * */
    override fun mapToEngineValue(value: Float): Int {
        //y = kx + m
        //k = m = 511 / 2
        y = (511 * (value + 1) / 2).toInt()

        return minOf(maxOf(0, y), 511)
    }

}