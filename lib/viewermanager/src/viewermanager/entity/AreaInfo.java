package viewermanager.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Map Entity Information
 *
 * <code>AreaInfo</code> holds static information of Entity inherits <code>Area</code> of RRS.
 * What type of Areas are distinguishable with <code>type</code> parameter.
 *
 * <code>AreaInfo</code>はRRSの<code>Area</code>から継承されたエンティティの静的な情報を保持します．
 * どのAreaの種類かは，<code>type</code>の属性で判断できます．
 *
 * */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AreaInfo {

    /**
     * Entity ID
     * */
    public Integer id;

    /**
     * Entity Type
     *  * Area
     *      - Road
     *          + Hydrant
     *      - Building
     *          + Gas Station
     *          + Ambulance Centre / Fire Station / Police Office
     *          + Refuge
     * */
    public String type;

    /**
     * coordinate of x
     * */
    public Integer x;

    /**
     * coordinate of y
     * */
    public Integer y;

    /**
     * edges of this area.
     * */
    public List<Edge> edges;

    /**
     * neighbours entity id.
     *  ^~~~> other entities sharing at least one of edges
     * */
    public List<Integer> neighbours;

    public AreaInfo() {
        this.x = null;
        this.y = null;
        this.edges = null;
        this.neighbours = null;
    }

    /**
     * Create Entity by RRS Area
     * */
    public AreaInfo(Area area) {
        this();

        if (area.isEdgesDefined()) {
            this.edges = new ArrayList<>();
            area.getEdges().stream()
                    .map(Edge::new)
                    .forEach(a -> edges.add(a));
        }
        if (area.isXDefined()) {
            this.x = area.getX();
        }
        if (area.isYDefined()) {
            this.y = area.getY();
        }
        if(area.getNeighbours().size() > 0) {
            this.neighbours = area.getNeighbours().stream().map(EntityID::getValue).collect(Collectors.toList());
        }

        if(area instanceof Road) {
            if (area instanceof Hydrant) this.type = EntityKey.Entity.HYDRANT;
            else this.type = EntityKey.Entity.ROAD;
        }
        else if(area instanceof Building) {
            if(area instanceof AmbulanceCentre) {
                this.type = EntityKey.Entity.CONTROL_AMBULANCE;
            }
            else if(area instanceof FireStation) {
                this.type = EntityKey.Entity.CONTROL_FIRE;
            }
            else if(area instanceof PoliceOffice) {
                this.type = EntityKey.Entity.CONTROL_POLICE;
            }
            else if(area instanceof GasStation) {
                this.type = EntityKey.Entity.GAS_STATION;
            }
            else if(area instanceof Refuge) {
                this.type = EntityKey.Entity.REFUGE;
            }
            else {
                this.type = EntityKey.Entity.BUILDING;
            }
        }else {
            this.type = EntityKey.UNKNOWN;
        }

        this.id = area.getID().getValue();
    }


}
