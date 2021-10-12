#pragma once
//
// Created by fetel on 06.10.2021.
//

#ifndef UNTITLED_COMMANDHANDLER_H
#define UNTITLED_COMMANDHANDLER_H
#include <limits.h>

/*
* Этот класс формирует последовательность байтов (слов) в int (составное слово).
* */
class CommandHandler
{
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

private:
    void addBytesInWord(unsigned int readByte);

    void shiftAndAddBytesInWord(unsigned int readByte);

public:
    static unsigned int const NO_COMMAND_YET = UINT_MAX;

    unsigned int tryGetCommand(unsigned int readByte);

};



#endif
