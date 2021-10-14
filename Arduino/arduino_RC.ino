#include <CommandHandler.h>
#include <SoftwareSerial.h>
#include <Servo.h>

Servo servo;
CommandHandler ch;

unsigned int currentCommand = 0;
unsigned int readByte = 0;
unsigned int purifiedCommand = 0;
unsigned int purifiedValue = 0;

unsigned int ENGINE_COMMAND = 3072;
unsigned int STEERING_WHEEL_COMMAND = 2048;

bool isSteeringWheelCommand(unsigned int command){
  return command == STEERING_WHEEL_COMMAND;
}

bool isEngineCommand(unsigned int command){
  return command == ENGINE_COMMAND;
}

bool isCommand(unsigned int command){
  return command != ch.NO_COMMAND_YET;
}

unsigned int getValue(unsigned int command) {
    return command & 1023;
}

unsigned int getCommand(unsigned int command) {
    return command & 3072;
}

void updateEnginge(unsigned int value){
  
}

void updateSteeringWheel(unsigned int value){
  servo.write(value);
}

void setup() {
  Serial.begin(9600);
  servo.attach(3);
}

void loop() {
  if (Serial.available() > 0)
  {
    readByte = Serial.read();
    currentCommand = ch.tryGetCommand(readByte);
//    Serial.println(readByte);
    
    if (isCommand(currentCommand)){
//        Serial.println("It is a command!");
        purifiedCommand = getCommand(currentCommand);
//        Serial.println(purifiedCommand);
        purifiedValue = getValue(currentCommand);
        
        if (isSteeringWheelCommand(purifiedCommand)){
//          Serial.println("it is a steering wheel command!");
          updateSteeringWheel(purifiedValue);
        } else if (isEngineCommand(purifiedCommand)){
//          Serial.println("it is an engine command!");
          updateEnginge(purifiedValue);
        }
    }
  }
}
