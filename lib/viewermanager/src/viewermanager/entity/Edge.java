package viewermanager.entity;


import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Edge Information
 *  has a start point and end point, and adjacent (neighbour entity's id).
 *
 * 辺の情報．
 *  始点と終点，辺を共有する他のエンティティのID(adjacent)を持つ
 * */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Edge {

    /**
     * start point of edge
     * */
    public Point start;

    /**
     * end point of edge
     * */
    public Point end;

    /**
     * adjacent id of entity that sharing this edge
     * */
    public Integer adjacent;

    public Edge() {
        start = null;
        end = null;
        adjacent = null;
    }

    public Edge(int x1, int y1, int x2, int y2) {
        this.start = new Point(x1, y1);
        this.end = new Point(x2, y2);
        this.adjacent = null;
    }

    public Edge(Point start, Point end) {
        this.start = start;
        this.end = end;
        this.adjacent = null;
    }

    /**
     * Create Edge from RRS Edge
     * */
    public Edge(rescuecore2.standard.entities.Edge edge)
    {
        this.start = new Point(edge.getStartX(), edge.getStartY());
        this.end = new Point(edge.getEndX(), edge.getEndY());
        this.adjacent = edge.isPassable() ? edge.getNeighbour().getValue() : null;
    }
}
