//
// Created by fetel on 06.10.2021.
//

#include "CommandHandler.h"

unsigned __int8 END_COMMAND = 63;
unsigned __int8 BEGIN_COMMAND = 192;
unsigned __int8 START_BYTES_ZERO_ONE = 64;
unsigned __int8 START_BYTES_ONE_ZERO = 128;

unsigned int command = 0;
unsigned int result = 0;

void CommandHandler::addBytesInWord(unsigned __int8 byte) {
    command = command | byte;
}

unsigned __int8 removeFirstTwoBytes(unsigned __int8 byte) {
    unsigned __int8 result = 0;

    if (byte >= BEGIN_COMMAND) {
        result = byte ^ BEGIN_COMMAND;
    }
    else if (byte <= END_COMMAND) {
        //do nothing
    }
    else if (byte < START_BYTES_ONE_ZERO) {
        //01
        result = byte ^ START_BYTES_ZERO_ONE;
    }
    else {
        //10
        result = byte ^ START_BYTES_ONE_ZERO;
    }
    
    return result;
}

void CommandHandler::shiftAndAddBytesInWord(unsigned __int8 byte) {
    command = command << 6;
    CommandHandler::addBytesInWord(byte);
}

/*
    ѕервые два бита указывают на составное слово.

    11 ** ** ** ** - начало слова
    10 ** ** ** ** или 01 ** ** ** ** - середина слова
    00 ** ** ** ** - конец слова

     онечное слово должно выгл€деть вот так:
    11 ** ** ** ... 01 ** ** ** ... 00 ** ** **

    “.к. возвращаемое значение должно быть типа int, то самое длинное слово может состо€ть только из 4 слов:
    11 ** ** **      01 ** ** **     01 ** ** **     00 ** ** **

    ћинимальное составное слово состоит из двух слов:
    11 ** ** **      00 ** ** **

    “ретий и четвертый бит в начале составного слова означают тип команды и отмечены # на схеме ниже:
    11 ## ** **

    ѕопытатьс€ получить команду. ¬ случае если команда не сформировалась, то возвращаетс€ -1
*/
unsigned int CommandHandler::tryGetCommand(unsigned __int8 byte) {
    

    if (byte >= BEGIN_COMMAND) {
        //первое слово в составном
        command = 0;
        addBytesInWord(removeFirstTwoBytes(byte));
    }
    else if (byte <= END_COMMAND) {
        //последнее слово в составном

        shiftAndAddBytesInWord(byte);
        result = command;
        command = 0;

        return result;
    }
    else {
        //слово находитс€ в середине составного
        shiftAndAddBytesInWord(removeFirstTwoBytes(byte));
    }

    return CommandHandler::NO_COMMAND_YET;
};