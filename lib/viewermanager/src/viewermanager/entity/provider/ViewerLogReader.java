package viewermanager.entity.provider;


import viewermanager.entity.MapInfo;
import viewermanager.entity.Record;
import viewermanager.log.LogFileException;
import viewermanager.log.ViewerLogFile;
import viewermanager.log.ViewerLogFileRecord;

import java.util.Map;

/**
 * Viewer Log Reader - Reading ViewerManager's Log File
 * ViewerManager用のLogFileを読み出すためのProvider
 * */
public class ViewerLogReader extends ProviderLogReader{

    /**
     * Log File Name
     * ログフィアルのパス
     * */
    private String fileName;

    /**
     * Reader of ViewerManager Log File
     * ViewerManagerのログファイルの読み出し
     * */
    private ViewerLogFileRecord recorder;

    /**
     * time Steps of the latest Records
     * 最後に読み込んだレコードのステップ数
     * */
    private Integer currentTimeStep;

    /**
     * max time step of the Log File
     * ログの最大ステップ数
     * */
    private Integer maxTimeStep;

    /**
     * Entity Provider's Status
     * Providerの状態
     * */
    private String status;

    /**
     * the latest record
     * 最後に読み込んだレコード
     * */
    private Record record;

    public ViewerLogReader() {
        super();
        recorder = null;
        currentTimeStep = 0;
        maxTimeStep = null;
        status = IEntityProvider.PROVIDER_NOT_CONNECTED;
        record = null;
    }

    public ViewerLogReader(String filename) throws LogFileException {
        this();
        open(filename);
    }

    /**
     * open log file.
     * ログファイルを開く
     *
     * @exception LogFileException any error occurred.
     * */
    @Override
    public void open(String filename) throws LogFileException {
        // Read and unpack all Log Data
        this.recorder = ViewerLogFile.logRead(filename);

        // Save Information
        this.maxTimeStep = recorder.getMaxTimeStep();
        this.fileName = filename;

        this.status = IEntityProvider.PROVIDER_WORKING;
    }

    /**
     * Shutdown all system and release resource.
     * すべてを終了して資源を開放する．
     * */
    @Override
    public void shutdown()
    {
        recorder = null;
        status = IEntityProvider.PROVIDER_END_WORKING;
    }

    /**
     * get this provider's identification string.
     * このProviderを示す一意な文字列を返す．
     * */
    @Override
    public String getProviderIDString() {
        return fileName;
    }

    /**
     * check is this provider working with online, so Networking.
     * このProviderがネットワーク越しのようなオンライン状態で仕事するものかを示す．
     * */
    @Override
    public boolean isOnline() {
        return false;
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

    /**
     * Get the last record's time step.
     * 現在読み出した最後のレコードのタイムステップを取得する．
     * */
    @Override
    public Integer getCurrentTimeStep() {
        return currentTimeStep;
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
        return recorder.map;
    }

    /**
     * Get the configs are given by the system.
     * 与えられたコンフィグ情報を取得する．
     * */
    @Override
    public Map<String, String> getConfig() {
        return recorder.getConfig();
    }

    /**
     * check is this provider connected to any system.
     * このProviderがシステムに接続されているかを示す．
     *
     * Condition: fileName is not null, and status is not NOT_CONNECTED
     * */
    @Override
    public boolean isConnected() {
        return fileName != null && !status.equals(IEntityProvider.PROVIDER_NOT_CONNECTED);
    }

    /**
     * check is this provider working -- connecting, reading, transforming, and providing.
     *  this means this provider 'is not finished.'
     * このProviderが仕事中(接続中，読み込み中，変換中，提供中)か確かめる．
     *  これはこのProviderが「終了していない」ことを意味する．
     *
     * Condition: isConnected(), and status is WORKING
     * */
    @Override
    public boolean isWorking() {
        return isConnected() && status.equals(IEntityProvider.PROVIDER_WORKING);
    }

    /**
     * check is this provider end up of working -- so no data incoming in future.
     * このProviderがもう仕事しない(以降どのようなデータも提供しない)かを確かめる．
     *
     * Condition: (recorder is null, and status is END_WORKING) and no records available
     * */
    @Override
    public boolean isClosed() {
        return (recorder == null) && status.equals(IEntityProvider.PROVIDER_END_WORKING)
                && !isIncomingRecordAvailable();
    }

    /**
     * check is the incoming record.
     * 新しいレコードが存在するか確かめる．
     *
     * @return if any record queued, return true. other false. レコードがある場合はTrue．
     * */
    @Override
    public boolean isIncomingRecordAvailable() {
        Record record = this.record;

        // Is the recorder still working
        if (isWorking() && record == null) {
            // Check the record
            int time = currentTimeStep + 1;
            record = recorder.get(time);
            currentTimeStep = time;

            // Set to the record global
            this.record = record;

            // Check is the record's time
            if (currentTimeStep >= recorder.getMaxTimeStep()) {
                // the record is the end of records list
                status = IEntityProvider.PROVIDER_END_WORKING;
            }
            else if (record == null) {
                // record is not end of the schedule, but it's end suddenly
                status = IEntityProvider.PROVIDER_END_WORKING;
            }
        }

        // Return result: is the record available
        return (record != null);
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
        Record rec = record;
        this.record = null;
        return rec;
    }
}
