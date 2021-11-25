package viewermanager.entity.provider;

import org.apache.log4j.Logger;
import rescuecore2.Timestep;
import rescuecore2.config.Config;
import rescuecore2.log.FileLogReader;
import rescuecore2.log.LogException;
import rescuecore2.log.LogReader;
import rescuecore2.misc.java.LoadableTypeProcessor;
import rescuecore2.registry.Registry;
import rescuecore2.score.ScoreFunction;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.score.LegacyScoreFunction;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.WorldModel;
import sun.rmi.runtime.Log;
import viewermanager.ViewerManagerKeys;
import viewermanager.entity.*;
import viewermanager.log.LogFileException;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Log File Reader and Provider for RRS Log
 * RRS Logのためのログファイル読み出しProvider
 * */
public class RRSLogReader extends ProviderLogReader {
    /**
     * log File's path
     * ログファイルのパス
     * */
    private String logFileName;

    /**
     * time step on the last record
     * 最後のレコードでのTimeStep
     * */
    private int currentTime;

    /**
     * max time step on the log file
     * ログファイルに含まれる最大ステップ数
     * */
    private int maxTimeStep;

    /**
     * Map Information on Log File
     * ログが示す地図情報
     * */
    private MapInfo mapInfo;

    /**
     * config on the Log
     * ログが示すコンフィグ
     * */
    private Map<String, String> config;

    /**
     * the last record read.
     * 最後に読み出したレコード
     * */
    protected Record currentRecord;

    /**
     * the WorldModel on the last record reading.
     * 最後のレコードを読み出したときのWorldModel
     * */
    protected Map<Integer, Entity> worldModelRead;

    /**
     * Log Reader for RRS Log File.
     * RRS Logのファイルを読み出すためのReader
     * */
    protected LogReader reader;

    /**
     * Object for calculate RRS Score.
     * RRSのスコアを計算するためのオブジェクト
     * */
    protected ScoreFunction scoreFunction;

    /**
     * current status; IEntityProvider
     * */
    String status;

    public RRSLogReader()
    {
        this.reader = null;
        this.currentTime = 0;
        this.maxTimeStep = 0;
        this.mapInfo = null;
        this.config = null;
        this.currentRecord = null;
        this.worldModelRead = null;
        this.logFileName = null;
        this.scoreFunction = null;
        this.status = IEntityProvider.PROVIDER_NOT_CONNECTED;
    }

    public RRSLogReader(String logFileName) throws LogFileException
    {
        this();
        open(logFileName);
    }


    //-------------------------------------------------------------------------
    /**
     * get this provider's identification string.
     * このProviderを示す一意な文字列を返す．
     * */
    @Override
    public String getProviderIDString() {
        return getLogFileName();
    }

    /**
     * Get Server's Status String.
     * サーバの状況を文字列として取得する．
     *
     * @return Defined as const String in <code>IEntityProvider</code>.
     * */
    @Override
    public String getStatus() {
        return status;
    }


    //-------------------------------------------------------------------------
    /**
     * open log file.
     * ログファイルを開く
     *
     * @exception LogFileException any error occurred.
     * */
    @Override
    public void open(String fileName) throws LogFileException {
        Config config = new Config();
        try {
            /**
             * Read RRS's Jar Files and collect register entry used in rescuecore2;
             * RRSのJarファイルを読み込み，rescuecore2で使われる情報を収集する
             * */
            config.setValue("loadabletypes.inspect.dir", "library/rescue");
            LoadableTypeProcessor pr = new LoadableTypeProcessor(config);
            pr.addFactoryRegisterCallbacks(Registry.SYSTEM_REGISTRY);
            pr.process();

            // Open Log Reader
            reader = new FileLogReader(fileName, Registry.SYSTEM_REGISTRY);

            // Read Initial Data
            //// Config
            this.config = new HashMap<>();
            Config logConfig = reader.getConfig();
            logConfig.getAllKeys().forEach(a -> {
                this.config.put(a, logConfig.getValue(a));
            });

            //// MaxTimeStep
            this.maxTimeStep = reader.getMaxTimestep();

            //// Map
            WorldModel<? extends StandardEntity> model
                = StandardWorldModel.createStandardWorldModel(reader.getWorldModel(0));
            this.mapInfo = new MapInfo();
            Rectangle2D bounds = ((StandardWorldModel) model).getBounds();
            if(bounds != null) {
                this.mapInfo.width = bounds.getWidth();
                this.mapInfo.height = bounds.getHeight();
            }
            this.mapInfo.entities = new ArrayList<>();
            model.getAllEntities().forEach(a -> {
                if(a instanceof Area) {
                    // If entity is Area
                    this.mapInfo.entities.add(new AreaInfo((Area)a));
                }
            });

            //// Score Function
            String scoreFunctionName = "LegacyScoreFunction";
            scoreFunction = new LegacyScoreFunction();
            if(scoreFunction != null) {
                scoreFunction.initialise(model, logConfig);
            }

            //// Initial All Information
            this.currentTime = 0;
            this.currentRecord = null;
            this.logFileName = fileName;
            this.status = IEntityProvider.PROVIDER_WORKING;
        } catch (IOException e) {
            // Maybe File Not Found
            Logger logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
            if (Objects.isNull(logger)) {
                logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
            }

            logger.warn("RRSLogReader - Cannot Open File", e);
            throw new LogFileException(LogFileException.FAIL_OPEN, e.getMessage(), e);
        } catch (LogException e) {
            // Maybe File is not RRS Log
            Logger logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
            if (Objects.isNull(logger)) {
                logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
            }
            logger.warn("RRSLogReader - Cannot Read Log", e);
            throw new LogFileException(LogFileException.FAIL_READ_LOG, e.getMessage(), e);
        }
    }

    /**
     * Get Log's FileName.
     * ログファイルの名前を返す
     * */
    public String getLogFileName() {
        return logFileName;
    }

    /**
     * read log record from file.
     * ログファイルからレコードを読み出す．
     *
     * @param time timeStep want to read. 読み出したいステップ数
     * */
    protected Record readLog(int time) {
        // If requested time is over the maxTimeStep
        if(time > maxTimeStep) {
            return null;
        }

        // Read once
        Record record = new Record();
        try {
            // Read World
            HashMap<Integer, Entity> world = new HashMap<>();
            //// Get WorldModel
            WorldModel<? extends StandardEntity> model
                 = StandardWorldModel.createStandardWorldModel(reader.getWorldModel(time));
            //// Convert all Entity and put to the world.
            model.getAllEntities().stream().forEach(a -> {
                world.put(a.getID().getValue(), new Entity(a));
            });

            // Set to Record
            record.world = new ArrayList<>(world.values());

            // Read Changes
            record.changes = new ArrayList<>();
            if(worldModelRead != null) {
                //// Get ChangeSet (Updates)
                ChangeSet changeSet = reader.getUpdates(time).getChangeSet();

                //// Convert all Updates to difference between previous entity.
                //// すべての更新されたEntityの差分を取って更新を保持する
                changeSet.getChangedEntities().forEach(a -> {
                    Entity eold = worldModelRead.get(a.getValue());
                    Entity entity = Entity.difference(eold, model.getEntity(a));

                    //// entity differences are not null (any difference available)
                    //// 何らかの差分が存在するため，追加する．
                    if (entity != null && !entity.isEmpty()) {
                        if (eold == null) entity.created = true;
                        record.changes.add(entity);
                    }
                });

                //// append deleted entities for record
                //// 削除済みEntityを保存する
                changeSet.getDeletedEntities().forEach(a -> {
                    Entity edel = new Entity();
                    edel.id = a.getValue();
                    if(worldModelRead.containsKey(edel.id)) {
                        edel.type = worldModelRead.get(edel.id).type;
                    }

                    edel.deleted = true;
                    record.changes.add(edel);
                });
            }

            // Read Command and convert all
            record.commands
                    = reader.getCommands(time).getCommands()
                    .stream().map(Action::new).collect(Collectors.toList());

            // Calc Score
            if(scoreFunction != null) {
                record.score = scoreFunction.score(model, new Timestep(time));
            }

            // Store Current State
            record.time = time;
            this.currentTime = time;
            worldModelRead = world;
        } catch (LogException e) {
            // File Read Error
            Logger logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
            if (Objects.isNull(logger)) {
                logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
            }
            logger.warn("RRSLogReader - Cannot Read Log", e);
            return null;
        }

        // Save Current Record
        currentRecord = record;

        return record;
    }

    //---[Impelement: IEntityProvider]------------------------------------------
    /**
     * Shutdown all system and release resource.
     * すべてを終了して資源を開放する．
     * */
    @Override
    public void shutdown() {
        reader = null;
    }

    /**
     * Get the last record's time step.
     * 現在読み出した最後のレコードのタイムステップを取得する．
     * */
    @Override
    public Integer getCurrentTimeStep() {
        return currentTime;
    }

    /**
     * Get the max steps of connected system.
     * 接続したシステムから提供される最大タイムステップを取得する．
     * */
    @Override
    public Integer getMaxTimeSteps() {
        return maxTimeStep;
    }

    /**
     * Get the Map information are given by the system.
     * 与えられたマップ情報を取得する．
     * */
    @Override
    public MapInfo getMap() {
        return mapInfo;
    }

    /**
     * Get the configs are given by the system.
     * 与えられたコンフィグ情報を取得する．
     * */
    @Override
    public Map<String, String> getConfig() {
        return config;
    }

    /**
     * check is this provider connected to any system.
     * このProviderがシステムに接続されているかを示す．
     *
     * Condition: reader is not null, and status is WORKING
     * */
    @Override
    public boolean isConnected() {
        return reader != null && status.equals(IEntityProvider.PROVIDER_WORKING);
    }

    /**
     * check is this provider working -- connecting, reading, transforming, and providing.
     *  this means this provider 'is not finished.'
     * このProviderが仕事中(接続中，読み込み中，変換中，提供中)か確かめる．
     *  これはこのProviderが「終了していない」ことを意味する．
     *
     *  Condition: reader is not null, and status is WORKING
     * */
    @Override
    public boolean isWorking() {
        return (reader != null) && status.equals(IEntityProvider.PROVIDER_WORKING);
    }

    /**
     * check is this provider end up of working -- so no data incoming in future.
     * このProviderがもう仕事しない(以降どのようなデータも提供しない)かを確かめる．
     *
     * Condition: (reader is null, or timeStep > maxTimeStep(ends up)) and not record available on queue.
     * */
    @Override
    public boolean isClosed() {
        return ((reader == null) || (currentTime > maxTimeStep)) && !isIncomingRecordAvailable();
    }

    /**
     * get the incoming record.
     * 新しいレコードを取得する．
     *
     * @return new record.
     * @apiNote check is the record available with <code>isIncomingRecordAvailable</code> before call this.
     * この関数を呼び出す前に<code>isIncomingRecordAvailable</code>で新しいレコードがあるか確かめてください．
     * */
    @Override
    public boolean isIncomingRecordAvailable() {
        if(currentTime < maxTimeStep) {
            // Read next Record
            Record record = readLog(this.currentTime+1);
            if(record != null) {
                this.currentRecord = record;

                if (currentTime >= maxTimeStep) {
                    this.status = IEntityProvider.PROVIDER_END_WORKING;
                }

                return true;
            }
        }

        // over maxTimeStep or cannot read Log;
        return false;
    }

    /**
     * get the incoming record.
     * 新しいレコードを取得する．
     *
     * @return new record.
     * @apiNote check is the record available with <code>isIncomingRecordAvailable</code> before call this.
     * この関数を呼び出す前に<code>isIncomingRecordAvailable</code>で新しいレコードがあるか確かめてください．
     * */
    @Override
    public Record getIncomingRecord() {
        return currentRecord;
    }


}


