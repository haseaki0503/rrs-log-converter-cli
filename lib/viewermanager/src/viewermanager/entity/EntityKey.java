package viewermanager.entity;

/**
 * Entityの型指定やActionの種類を示す固定文字列
 * */
public class EntityKey {

    public class Action {

        public static final String MOVE = "Move";
        public static final String REST = "Rest";
        public static final String LOAD = "Load";
        public static final String UNLOAD = "Unload";
        public static final String RESCUE = "Rescue";
        public static final String EXTINGUISH = "Extinguish";
        public static final String CLEAR = "Clear";
        public static final String LCLEAR = "LClear";
        public static final String RADIO = "Radio";
        public static final String VOICE = "Voice";
        public static final String SUBSCRIBE = "Subscribe";
        public static final String TELL = "Tell";

    }

    public class Entity
    {
        // ENTITY TYPE
        public static final String TACTICS_AMBULANCE = "TacticsAmbulance";
        public static final String TACTICS_FIRE = "TacticsFire";
        public static final String TACTICS_POLICE = "TacticsPolice";

        // NOT IMPLEMENTED BY SERVER IN 7/18
        public static final String CONTROL_AMBULANCE = "ControlAmbulance";
        public static final String CONTROL_FIRE = "ControlFire";
        public static final String CONTROL_POLICE = "ControlPolice";

        public static final String CIVILIAN = "Civilian";
        public static final String REFUGE = "Refuge";

        public static final String BUILDING = "Building";
        public static final String ROAD = "Road";
        public static final String BLOCKADE = "Blockade";

        // NOT IMPLEMENTED BY SERVER IN 7/18
        public static final String GAS_STATION = "GasStation";
        public static final String HYDRANT = "Hydrant";

        public static final String AREA = "Area";
    }

    public static final String UNKNOWN = "Unknown";
}
