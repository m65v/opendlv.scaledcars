#ifndef OVERTAKER_H_
#define OVERTAKER_H_

#include <iostream>
#include <stdint.h>
#include <memory>
#include <math.h>
#include <cstdio>
#include <cmath>

#include <opendavinci/odcore/base/KeyValueConfiguration.h>
#include <opendavinci/odcore/base/Lock.h>
#include <opendavinci/odcore/base/module/DataTriggeredConferenceClientModule.h>
#include <opendavinci/odcore/io/conference/ContainerConference.h>

#include <opendavinci/odcore/data/Container.h>
#include <opendavinci/odcore/data/TimeStamp.h>
#include <opendavinci/odcore/wrapper/SharedMemoryFactory.h>
#include <opendavinci/odcore/wrapper/SharedMemory.h>
#include <opendavinci/GeneratedHeaders_OpenDaVINCI.h>
#include <automotivedata/GeneratedHeaders_AutomotiveData.h>
#include "automotivedata/generated/automotive/VehicleData.h"
#include "automotivedata/generated/automotive/miniature/SensorBoardData.h"
#include "odvdscaledcarsdatamodel/generated/group5/CommunicationLinkMSG.h"
#include "odvdscaledcarsdatamodel/generated/group5/OvertakerMSG.h"

namespace scaledcars {
    namespace control {

        using namespace std;
        using namespace group5;
        using namespace odcore::base::module;
        using namespace odcore::data;

        class Overtaker : public DataTriggeredConferenceClientModule {
        private:
            Overtaker(const Overtaker &/*obj*/);
            Overtaker& operator=(const Overtaker &/*obj*/);

        public:
            Overtaker(const int32_t &argc, char **argv);
            virtual ~Overtaker();

        private:
            virtual void setUp();
            virtual void tearDown();
            virtual void nextContainer(Container &c);

            void measuringMachine();

            enum StateMachine {
                FIND_OBJECT,
                FIND_OBJECT_PLAUSIBLE,
                HAVE_BOTH_IR,
                HAVE_NO_READING,
                KEEP_TURN_RIGHT,
                KEEP_TURN_RIGHT_END,
                ADJUST_TO_STRAIGHT,
                DISABLE
            };

            automotive::VehicleControl m_vehicleControl;

            bool Sim;

            int _state;

            OvertakerMSG overtakerMSG;

            const double OVERTAKING_DISTANCE;
            const double HEADING_PARALLEL;

            const double SPEED_CAR;
            const double TURN_ANGLE_CAR_LEFT;
            const double TURN_ANGLE_CAR_RIGHT;
            const double STRAIGHT_ANGLE_CAR;

            double IR_RR;
            double IR_FR;
            double IR_BACK;
            double US_C;
            double US_R;

            int odometerReal;
            int oldOdometer;
            int odo;

            int us_c_old;
            int _us_c_old;

            StateMachine stage;

            double distanceToObstacle;
            double distanceToObstacleOld;

            const int OBJECT_PLAUSIBLE_COUNT;
            int objectPlausibleCount;
            int objectDangerCount;

            int _stop;

            int valid_us;

            int park_state;

            int flag;
        };
    }
}

#endif