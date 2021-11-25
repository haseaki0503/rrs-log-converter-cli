package viewermanager.entity;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Map Information
 * */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MapInfo {

    /**
     * width of map (by WorldModel)
     * */
    public double width;

    /**
     * height of map (by WorldModel)
     * */
    public double height;

    /**
     * List of AreaInfo : Map Entity Information List
     * */
    public List<AreaInfo> entities;

    @Override
    public String toString()
    {
        return String.format("{width: %s, height: %s, entities: %s}", width, height, (entities != null) ? "@" + entities.size() : "null");
    }
}
