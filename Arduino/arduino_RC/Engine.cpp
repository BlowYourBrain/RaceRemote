class Engine {

    private:
        unsigned int readByte = 0;
        unsigned int currentCommand = 0;

        unsigned int purifiedValue = 0;
        unsigned int purifiedCommand = 0;

        unsigned int ENGINE_COMMAND = 3072;
        unsigned int STEERING_WHEEL_COMMAND = 2048;

        (*onUpdateEngine)(unsigned int value);
        (*onUpdateSteeringWheel)(unsigned int value);

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
        void updateEngine(unsigned int value){
         motor.setSpeed(value - 255);
        }

        void updateSteeringWheel(unsigned int value){
            &onUpdateSteeringWheel(value)
        }


    public:

        void onUpdateSteeringWheel((*function)(unsigned int value)){
            onUpdateSteeringWheel = function;
        }

        void onUpdateEngine((*function)(unsigned int value)){
            onUpdateEngine = function
        }
}