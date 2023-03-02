package com.simple.raceremote.features.remote_control.utils

interface ICompoundCommandExtractor {

    fun extractStartCommand(command: Int): Byte

    fun extractEndCommand(command: Int): Byte

}

class CompoundCommandExtractor : ICompoundCommandExtractor {
    companion object {
        private const val clearFirst8Bytes = 0b0000_0000_1111_1111
    }

    override fun extractStartCommand(command: Int): Byte = (command shr 8).toByte()

    override fun extractEndCommand(command: Int): Byte = (command and clearFirst8Bytes).toByte()
}