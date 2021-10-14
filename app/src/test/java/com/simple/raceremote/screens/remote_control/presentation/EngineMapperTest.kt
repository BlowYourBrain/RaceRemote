package com.simple.raceremote.screens.remote_control.presentation

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
    fun `value is 0`(){
        val given = 0f
        assertEquals(255, mapper.mapToEngineValue(given))
    }

}