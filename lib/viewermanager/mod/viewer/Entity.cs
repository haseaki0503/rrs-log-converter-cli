using System;
using System.Collections.Generic;

namespace Viewer
{
    /*******************************************************************/
    public class AreaInfo
    {
        public int id { get; set; }
        public string type { get; set; }
        public int? x { get; set; }
        public int? y { get; set; }
        public Edge[] edges { get; set; }
        public int[] neighbours { get; set; }

        override
        public string ToString()
        {
            return Utilities.ToMsg(this);
        }
    }

    public class Action
    {
        public int id { get; set; }
        public string type { get; set; }
        public int[] path { get; set; }
        public int? x { get; set; }
        public int? y { get; set; }
        public int? channel { get; set; }
        public int? target { get; set; }
        public int? water { get; set; }

        public override string ToString()
        {
            return Utilities.ToMsg(this);
        }
    }

    public class Record
    {
        public uint time { get; set; }
        public AreaInfo[] map { get; set; }
        public Entity[] world { get; set; }
        public Action[] commands { get; set; }
        public Entity[] changes { get; set; }
        public Dictionary<string, string> config { get; set; }

        override
        public string ToString()
        {
            return Utilities.ToMsg(this);
        }
    };
    /*******************************************************************/
    
    public class Entity
    {
        public int id { get; set; }
        public string type { get; set; }
        public bool? deleted { get; set; }

        public int? x { get; set; }
        public int? y { get; set; }

        public int? position { get; set; }

        public int? damage { get; set; }
        public int? buried { get; set; }
        public int? hp { get; set; }

        public Point[] history { get; set; }
        public int? travel { get; set; }

        public int? board { get; set; }
        public int? water { get; set; }

        public int[] blockades { get; set; }

        public int? temp { get; set; }
        public int? broken { get; set; }
        public int? fiery { get; set; }

        public int? repairCost { get; set; }
        public Point[] apexes { get; set; }

        public Entity()
        {
            id = 0;
            //type = ViewerKey.Entity.Unknown;
            //deleted = false;
            //x = null; y = null; position = null;
            //damage = null; buried = null; hp = null;
            //history = null; travel = null;
            //board = null; water = null;
            //blockades = null;
            //temp = null; broken = null; fiery = null;
            //repairCost = null; apexes = null;
        }

        public Entity Update(Entity e)
        {
            if (id != e.id) return this;

            Utilities.Update(this, e);

            return this;
        }

        override
        public string ToString()
        {
            return Utilities.ToMsg(this);
        }
    }


    public class Point
    {
        public int x { get; set; }
        public int y { get; set; }

        public override string ToString()
        {
            return Utilities.ToMsg(this);
        }
    }
    public class Edge
    {
        public Point start { get; set; }
        public Point end { get; set; }

        public override string ToString()
        {
            return Utilities.ToMsg(this);
        }
    }


    /*******************************************************************/
    public class Request
    {
        public int request { get; set; }
        public int? time { get; set; }

        public static int OPEN = (1 << 0);
        public static int CLOSE = (1 << 1);
        public static int ACTION = (1 << 2);
        public static int UPDATE = (1 << 3);
        public static int WORLD = (1 << 4);
        public static int PERCEPTION = (1 << 5);
        public static int CONFIG = (1 << 6);
        public static int MAP = (1 << 7);

        public Request()
        {
            request = OPEN;
            time = null;
        }

        public override string ToString()
        {
            return Utilities.ToMsg(this);
        }
    }

    public class Result
    {
        public int request { get; set; }
        public string message { get; set; }
        public bool? result { get; set; }
        public int? time { get; set; }
        public Record record { get; set; }

        override
        public string ToString() {
            return Utilities.ToMsg(this);
        }
    };
}

