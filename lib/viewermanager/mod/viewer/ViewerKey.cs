
namespace Viewer
{
    public class ViewerKey
    {
        public static int GetFromName<T>(string name)
        {
            return Utilities.GetStaticFieldFrom<int, T>(name);
        }

        public struct Entity {
            public static readonly int Unknown          = 0;
            public static readonly int TacticsAmbulance = 1;
            public static readonly int TacticsFire      = 2;
            public static readonly int TacticsPolice    = 3;
            public static readonly int ControlAmbulance = 4;
            public static readonly int ControlFire      = 5;
            public static readonly int ControlPolice    = 6;
            public static readonly int Civilian         = 7;
            public static readonly int Refuge           = 8;
            public static readonly int Building         = 9;
            public static readonly int Road             = 10;
            public static readonly int Blockade         = 11;
            public static readonly int GasStation       = 12;
            public static readonly int Hydrant          = 13;
            public static readonly int Area             = 14;

            public static int Get(string name) { return ViewerKey.GetFromName<Entity>(name); }
        }

        public struct Action {
            public static readonly int Unknown          = 0;
            public static readonly int Move             = 1;
            public static readonly int Rest             = 2;
            public static readonly int Load             = 3;
            public static readonly int Unload           = 4;
            public static readonly int Rescue           = 5;
            public static readonly int Extinguish       = 6;
            public static readonly int Clear            = 7;
            public static readonly int LClear           = 8;
            public static readonly int Radio            = 9;
            public static readonly int Voice            = 10;
            public static readonly int Subscribe        = 11;
            public static readonly int Tell             = 12;

            public static int Get(string name) { return ViewerKey.GetFromName<Action>(name); }
        }
    }
    
}