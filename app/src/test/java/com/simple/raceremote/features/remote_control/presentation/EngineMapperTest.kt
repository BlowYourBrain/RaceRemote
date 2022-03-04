package com.simple.raceremote.features.remote_control.presentation

import com.simple.raceremote.features.remote_control.presentation.mapper.EngineMapper
import com.simple.raceremote.features.remote_control.presentation.mapper.IEngineMapper
import org.junit.Assert.*
import org.junit.Test

class EngineMapperTest {

    private val MIN_VALUE = 0
    private val MAX_VALUE = 511
    private val mapper: IEngineMapper = EngineMapper()

    @Test
    fun `value below -1f`() {
        val given = -2f
        assertEquals(MIN_VALUE, mapper.mapToEngineValue(given))
    }

    @Test
    fun `value higher 1f`() {
        val given = 2f
        assertEquals(MAX_VALUE, mapper.mapToEngineValue(given))
    }

    @Test
    fun `value is -1f`() {
        val given = -1f
        assertEquals(MIN_VALUE, mapper.mapToEngineValue(given))
    }

    @Test
    fun `value is 1f`() {
        val given = 1f
        assertEquals(MAX_VALUE, mapper.mapToEngineValue(given))
    }

    @Test
    fun `value is 0f`(){
        val given = 0f
        assertEquals(255, mapper.mapToEngineValue(given))
    }

    @Test
    fun `value is 0,5f`(){
        val given = 0.5f
        assertEquals(383, mapper.mapToEngineValue(given))
    }

}