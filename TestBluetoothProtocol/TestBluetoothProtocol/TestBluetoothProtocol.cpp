// TestBluetoothProtocol.cpp : Этот файл содержит функцию "main". Здесь начинается и заканчивается выполнение программы.
//

#include <iostream>
#include <string> 
#include <stdexcept>
#include "CommandHandler.h"
using namespace std;

void println(string s) {
    cout << s + "\n";
}

void divider() {
    println("*****************************************************");
}

void testPassed(string testName) {
    println(testName + " passed");
}

void assert(unsigned int expected, unsigned int actual) {
    if (expected != actual) {
        throw invalid_argument(
            "Expected: " + to_string(expected) + ", but actual: " + to_string(actual)
        );
    }
}

void runTest(string testName) {
    try
    {
        println(testName + " passed!");
    }
    catch (const std::exception& e)
    {
        println(testName + " failed!");
        println(e.what());
    }
}

void testFirstWord(unsigned int expected, unsigned int firstWord) {
    CommandHandler ch;
    assert(expected, ch.tryGetCommand(firstWord));
}

void testFirstWordWithStartCommand() {
    testFirstWord(-1, 192);
}

void testFirstWordWithMiddleCommand() {
    testFirstWord(-1, 128);
    testFirstWord(-1, 191);
    testFirstWord(-1, 64);
    testFirstWord(-1, 65);
}

void commonTestWithTwoWords(
    unsigned __int8 firstWord,
    unsigned __int8 lastWord,
    unsigned int expected
) {
    CommandHandler ch;

    assert(ch.NO_COMMAND_YET, ch.tryGetCommand(firstWord));
    assert(expected, ch.tryGetCommand(lastWord));
}

void testWithFirstAndLastWordsResultZero() {
    unsigned __int8 first = 192;
    unsigned __int8 last = 0;
    unsigned int expected = 0;
    
    commonTestWithTwoWords(first, last, expected);
}

void testFirstAndLastWordsResult180() {
    //1100 0010
    unsigned __int8 first = 194;
    //0011 0100
    unsigned __int8 last = 52;
    //1011 0100
    unsigned int expected = 180;

    commonTestWithTwoWords(first, last, expected);
}

void testOnlyLastWordResult63() {
    unsigned __int8 first = 192;
    unsigned __int8 last = 63;
    unsigned int expected = 63;

    commonTestWithTwoWords(first, last, expected);
}

void testThreeWords(
    unsigned __int8 first,
    unsigned __int8 middle,
    unsigned __int8 last,
    unsigned int expected
) {
    CommandHandler ch;

    assert(ch.NO_COMMAND_YET, ch.tryGetCommand(first));
    assert(ch.NO_COMMAND_YET, ch.tryGetCommand(middle));
    assert(expected, ch.tryGetCommand(last));
}

void testThreeWord63() {
    CommandHandler ch;
    unsigned __int8 first = 192;
    unsigned __int8 middle = 64;
    unsigned __int8 last = 63;
    unsigned int expected = 63;

    testThreeWords(
        first,
        middle,
        last,
        expected
    );
}

void testThreeWordResult49345() {
    CommandHandler ch;
    unsigned __int8 first = 204;
    unsigned __int8 middle = 67;
    unsigned __int8 last = 1;
    unsigned int expected = 49345;

    assert(ch.NO_COMMAND_YET, ch.tryGetCommand(first));
    assert(ch.NO_COMMAND_YET, ch.tryGetCommand(middle));
    assert(expected, ch.tryGetCommand(last));
}

unsigned int getValue(unsigned int command) {
    unsigned int val = 1023;
    return command & val;
}

unsigned int getCommand(unsigned int command) {
    unsigned int val = 3072;
    return command & val;
}


void testOutputCommand() {
    //unsigned __int8 first = 240; //11 11 00 00 
    unsigned __int8 first = 226; //11 10 00 10
    unsigned __int8 last = 52; //00 11 01 00
    unsigned int leftRight = 2048; // 10 00 00 00 00 00
    unsigned int forwardBackward = 3072; // 11 00 00 00 00 00
    unsigned int commandValue = 180;
    CommandHandler ch;

    ch.tryGetCommand(first);
    unsigned int command = ch.tryGetCommand(last);

    assert(leftRight, getCommand(command));
    assert(commandValue, getValue(command));
}



int main()
{
    println("Start of tests");
    divider();

    testFirstWordWithStartCommand();
    testPassed("testFirstWordWithStartCommand");
    
    testFirstWordWithMiddleCommand();
    testPassed("testFirstWordWithMiddleCommand");

    testWithFirstAndLastWordsResultZero();
    testPassed("testWithFirstAndLastWordsResultZero");

    testFirstAndLastWordsResult180();
    testPassed("testFirstAndLastWordsResult180");

    testOnlyLastWordResult63();
    testPassed("testOnlyLastWordResult63");

    testThreeWord63();
    testPassed("testThreeWord63");

    testThreeWordResult49345();
    testPassed("testThreeWordResult197377");

    testOutputCommand();
    testPassed("testOutputCommand");
   
    divider();
    println("End of tests");
}

// Запуск программы: CTRL+F5 или меню "Отладка" > "Запуск без отладки"
// Отладка программы: F5 или меню "Отладка" > "Запустить отладку"

// Советы по началу работы 
//   1. В окне обозревателя решений можно добавлять файлы и управлять ими.
//   2. В окне Team Explorer можно подключиться к системе управления версиями.
//   3. В окне "Выходные данные" можно просматривать выходные данные сборки и другие сообщения.
//   4. В окне "Список ошибок" можно просматривать ошибки.
//   5. Последовательно выберите пункты меню "Проект" > "Добавить новый элемент", чтобы создать файлы кода, или "Проект" > "Добавить существующий элемент", чтобы добавить в проект существующие файлы кода.
//   6. Чтобы снова открыть этот проект позже, выберите пункты меню "Файл" > "Открыть" > "Проект" и выберите SLN-файл.
