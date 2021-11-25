package viewermanager.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Perception {
    public Integer id;
    public List<Integer> updated;
    public List<Integer> deleted;
    public List<Action> heard;
}
