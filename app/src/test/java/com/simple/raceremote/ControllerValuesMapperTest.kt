package com.simple.raceremote

import com.simple.raceremote.utils.toServoDegrees
import org.junit.Assert
import org.junit.Test

class ControllerValuesMapperTest {

    @Test
    fun `compare servo boundary values`() {
        Assert.assertEquals((-1f).toServoDegrees(), 0)
        Assert.assertEquals(0f.toServoDegrees(), 90)
        Assert.assertEquals(1f.toServoDegrees(), 180)
    }

    @Test
    fun `check round up`() {
        Assert.assertEquals(0.35f.toServoDegrees(), 122)
    }

    @Test
    fun `check round down`() {
        Assert.assertEquals(0.34f.toServoDegrees(), 121)
        Assert.assertEquals(0.345f.toServoDegrees(), 121)
    }
}