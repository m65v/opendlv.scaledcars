#include "SerialReceiveHandler.h"
#include "protocol.c"
#include "serial.c"
#include "arduino.c"

namespace scaledcars {
    namespace control {

        using namespace std;
        using namespace odcore;
        using namespace odcore::base::module;
        using namespace odcore::data;
        using namespace odcore::wrapper;
        using namespace odcore::data::dmcp;
        using namespace automotive;
        using namespace automotive::miniature;
        using namespace odcore::base;

        string SERIAL_PORT = "";
        int BAUD_RATE = 115200;
        serial_state *serial_;
        SensorsMSG sbd;
        map<uint32_t, double> sensors;
        int realOdometer = 0;
        long counter = 0;
        bool isSensorValues = false;
        vector<int> ur_list_values;
        vector<int> ur2_list_values;
        vector<int> ir_side_front_list_values;
        vector<int> ir_side_back_list_values;
        vector<int> ir_back_list_values;
        const uint32_t ONE_SECOND = 1000 * 1000;

        /**
       * Filters the data according to what sensor it represents and that sensors ranges.
       * Ultrasonic and IR-sensor values are added to the "values", incrementing the "numbers".
       * Every odometer value is passed forward for packing and sending.
       *
       * @param data to filter
       */
        void SerialReceiveHandler::filterData(int id, int value) {

            if ((id == ID_IN_ULTRASONIC_CENTER || id == ID_IN_ULTRASONIC_SIDE_FRONT) && value > 0) {
                if (id == ID_IN_ULTRASONIC_CENTER) {
                    ur_list_values.push_back(value);
                } else {
                    ur2_list_values.push_back(value);
                }

                //IR-SENSOR [ID 3] [ID 4] with value between 3 - 30
            } else if ((id == ID_IN_ULTRASONIC_CENTER || id == ID_IN_ULTRASONIC_SIDE_FRONT) && value == 0) {
                if (id == ID_IN_ULTRASONIC_CENTER) {
                    ur_list_values.push_back(-1);
                } else {
                    ur2_list_values.push_back(-1);
                }

                //IR-SENSOR [ID 3] [ID 4] with value between 3 - 40
            } else if (
                    (id == ID_IN_INFRARED_SIDE_FRONT || id == ID_IN_INFRARED_SIDE_BACK || id == ID_IN_INFRARED_BACK) &&
                    value > 2) {
                if (id == ID_IN_INFRARED_SIDE_FRONT) {
                    ir_side_front_list_values.push_back(value);
                } else if (id == ID_IN_INFRARED_SIDE_BACK) {
                    ir_side_back_list_values.push_back(value);
                } else {
                    ir_back_list_values.push_back(value);
                }

            } else if (
                    (id == ID_IN_INFRARED_SIDE_FRONT || id == ID_IN_INFRARED_SIDE_BACK || id == ID_IN_INFRARED_BACK) &&
                    value == 0) {
                if (id == ID_IN_INFRARED_SIDE_FRONT) {
                    ir_side_front_list_values.push_back(-1);
                } else if (id == ID_IN_INFRARED_SIDE_BACK) {
                    ir_side_back_list_values.push_back(-1);
                } else {
                    ir_back_list_values.push_back(-1);
                }

                //ODOMETER [ID 6] with value between 0 - 255
            } else if (id == ID_IN_ENCODER) {
                realOdometer += value;

                if (realOdometer >= KM_IN_CM) {
                    realOdometer -= KM_IN_CM;
                    counter++;
                }

                sbd.setTravelledDistance(realOdometer);
                sbd.setTravelledKm(counter);
            }
        }

        void __on_read(uint8_t b) {
            cout << ">> read " << (int) b << endl;
        }

        void __on_write(uint8_t b) {
            cout << "<< write " << (int) b << endl;
        }

        SerialReceiveHandler::SerialReceiveHandler(const int32_t &argc, char **argv) :
                DataTriggeredConferenceClientModule(argc, argv, "SerialReceiveHandler"),
                serial(){}

        SerialReceiveHandler::~SerialReceiveHandler() {}

        void SerialReceiveHandler::setUp() {
            try {
                KeyValueConfiguration kv = getKeyValueConfiguration();

                SERIAL_PORT =  kv.getValue<string>("serialreceivehandler.sensors");

                const string _S_PORT = SERIAL_PORT;

                cerr << "Setting up serial handler to port " << SERIAL_PORT << endl;

                this->serial = serial_new();

                this->serial->incoming_frame_t = FRAME_T2;
                this->serial->outgoing_frame_t = FRAME_T1;

                this->serial->on_write = &__on_write;
                this->serial->on_read = &__on_read;

                const char *_port = _S_PORT.c_str();
                serial_open(this->serial, _port, BAUD_RATE);

                serial_handshake(this->serial, '\n');
                protocol_data d;
                d.id = 0;
                d.value = 90;
                serial_send(this->serial, d);

                odcore::base::Thread::usleepFor(5 * ONE_SECOND);

                serial_start(this->serial);

                serial_ = this->serial;
            } catch (const char *msg) {
                cerr << "Serial error : " << msg << endl;
            }
        }

        void SerialReceiveHandler::tearDown() {
            cerr << "Shutting down serial handler" << endl;

            serial_stop(this->serial);
            serial_free(this->serial);
        }

        void SerialReceiveHandler::nextContainer(Container &c) {
                c = c;

                int pending = g_async_queue_length(serial_->incoming_queue);
                protocol_data incoming;
                ur_list_values.clear();
                ur2_list_values.clear();
                ir_side_front_list_values.clear();
                ir_side_back_list_values.clear();
                ir_back_list_values.clear();
                for (int i = 0; i < pending; i++) {
                    if (serial_receive(serial_, &incoming)) {
                        filterData(incoming.id, incoming.value);
                    }

                    if (ur_list_values.size() > 0) {
                        sort(ur_list_values.begin(), ur_list_values.end());
                        int med = (int) ur_list_values.size() / 2;
                        sensors[ID_IN_ULTRASONIC_CENTER] = ur_list_values[med];
                    }

                    if (ur2_list_values.size() > 0) {
                        sort(ur2_list_values.begin(), ur2_list_values.end());
                        int med = (int) ur2_list_values.size() / 2;
                        sensors[ID_IN_ULTRASONIC_SIDE_FRONT] = ur2_list_values[med];
                    }

                    if (ir_side_front_list_values.size() > 0) {
                        sort(ir_side_front_list_values.begin(), ir_side_front_list_values.end());
                        int med = (int) ir_side_front_list_values.size() / 2;
                        sensors[ID_IN_INFRARED_SIDE_FRONT] = ir_side_front_list_values[med];
                    }

                    if (ir_side_back_list_values.size() > 0) {
                        sort(ir_side_back_list_values.begin(), ir_side_back_list_values.end());
                        int med = (int) ir_side_back_list_values.size() / 2;
                        sensors[ID_IN_INFRARED_SIDE_BACK] = ir_side_back_list_values[med];
                    }

                    if (ir_back_list_values.size() > 0) {
                        sort(ir_back_list_values.begin(), ir_back_list_values.end());
                        int med = (int) ir_back_list_values.size() / 2;
                        sensors[ID_IN_INFRARED_BACK] = ir_back_list_values[med];
                    }

                    isSensorValues = true;
                }

                if (isSensorValues) {
                    sendSensorBoardData(sensors);
                }
        }

        /**
      	* Pack a map of sensor values ad SensorBoardData.
      	* Then put the SensorBoardData into a Container and send the
      	* Container to the Conference.
      	*
      	* @param a map of sensor data from every ultrasonic and ir-sensor
			*/
        void SerialReceiveHandler::sendSensorBoardData(map<uint32_t, double> sensor) {
            sbd.setMapOfDistances(sensor);
            Container c(sbd);
            getConference().send(c);
            isSensorValues = false;
        }
    }
}
