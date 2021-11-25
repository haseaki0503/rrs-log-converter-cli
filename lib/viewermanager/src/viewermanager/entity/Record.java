package viewermanager.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Record - Hold all information of changes between 1 steps
 * 1ステップの間に発生した変化をすべて保持する
 * */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Record {

    /**
     * time step of this record.
     * このレコードのtimeStep
     * */
    public Integer time;

    /**
     * this step's score
     * このステップのスコア
     * */
    public Double score;

    /**
     * map information (optional)
     * マップ情報(任意使用)
     * */
    public MapInfo map;

    /**
     * all information of current world (optional)
     * 現在のワールド全体の情報 (任意使用)
     * */
    public List<Entity> world;

    /**
     * all communication, and actions on this time step (may be not null)
     * この時間で発生した通信や行動などのすべて (おそらくnullでない)
     * */
    public List<Action> commands;

    /**
     * all changes of the world entity on this time step (may be not null)
     * この時間で発生したすべてのEntityの変化 (おそらくnullでない)
     * */
    public List<Entity> changes;

    /**
     * config (optional)
     * コンフィグ (任意使用)
     * */
    public Map<String, String> config;

    /**
     * perceptions (not used)
     * */
    public Map<Integer, Perception> perceptions;

    public Record() {
        time = null;
        score = null;
        map = null;
        world = null;
        commands = null;
        changes = null;
        config = null;
    }

    public Record copy() {
        Record rec = new Record();
        rec.time = this.time;
        rec.score = this.score;
        rec.map = this.map;
        rec.world = this.world;
        rec.commands = this.commands;
        rec.changes = this.changes;
        rec.config = this.config;
        return rec;
    }
}
