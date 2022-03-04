package com.simple.raceremote.screens.remote_control.presentation

import com.simple.raceremote.screens.remote_control.presentation.mapper.ISteeringWheelMapper
import com.simple.raceremote.screens.remote_control.presentation.mapper.SteeringWheelMapper
import org.junit.Assert
import org.junit.Test


class SteeringWheelMapperTest {

    private val mapper: ISteeringWheelMapper = SteeringWheelMapper()
    private val MIN_VALUE: Int = 0
    private val MAX_VALUE = 180
    private val MIDDLE_VALUE = 90

    @Test
    fun `value lower than -1f`() {
        //given
        val given = -2f
        //when
        val mappedValue = mapper.mapToSteeringWheel(given)
        //then
        Assert.assertEquals(MAX_VALUE, mappedValue)
    }

    @Test
    fun `value higher than 1f`() {
        //given
        val given = 2f
        //when
        val mappedValue = mapper.mapToSteeringWheel(given)
        //then
        Assert.assertEquals(MIN_VALUE, mappedValue)
    }

    @Test
    fun `value is 0f`() {
        //given
        val given = 0f
        //when
        val mappedValue = mapper.mapToSteeringWheel(given)
        //then
        Assert.assertEquals(MIDDLE_VALUE, mappedValue)
    }

    @Test
    fun `value is -1f`() {
        //given
        val given = -1f
        //when
        val mappedValue = mapper.mapToSteeringWheel(given)
        //then
        Assert.assertEquals(MAX_VALUE, mappedValue)
    }

    @Test
    fun `value lower than 1f`() {
        //given
        val given = 1f
        //when
        val mappedValue = mapper.mapToSteeringWheel(given)
        //then
        Assert.assertEquals(MIN_VALUE, mappedValue)
    }
}