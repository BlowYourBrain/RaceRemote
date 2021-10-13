package com.simple.raceremote.screens.remote_control

private const val START_COMMAND = 0b1100_0000_0000_0000
private const val END_COMMAND = 0b0011_1111
private const val STEERING_WHEEL_COMMAND = 0b0010_0000_0000_0000
private const val ENGINE_COMMAND = 0b0011_0000_0000_0000

interface ICompoundCommandCreator {
    fun createSteeringWheelCommand(value: Int): Int
    fun createEngineCommand(value: Int): Int
}

/**
 * Класс, создающий команду для удаленного устройства.
 * */
class CompoundCommandCreator : ICompoundCommandCreator {

    internal fun createStartCommand(commandValue: Int) = (((commandValue shr 6) shl 8) or START_COMMAND)

    internal fun createEndCommand(commandValue: Int) = (commandValue and END_COMMAND)

    /**
     * В настоящий момент соблюдается строгий контракт, что одна составная команда состоит из двух команд.
     *
     * @param value - значение, которое будет внесено в команду.
     */
    internal fun createCommand(value: Int) = createStartCommand(value) or createEndCommand(value)

    override fun createSteeringWheelCommand(value: Int): Int =
        createCommand(value) or STEERING_WHEEL_COMMAND

    override fun createEngineCommand(value: Int): Int = createCommand(value) or ENGINE_COMMAND
}