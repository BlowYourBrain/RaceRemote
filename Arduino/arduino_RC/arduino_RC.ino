#include <CommandHandler.h>
#include <SoftwareSerial.h>
#include <GyverMotor.h>
#include <Servo.h>



Servo servo;
CommandHandler ch;
GMotor motor(DRIVER2WIRE, 6, 7, HIGH);

unsigned int readByte = 0;
unsigned int currentCommand = 0;

unsigned int purifiedValue = 0;
unsigned int purifiedCommand = 0;

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
  motor.setSpeed(value - 255);
}

void updateSteeringWheel(unsigned int value){
  servo.write(value);
}

void setup() {
  Serial.begin(9600);
  servo.attach(3);
  motor.setDirection(REVERSE);
  motor.setMode(FORWARD);
}

void loop() {
  if (Serial.available() > 0)
  {
    readByte = Serial.read();
    currentCommand = ch.tryGetCommand(readByte);
    
    if (isCommand(currentCommand)){
        purifiedCommand = getCommand(currentCommand);
        purifiedValue = getValue(currentCommand);
        
        if (isSteeringWheelCommand(purifiedCommand)){
          updateSteeringWheel(purifiedValue);
        } else if (isEngineCommand(purifiedCommand)){
          updateEnginge(purifiedValue);
        }
    }
  }
}
