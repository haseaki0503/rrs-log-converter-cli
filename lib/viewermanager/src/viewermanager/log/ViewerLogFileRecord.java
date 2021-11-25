package viewermanager.log;


import com.fasterxml.jackson.annotation.JsonIgnore;
import viewermanager.entity.MapInfo;
import viewermanager.entity.Record;
import viewermanager.entity.provider.IEntityProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * ViewerManager's Record format for Output/Input from File
 * ファイルへの読み込み/書き出しのためのViewerManager用のRecord形式
 * */
public class ViewerLogFileRecord {

    /**
     * format version
     * 形式のバージョン
     *
     * @implNote currently 1
     * */
    public int version;

    /**
     * max timestep of the log
     * ログの最大ステップ数
     * */
    public int maxTimeStep;

    /**
     * names of map read from Config(mapdir)
     * コンフィグのmapdirから読み出したマップの名前
     * */
    public String mapName;

    /**
     * Map's information
     * mapの情報
     * */
    public MapInfo map;

    /**
     * configs
     * コンフィグ
     * */
    public Map<String, String> config;

    /**
     * (timestep and Record)'s pair of log data
     * ログのデータの(ステップ数とレコードの)ペア
     * */
    public Map<Integer, Record> log;

    public ViewerLogFileRecord() {
        version = 1;
        maxTimeStep = 0;
        mapName = null;
        map = null;
        config = null;
        log = null;
    }

    /**
     * create new <code>ViewerLogFileRecord</code> from EntityProvider's information.
     *  uses <code>maxTimeStep</code>, <code>map</code>, <code>config</code> of entityProvider.
     *
     * EntityProviderから新しい<code>ViewerLogFileRecord</code>を作る．
     *  entityProviderの<code>maxTimeStep</code>, <code>map</code>, <code>config</code>を使用する．
     * */
    public ViewerLogFileRecord(IEntityProvider entityProvider) {
        this();

        maxTimeStep = entityProvider.getMaxTimeSteps();
        map = entityProvider.getMap();
        config = entityProvider.getConfig();

        mapName = config.get("gis.map.dir");
        log = null;
    }

    public ViewerLogFileRecord(IEntityProvider entityProvider, Map<Integer, Record> log)
    {
        this(entityProvider);
        this.log = log;
    }

    /**
     * get log format version
     * ログ形式のバージョンを取得する
     * */
    public int getVersion() {
        return version;
    }

    /**
     * get max timeStep of log
     * ログデータの最大ステップ数を取得する
     * */
    public int getMaxTimeStep() {
        return maxTimeStep;
    }

    /**
     * get mapName
     * 地図名を取得する
     * */
    public String getMapName() {
        return mapName;
    }

    /**
     * get config
     * コンフィグを取得する
     * */
    public Map<String, String> getConfig()
    {
        return config;
    }

    /**
     * get listed record count
     * 保存されたレコードの数を取得する
     * */
    @JsonIgnore
    public int size() {
        return log.size();
    }

    /**
     * get record containing log-list
     * 保存されたレコードを取り出す
     *
     * @param i timeStep of record
     * */
    public Record get(int i)
    {
        return log.get(i);
    }

    /**
     * append <code>Record</code> to FileRecord
     * <code>Record</code>をFileRecordに追加する
     * */
    public void put(Record r) {
        if(r == null) return ;

        // Get Time
        Integer time = r.time;
        // Create new empty record to take data needed
        Record record = new Record();

        // select data
        record.time = r.time;
        record.score = r.score;
        record.commands = (r.commands != null && !r.commands.isEmpty()) ? r.commands : null;
        record.changes = (r.changes != null && !r.changes.isEmpty()) ? r.changes : null;
        record.perceptions = (r.perceptions != null && !r.perceptions.isEmpty()) ? r.perceptions : null;

        // (log list is empty) == (the first record)
        if(log == null)
        {
            log = new HashMap<>();
            record.world = r.world;
        }
        log.put(time, record);
    }
}



