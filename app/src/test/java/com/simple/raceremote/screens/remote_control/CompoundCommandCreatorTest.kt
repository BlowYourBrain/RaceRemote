package com.simple.raceremote.screens.remote_control

import com.simple.raceremote.screens.remote_control.utils.CompoundCommandCreator
import org.junit.Assert.*
import org.junit.Test

class CompoundCommandCreatorTest{

    private val compoundCommandCreator = CompoundCommandCreator()
    private val value = 0b1011_0100

    @Test
    fun `end command test`(){
        val command = compoundCommandCreator.createEndCommand(value)
        assertEquals(0b0011_0100, command)
    }

    @Test
    fun `start command test`(){
        val command = compoundCommandCreator.createStartCommand(value)
        assertEquals(0b1100_0010_0000_0000, command)
    }

    @Test
    fun `create command test`(){
        val command = compoundCommandCreator.createCommand(value)
        assertEquals(0b1100_0010_0011_0100, command)
    }

    @Test
    fun `create steering wheel test`(){
        val command = compoundCommandCreator.createSteeringWheelCommand(value)
        assertEquals(0b1110_0010_0011_0100, command)
    }

    @Test
    fun `create engine test`(){
        val command = compoundCommandCreator.createEngineCommand(value)
        assertEquals(0b1111_0010_0011_0100, command)
    }

}