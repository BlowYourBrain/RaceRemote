// Host-only adapter. The state machine and packet codec come from the firmware
// include directory supplied by the runner, never from a copied implementation.
#include <control.h>
#include <array>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <string>

int main() {
    std::array<rc::Control, 6> cars{};
    std::string line;
    while (std::getline(std::cin, line)) {
        std::istringstream input(line);
        unsigned car, client;
        uint32_t session, now;
        std::string operation, hex;
        if (!(input >> car >> operation >> client >> session >> now >> hex) ||
            car >= cars.size() || client > 255) return 2;
        auto& control = cars[car];
        bool accepted = false;
        uint8_t response_kind = 0;
        if (operation == "connect") {
            accepted = control.connect(static_cast<uint8_t>(client), session, now);
            if (accepted) response_kind = rc::Hello;
        } else if (operation == "accept") {
            if (hex.size() != rc::packet_size * 2) return 3;
            std::array<uint8_t, rc::packet_size> bytes{};
            for (size_t i = 0; i < bytes.size(); ++i) {
                unsigned value;
                std::istringstream pair(hex.substr(i * 2, 2));
                if (!(pair >> std::hex >> value)) return 4;
                bytes[i] = static_cast<uint8_t>(value);
            }
            accepted = control.accept(static_cast<uint8_t>(client), bytes.data(), bytes.size(), now);
            if (accepted && control.active) response_kind = rc::Ack;
        } else if (operation == "disconnect") {
            control.disconnect(static_cast<uint8_t>(client));
            accepted = true;
        } else if (operation == "tick") {
            accepted = control.tick(now);
        } else if (operation == "state") {
            accepted = true;
        } else return 5;

        std::ostringstream response;
        if (response_kind) {
            std::array<uint8_t, rc::packet_size> bytes{};
            rc::encode({response_kind, control.token, control.sequence, control.throttle, control.steering}, bytes.data());
            for (const auto byte : bytes) response << std::hex << std::setfill('0') << std::setw(2) << unsigned(byte);
        } else response << '-';
        std::cout << accepted << ' ' << control.active << ' ' << control.armed << ' '
                  << unsigned(control.owner) << ' ' << control.token << ' ' << control.sequence << ' '
                  << control.throttle << ' ' << control.steering << ' ' << control.last_command << ' '
                  << control.stop_revision << ' ' << response.str() << std::endl;
    }
}
