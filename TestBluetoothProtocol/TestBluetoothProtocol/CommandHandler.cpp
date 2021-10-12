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
unsigned int CommandHandler::tryGetCommand(unsigned __int8 byte) {
    

    if (byte >= BEGIN_COMMAND) {
        //������ ����� � ���������
        command = 0;
        addBytesInWord(removeFirstTwoBytes(byte));
    }
    else if (byte <= END_COMMAND) {
        //��������� ����� � ���������

        shiftAndAddBytesInWord(byte);
        result = command;
        command = 0;

        return result;
    }
    else {
        //����� ��������� � �������� ����������
        shiftAndAddBytesInWord(removeFirstTwoBytes(byte));
    }

    return CommandHandler::NO_COMMAND_YET;
};