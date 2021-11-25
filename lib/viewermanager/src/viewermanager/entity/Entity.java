package viewermanager.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.log4j.Logger;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import viewermanager.ViewerManagerKeys;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Information of Entity, contains all information for Area,Building,Human.
 *   use parameters as needed.
 * Entityの情報，AreaやBuilding,Humanのすべての情報を含む．
 *   必要な情報だけ使う．
 * */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Entity {

    /**
     * id of entity
     * EntityのID
     * */
    public Integer id;

    /**
     * type of entity : Look <code>EntityKey.Entity</code>
     * Entityの種類 : <code>EntityKey.Entity</code>を参照
     * */
    public String type;

    /**
     * [USE AS DIFFER] created on this step;
     * [差分利用] このステップで作成されました．
     * */
    public Boolean created;

    /**
     * [USE AS DIFFER] deleted on this step;
     * [差分利用] このステップで削除されました．
     * */
    public Boolean deleted;

    //--------------------------------------------
    // Parameter for Human, Blockade, Area
    /**
     * coordinate x
     * x座標
     * */
    public Integer x;

    /**
     * coordinate y
     * y座標
     */
    public Integer y;

    //--------------------------------------------
    // Parameter for Human, Blockade
    /**
     * position area's entity id
     * 存在するAreaのEntityId
     * */
    public Integer position;

    //--------------------------------------------
    // Parameter of Human
    /**
     * damaged value
     * 受けたダメージ
     * */
    public Integer damage;

    /**
     * buried value
     * 埋没してる度合い
     * */
    public Integer buried;

    /**
     * current hp
     * 現在のHP
     * */
    public Integer hp;

    /**
     * position history: what point passes to move
     * 位置の履歴： どの点を通って移動したか
     * */
    public List<Point> history;

    /**
     * travel distances. how long the agent move on this step;
     * 移動経路長．このステップでどの程度の距離移動したか
     * */
    public Integer travel;

    // Tactics Ambulance
    /**
     * ambulance boarding agent id (not implemented)
     * 救急隊が搬送しているAgentのID (未実装)
     * */
    public Integer board;
    // Tactics Fire
    /**
     * water quantity tanked on FireBrigade
     * 消防隊に積載されている水の量
     * */
    public Integer water;

    //-------------------------------------------

    // Road
    /**
     * blockades id list on the Area
     * そのAreaの上に存在する障害物のIDリスト
     * */
    public List<Integer> blockades;

    // Building
    /**
     * temperature of building
     * 建物の温度
     * */
    public Integer temp;

    /**
     * broken quantity of building
     * 建物の倒壊律
     * */
    public Integer broken;

    /**
     * Fiery quantity of building
     * 建物の燃焼度
     * */
    public Integer fiery;

    //-------------------------------------------
    // Parameter of Blockade
    /**
     * repair cost of blockade (legacy)
     * 障害物の修繕コスト
     * */
    public Integer repairCost;

    /**
     * apexes of blockades
     * 障害物の頂点情報(形状)
     * */
    public List<Point> apexes;

    /**
     * create empty entity
     * */
    public Entity() {
        /// Set All Member to NULL
        for(Field field : this.getClass().getFields()) {
            try {
                field.set(this, null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * create entity from RRS entity
     * */
    public Entity(StandardEntity entity) {
        /// Create Instance from StandardEntity
        if(entity instanceof Human) packAgent((Human)entity);
        else if(entity instanceof Area) packArea((Area)entity);
        else if(entity instanceof Blockade) { packBlockade((Blockade)entity); }

        /// Reserve id
        this.id = entity.getID().getValue();
    }

    /**
     * check is the empty entity.
     *  empty means "all parameter is null, without `id` and `type`".
     * 空のEntityか確かめる．
     *  空のEntityは「idとtype以外のすべてのパラメータがnull」を示す．
     * */
    @JsonIgnore
    public boolean isEmpty() {
        // Empty : non null only id and type

        boolean filled = false;
        try {
            for (Field field : this.getClass().getFields()) {
                if (!field.getName().equals("id") && !field.getName().equals("type")) {
                    // If any field filled, this may true
                    filled |= (field.get(this) != null);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch(NullPointerException ex) {
            //
        }

        // empty == !filled
        return !filled;
    }

    /**
     * merge entity given and assign (overwritten)
     * 与えられたEntityの情報で上書きする
     * */
    public void update(Entity entity) {
        /// update members use entity
        if(entity == null) return;

        try {
            /// Check all members
            for(Field field : entity.getClass().getFields()) {
                if(!Objects.equals(field.get(entity), null) && !Objects.equals(field.get(this), field.get(entity)))
                {
                    /// if `entity`'s field is not null and difference from `this`
                    field.set(this, field.get(entity));
                }
            }
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert Human of RRS Entity to ViewerManager Entity.
     * RRSのHumanを，ViewerManagerのEntityに変換する
     * */
    protected void packAgent(Human human) {
        if(human instanceof AmbulanceTeam) {
            this.type = EntityKey.Entity.TACTICS_AMBULANCE;
        }
        else if(human instanceof FireBrigade) {
            FireBrigade fireBrigade = (FireBrigade)human;
            if(fireBrigade.isWaterDefined()) {
                this.water = fireBrigade.getWater();
            }
            this.type = EntityKey.Entity.TACTICS_FIRE;
        }
        else if(human instanceof PoliceForce) {
            this.type = EntityKey.Entity.TACTICS_POLICE;
        }
        else if (human instanceof Civilian) {
            this.type = EntityKey.Entity.CIVILIAN;
        }
        else {
            this.type = EntityKey.UNKNOWN;
        }

        if(human.isXDefined()) {
            this.x = human.getX();
        }
        if(human.isYDefined()) {
            this.y = human.getY();
        }
        if(human.isBuriednessDefined()) {
            this.buried = human.getBuriedness();
        }
        if(human.isDamageDefined()) {
            this.damage = human.getDamage();
        }
        if(human.isHPDefined()) {
            this.hp = human.getDamage();
        }
        if(human.isPositionDefined()) {
            this.position = human.getPosition().getValue();
        }
        if (human.isPositionHistoryDefined()) {
            int[] path = human.getPositionHistory();
            if(path.length != 0) {
                this.history = new ArrayList<>();
                for (int i = 0; i < path.length; i += 2) {
                    this.history.add(new Point(path[i], path[i + 1]));
                }
            }
        }
        if(human.isTravelDistanceDefined()) {
            this.travel = human.getTravelDistance();
        }
    }

    /**
     * Convert Area of RRS Entity to ViewerManager Entity.
     * RRSのAreaを，ViewerManagerのEntityに変換する
     * */
    protected void packArea(Area area) {
        if (area instanceof Building) {
            if (area instanceof Refuge) {
                this.type = EntityKey.Entity.REFUGE;
            } else if (area instanceof GasStation) {
                this.type = EntityKey.Entity.GAS_STATION;
            } else if (area instanceof AmbulanceCentre) {
                this.type = EntityKey.Entity.CONTROL_AMBULANCE;
            } else if (area instanceof FireStation) {
                this.type = EntityKey.Entity.CONTROL_FIRE;
            } else if (area instanceof PoliceOffice) {
                this.type = EntityKey.Entity.CONTROL_POLICE;
            } else {
                this.type = EntityKey.Entity.BUILDING;
            }

            Building building = (Building) area;
            if (building.isBrokennessDefined()) {
                this.broken = building.getBrokenness();
            }
            if (building.isTemperatureDefined()) {
                this.temp = building.getTemperature();
            }
            if (building.isFierynessDefined()) {
                this.fiery = building.getFieryness();
            }
        } else if (area instanceof Road) {
            if (area instanceof Hydrant) {
                this.type = EntityKey.Entity.HYDRANT;
            } else {
                this.type = EntityKey.Entity.ROAD;
            }
        }
        if (area.isBlockadesDefined() && !area.getBlockades().isEmpty()) {
            this.blockades = area.getBlockades().stream().map(EntityID::getValue).collect(Collectors.toList());
        }
    }

    /**
     * Convert Blockade of RRS Entity to ViewerManager Entity.
     * RRSのBlockadeを，ViewerManagerのEntityに変換する
     * */
    protected void packBlockade(Blockade blockade) {
        this.type = EntityKey.Entity.BLOCKADE;
        if(blockade.isXDefined()) { this.x = blockade.getX(); }
        if(blockade.isYDefined()) { this.y = blockade.getY(); }
        if(blockade.isPositionDefined()) { this.position = blockade.getPosition().getValue(); }
        if(blockade.isApexesDefined()) {
            int[] apexes = blockade.getApexes();
            this.apexes = new ArrayList<>();
            for(int i=0; i<apexes.length; i+=2) {
                this.apexes.add(new Point(apexes[i], apexes[i+1]));
            }
        }
        if(blockade.isRepairCostDefined()) { this.repairCost = blockade.getRepairCost(); }
    }


    //-------------------------------------------------------------------------
    /**
     * Get Difference e2 from e1
     * e1から，e2の差分を出す．
     *
     * | param | e1 | e2 | differ |
     * |   A   | 10 | 11 |   11   |
     * |   B   |  4 |  4 |  null  |
     * */
    public static Entity difference(Entity e1, Entity e2) {
        /// Get Difference e2 from e1
        if (e1 != null && e2 != null && !e1.id.equals(e2.id)) return null;
        Entity entity = new Entity();

        try {
            for (Field field : entity.getClass().getFields()) {
                /// Compare all members
                if (!Objects.equals(field.get(e1), field.get(e2))) {
                    /// keep
                    field.set(entity, field.get(e2));
                }
            }
        } catch (IllegalAccessException e) {
            Logger logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
            if (logger != null) {
                logger.warn("cannot parse difference correctly", e);
            }
            return null;
        }

        entity.id = e1.id;
//        entity.type = e1.type;

        return entity;
    }

    /**
     * Get Difference e2 from e1
     * e1から，e2の差分を出す．
     *
     * | param | e1 | e2 | differ |
     * |   A   | 10 | 11 |   11   |
     * |   B   |  4 |  4 |  null  |
     * */
    public static Entity difference(Entity e1, StandardEntity e2) {
        // Get Difference e2 from e1
        if (e1 == null) {
            // e1 is null;
            return new Entity(e2);
        } else if (!e1.id.equals(e2.getID().getValue())) {
            // not a same entity
            return null;
        }
        // create new entity to set data;
        Entity entity = new Entity();
        entity.id = e1.id;

        // Human
        if (e2 instanceof Human) {
            if (e2 instanceof FireBrigade) {
                // If e2 is FireBrigade, check Water
                FireBrigade fb = (FireBrigade) e2;

                // The water defined and e1.water != fb.water();
                if (fb.isWaterDefined() && !Objects.equals(e1.water, fb.getWater())) {
                    entity.water = fb.getWater();
                }
            }

            // Check As Human
            Human human = (Human) e2;
            if (human.isBuriednessDefined()
                    && !Objects.equals(e1.buried, human.getBuriedness())) {
                // Buriedness Defined, and not same value of the e1's
                entity.buried = human.getBuriedness();
            }
            if (human.isTravelDistanceDefined()
                    && !Objects.equals(e1.travel, human.getTravelDistance())) {
                // Travel Distance defined, and not same value of the e1's
                entity.travel = human.getTravelDistance();
            }

            if (human.getPositionHistory() != null && human.getPositionHistory().length != 0)
            {
                // Position History defined, and
                //      1. e1's history is not defined
                //      2. e1's history length is not same points on human's
                //      3. "human's initial point of history" is not same of e1's point
                if (e1.history == null
                        || (e1.x == null || e1.y == null)
                        || (human.getPositionHistory().length / 2 == e1.history.size())
                        || (human.getPositionHistory()[0] != e1.x)
                        || (human.getPositionHistory()[1] != e1.y))
                {
                    // get each position
                    int[] path = human.getPositionHistory();
                    if (path.length != 0) {
                        entity.history = new ArrayList<>();
                        for (int i = 0; i < path.length; i += 2) {
                            // data put as [x1, y1, x2, y2, x3, y3, ...]
                            entity.history.add(new Point(path[i], path[i + 1]));
                        }
                    }
                }
            }

            if (human.isPositionDefined()
                    && !Objects.equals(e1.position, human.getPosition().getValue())) {
                // Defined, and the value is not same of e1's one.
                entity.position = human.getPosition().getValue();
            }
            if (human.isHPDefined()
                    && !Objects.equals(e1.hp, human.getHP())) {
                // ditto
                entity.hp = human.getHP();
            }
            if (human.isDamageDefined()
                    && !Objects.equals(e1.damage, human.getDamage())) {
                // ditto
                entity.damage = human.getDamage();
            }
            if ((human.isXDefined() && human.isYDefined())
                    && (human.getX() >= 0 && human.getY() >= 0)
                    && (!Objects.equals(e1.x, human.getX()) || !Objects.equals(e1.y, human.getY())))
            {
                // X, Y defined, and it's on positive, and not same value between e1's
                entity.x = human.getX();
                entity.y = human.getY();
            }

        } // fi Human
        else if (e2 instanceof Area)
        {
            // e2 is Area
            if (e2 instanceof Building) {
                // and Building
                Building building = (Building) e2;

                if (building.isFierynessDefined()
                        && !Objects.equals(e1.fiery, building.getFieryness())) {
                    // Defined, and the value is not same as e1's
                    entity.fiery = building.getFieryness();
                }
                if (building.isTemperatureDefined() && !Objects.equals(e1.temp, building.getTemperature())) {
                    // Ditto
                    entity.temp = building.getTemperature();
                }
                if (building.isBrokennessDefined() && !Objects.equals(e1.broken, building.getBrokenness())) {
                    // Ditto
                    entity.broken = building.getBrokenness();
                }
            }

            Area area = (Area)e2;

            if (area.isBlockadesDefined()
                    && (e1.blockades != null && area.getBlockades().size() != e1.blockades.size()))
            {
                // defined, and it's length is not same as e1's
                entity.blockades = area.getBlockades().stream().map(EntityID::getValue).collect(Collectors.toList());
            }
            if (area.isXDefined() && area.isYDefined()
                    && (!Objects.equals(e1.x, area.getX()) || !Objects.equals(e1.y, area.getY())))
            {
                // defined, and it's not same as e1's
                entity.x = area.getX();
                entity.y = area.getY();
            }
        } // Area
        else if (e2 instanceof Blockade)
        {
            // e2 is Blockade
            Blockade blockade = (Blockade)e2;
            if (blockade.isRepairCostDefined() && !Objects.equals(e1.repairCost, blockade.getRepairCost()))
            {
                // defined, and e1 has not same value
                entity.repairCost = blockade.getRepairCost();
            }
            if (blockade.isApexesDefined() &&
                    (e1.blockades == null) ||
                    (blockade.getApexes().length / 2) != e1.blockades.size())
            {
                // defined, and
                    // 1. e1's not defined, or
                    // 2. data length is not same as e1's
                int[] apexes = blockade.getApexes();
                entity.apexes = new ArrayList<>();
                for(int i=0; i<apexes.length; i+=2) {
                    // data placed as [x1, y1, x2, y2, ...]
                    entity.apexes.add(new Point(apexes[i], apexes[i+1]));
                }
            }
            if (blockade.isXDefined() && blockade.isYDefined()
                    && (!Objects.equals(e1.x, blockade.getX()) || !Objects.equals(e1.y, blockade.getY())))
            {
                // defined, and e1 has not same value;
                entity.x = blockade.getX();
                entity.y = blockade.getY();
            }
        } // Blockade
        else {
            Logger logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
            if (logger != null) {
                logger.warn("Unknown Type: " + e2.getURN());
            }
        }

        return entity;
    }


}
