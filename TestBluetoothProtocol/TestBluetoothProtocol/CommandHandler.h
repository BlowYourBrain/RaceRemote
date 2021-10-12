#pragma once
//
// Created by fetel on 06.10.2021.
//

#ifndef UNTITLED_COMMANDHANDLER_H
#define UNTITLED_COMMANDHANDLER_H
#include <limits.h>

/*
* ���� ����� ��������� ������������������ ������ (����) � int (��������� �����).
* */
class CommandHandler
{
    /*
        ������ ��� ���� ��������� �� ��������� �����.

        11 ** ** ** ** - ������ �����
        10 ** ** ** ** ��� 01 ** ** ** ** - �������� �����
        00 ** ** ** ** - ����� �����

        �������� ����� ������ ��������� ��� ���:
        11 ** ** ** ... 01 ** ** ** ... 00 ** ** **

        �.�. ������������ �������� ������ ���� ���� int, �� ����� ������� ����� ����� �������� ������ �� 4 ����:
        11 ** ** **      01 ** ** **     01 ** ** **     00 ** ** **

        ����������� ��������� ����� ������� �� ���� ����:
        11 ** ** **      00 ** ** **

        ������ � ��������� ��� � ������ ���������� ����� �������� ��� ������� � �������� # �� ����� ����:
        11 ## ** **

        ���������� �������� �������. � ������ ���� ������� �� ��������������, �� ������������ -1
    */

private:
    void addBytesInWord(unsigned __int8 byte);

    void shiftAndAddBytesInWord(unsigned __int8 byte);

public:
    static unsigned int const NO_COMMAND_YET = UINT_MAX;

    unsigned int tryGetCommand(unsigned __int8 byte);

};



#endif
