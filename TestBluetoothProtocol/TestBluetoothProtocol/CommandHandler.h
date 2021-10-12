#pragma once
//
// Created by fetel on 06.10.2021.
//

#ifndef UNTITLED_COMMANDHANDLER_H
#define UNTITLED_COMMANDHANDLER_H
#include <limits.h>

/*
* Ётот класс формирует последовательность байтов (слов) в int (составное слово).
* */
class CommandHandler
{
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

private:
    void addBytesInWord(unsigned __int8 byte);

    void shiftAndAddBytesInWord(unsigned __int8 byte);

public:
    static unsigned int const NO_COMMAND_YET = UINT_MAX;

    unsigned int tryGetCommand(unsigned __int8 byte);

};



#endif
