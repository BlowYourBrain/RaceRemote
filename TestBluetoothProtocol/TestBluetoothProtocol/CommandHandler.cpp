//
// Created by fetel on 06.10.2021.
//

#include "CommandHandler.h"

unsigned int END_COMMAND = 63;
unsigned int BEGIN_COMMAND = 192;
unsigned int START_BYTES_ZERO_ONE = 64;
unsigned int START_BYTES_ONE_ZERO = 128;

unsigned int command = 0;
unsigned int result = 0;

void CommandHandler::addBytesInWord(unsigned int readByte) {
    command = command | readByte;
}

unsigned int removeFirstTwoBytes(unsigned int readByte) {
    unsigned int result = 0;

    if (readByte >= BEGIN_COMMAND) {
        result = readByte ^ BEGIN_COMMAND;
    }
    else if (readByte <= END_COMMAND) {
        //do nothing
    }
    else if (readByte < START_BYTES_ONE_ZERO) {
        //01
        result = readByte ^ START_BYTES_ZERO_ONE;
    }
    else {
        //10
        result = readByte ^ START_BYTES_ONE_ZERO;
    }

    return result;
}

void CommandHandler::shiftAndAddBytesInWord(unsigned int readByte) {
    command = command << 6;
    CommandHandler::addBytesInWord(readByte);
}

/*
    Первые два бита указывают на составное слово.

    11 ** ** ** ** - начало слова
    10 ** ** ** ** или 01 ** ** ** ** - середина слова
    00 ** ** ** ** - конец слова

    Конечное слово должно выглядеть вот так:
    11 ** ** ** ... 01 ** ** ** ... 00 ** ** **

    Т.к. возвращаемое значение должно быть типа int, то самое длинное слово может состоять только из 4 слов:
    11 ** ** **      01 ** ** **     01 ** ** **     00 ** ** **

    Минимальное составное слово состоит из двух слов:
    11 ** ** **      00 ** ** **

    Третий и четвертый бит в начале составного слова означают тип команды и отмечены # на схеме ниже:
    11 ## ** **

    Попытаться получить команду. В случае если команда не сформировалась, то возвращается -1
*/
unsigned int CommandHandler::tryGetCommand(unsigned int readByte) {


    if (readByte >= BEGIN_COMMAND) {
        //первое слово в составном
        command = 0;
        addBytesInWord(removeFirstTwoBytes(readByte));
    }
    else if (readByte <= END_COMMAND) {
        //последнее слово в составном

        shiftAndAddBytesInWord(readByte);
        result = command;
        command = 0;

        return result;
    }
    else {
        //слово находится в середине составного
        shiftAndAddBytesInWord(removeFirstTwoBytes(readByte));
    }

    return CommandHandler::NO_COMMAND_YET;
};