using System;
using System.Collections.Generic;
using System.Linq;

using Viewer;

namespace Viewer
{

    /// <summary>Interface of World</summary>
    /// World manages entities used in Simulation.
    ///  Entities given as Record that Received from Manager or Read From Log.
    ///  Entities has the map, so these'd be merged in this process.
    public interface World
    {
        /***[Generic Information]*********************************************************/
        /// <summary>Get Config</summary>
        Dictionary<string, string> Config { get; }

        /// <summary>Get Scale of World</summary>
        float Scale { get; set; }

        /// <summary>Get Current Time Step</summary>
        uint Time { get; }

        /// <summary>Get Max Time Step</summary>
        uint MaxTimeStep { get; }

        /// <summary>Get Simulation Map Name</summary>
        string MapName { get; }


        /***[Generic Methods]************************************************************/
        /// <summary>Clear all Entity</summary>
        void Clear();

        /// <summary>Update entities information</summary>
        /// <param name="result">Received Entity Information</param>
        bool Update(Record record);


        /***[Entity Accessor]************************************************************/
        List<Entity> TacticsAmbulance { get; }
        List<Entity> TacticsFire      { get; }
        List<Entity> TacticsPolice    { get; }
        List<Entity> Civilian         { get; }
        
        List<Entity> Blockade         { get; }
        
        List<Entity> ControlAmbulance { get; }
        List<Entity> ControlFire      { get; }
        List<Entity> ControlPolice    { get; }
        
        List<Entity> Road             { get; }
        List<Entity> Hydrant          { get; }
        List<Entity> Building         { get; }
        List<Entity> Refuge           { get; }
        List<Entity> GasStation       { get; }

        List<int> EntityIDs { get; }

        Entity GetEntity(int entityID);
        
        /***[Map Accessor]************************************************************/
        List<AreaInfo> Areas { get; }

        /***[Action Accessor]************************************************************/
        List<Action> Actions { get; }
    }

    /// <summary>Reference implement of World</summary>
    /// <summary></summary>
    public class EntityWorld : World
    {
        /***[Values]*********************************************************/
        protected uint time; // Current Time Step
        protected uint maxTimeStep; // Max Tiem Step
        protected string map; // Map name

        // List of Entities
        protected Dictionary<int, List<Entity>> entities;

        // mappings of EntityID to Entity
        protected Dictionary<int, Entity> entityRelation;

        /// <summary>config data</summary>
        private Dictionary<string, string> config;

        /// <summary>map informatin</summary>
        private List<AreaInfo> areaInfos;

        /// <summary>Action, Agent Commands</summary>
        private List<Action> actions;

        /***[Generic Information]*********************************************************/


        /// <summary>Get Config</summary>
        public Dictionary<string, string> Config
        {
            get {
                return config;
            }
        }

        /// <summary>Get Scale of World</summary>
        public float Scale { get; set; }

        /// <summary>Get Current Time Step</summary>
        public uint Time
        {
            get {
                return time;
            }
        }
        
        /// <summary>Get Max Time Step</summary>
        public uint MaxTimeStep
        {
            get
            {
                return maxTimeStep;
            }
        }

        /// <summary>Get Simulation Map Name</summary>
        public string MapName
        {
            get {
                return map;
            }
        }

        /***[Constructor]****************************************************************/
        public EntityWorld() {
            time = 0;
            maxTimeStep = 0;
            map = null;

            entities = new Dictionary<int, List<Entity>>();
            entityRelation = new Dictionary<int, Entity>();

            init();
        }


        /***[Generic Methods]************************************************************/
        /// <summary>Initialize all members</summary>
        private void init()
        {
            // Create Entity Lists
            entities.Add(ViewerKey.Entity.TacticsAmbulance, new List<Entity>());
            entities.Add(ViewerKey.Entity.TacticsFire, new List<Entity>());
            entities.Add(ViewerKey.Entity.TacticsPolice, new List<Entity>());
            entities.Add(ViewerKey.Entity.Civilian, new List<Entity>());
            entities.Add(ViewerKey.Entity.Blockade, new List<Entity>());
            entities.Add(ViewerKey.Entity.ControlAmbulance, new List<Entity>());
            entities.Add(ViewerKey.Entity.ControlFire, new List<Entity>());
            entities.Add(ViewerKey.Entity.ControlPolice, new List<Entity>());
            entities.Add(ViewerKey.Entity.Road, new List<Entity>());
            entities.Add(ViewerKey.Entity.Hydrant, new List<Entity>());
            entities.Add(ViewerKey.Entity.Building, new List<Entity>());
            entities.Add(ViewerKey.Entity.Refuge, new List<Entity>());
            entities.Add(ViewerKey.Entity.GasStation, new List<Entity>());

            config = null;
            areaInfos = null;
            actions = null;
        }

        /// <summary>Clear all Entity</summary>
        public void Clear() {
            entities.Clear();
            entityRelation.Clear();
            init();
        }

        /// <summary>Update entities information</summary>
        /// <param name="result">Received Entity Information</param>
        public bool Update(Record record)
        {
            if (record == null)
            {
                // Invalid. Abort;
                return false;
            }
            
            // If config incoming
            if (record.config != null)
            {
                string value;
                if (record.config.TryGetValue("gis.map.dir", out value))
                {
                    // map name available
                    if (value.Equals(this.map))
                    {
                        // World has simulation data, So got another world
                        this.map = value;
                        this.Clear();
                    }
                    else {
                        // World has no simulation data
                        this.map = value;
                    }
                }

                if (record.config.TryGetValue("kernel.timesteps", out value))
                {
                    // max Time Step available
                    maxTimeStep = uint.Parse(value);
                }

                // Keep Config
                this.config = record.config;
            }

            // If contains AreaInfo
            if (record.map != null)
            {
                areaInfos = new List<AreaInfo>(record.map);
            }

            // If contains Command
            if (record.commands != null)
            {
                actions = new List<Action>(record.commands);
            }

            // Update / Register Entities
            Entity[] update = null;
            if (record.world != null)
            {
                // Set World Entities with Incoming World
                Clear();
                update = record.world;
            }
            else if (record.changes != null)
            {
                // Update Entities with Incoming
                update = record.changes;
            }

            // Update with data
            if (update != null)
            {
                foreach (Entity e in update)
                {
                    if (!entityRelation.ContainsKey(e.id))
                    {
                        // Entity not Registered
                        entityRelation.Add(e.id, e);
                        entities[ViewerKey.Entity.Get(e.type)].Add(e);
                    }
                    else
                    {
                        // Entity Registed
                        entityRelation[e.id].Update(e);
                    }
                }
            }


            // Get Current Time Step
            if (record.time != 0)
            {
                time = record.time;
            }

            return true;
        }


        /***[Entity Accessor]************************************************************/
        public List<Entity> TacticsAmbulance { get { return entities[ViewerKey.Entity.TacticsAmbulance]; } }
        public List<Entity> TacticsFire      { get { return entities[ViewerKey.Entity.TacticsFire]; } }
        public List<Entity> TacticsPolice    { get { return entities[ViewerKey.Entity.TacticsPolice]; } }
        public List<Entity> Civilian         { get { return entities[ViewerKey.Entity.Civilian]; } }
        
        public List<Entity> Blockade         { get { return entities[ViewerKey.Entity.Blockade]; } }
        
        public List<Entity> ControlAmbulance { get { return entities[ViewerKey.Entity.ControlAmbulance]; } }
        public List<Entity> ControlFire      { get { return entities[ViewerKey.Entity.ControlFire]; } }
        public List<Entity> ControlPolice    { get { return entities[ViewerKey.Entity.ControlPolice]; } }
        
        public List<Entity> Road             { get { return entities[ViewerKey.Entity.Road]; } }
        public List<Entity> Hydrant          { get { return entities[ViewerKey.Entity.Hydrant]; } }
        public List<Entity> Building         { get { return entities[ViewerKey.Entity.Building]; } }
        public List<Entity> Refuge           { get { return entities[ViewerKey.Entity.Refuge]; } }
        public List<Entity> GasStation       { get { return entities[ViewerKey.Entity.GasStation]; } }

        public List<int> EntityIDs
        {
            get
            {
                return entityRelation.Keys.ToList();
            }
        }

        public Entity GetEntity(int entityID)
        {
            return entityRelation.ContainsKey(entityID) ? entityRelation[entityID] : null;
        }


        /***[Map Accessor]************************************************************/
        public List<AreaInfo> Areas
        {
            get
            {
                return areaInfos;
            }
        }

        /***[Action Accessor]************************************************************/
        public List<Action> Actions
        {
            get
            {
                return actions;
            }
        }
        
    }
}
