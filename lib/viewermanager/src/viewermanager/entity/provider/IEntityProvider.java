package viewermanager.entity.provider;

import viewermanager.entity.MapInfo;
import viewermanager.entity.Record;

import java.util.Map;

/**
 * IEntityProvider - Interface of Entity Provider.
 * To make <code>Entity</code>, and provides it as <code>Record</code>.
 *
 * <code>Entity</code>を作り，<code>Record</code>として提供する．
 *
 * Reference Implements:
 *  - RRSEntityProvider : Connect to Kernel as Viewer and transform RRS records to Record
 *  - RRSLogProvider : Read RRS Log File and transform RRS records to Record
 *  - ViewerLogProvider : Read packed Record and provide as Record
 * */
public interface IEntityProvider {

    /**
     * Shutdown all system and release resource.
     * すべてを終了して資源を開放する．
     * */
    void shutdown();

    /**
     * Get Server's Status String.
     * サーバの状況を文字列として取得する．
     *
     * @return Defined as const String in <code>IEntityProvider</code>.
     * */
    String getStatus();

    /**
     * returned on <code>getStatus</code>. Not connected to any system.
     * <code>getStatus</code>で使われる． どのシステムにも接続していない．
     * */
    static final String PROVIDER_NOT_CONNECTED = "Not connected";

     /**
     * returned on <code>getStatus</code>. Connected to system, but not works.
     * <code>getStatus</code>で使われる． システムに接続されているが仕事はしていない．
     * */
    static final String PROVIDER_NOT_WORKING = "Connected not Processing";

    /**
     * returned on <code>getStatus</code>. Connected to system, and working.
     * <code>getStatus</code>で使われる． システムに接続されて仕事中．
     * */
    static final String PROVIDER_WORKING = "Processing";

    /**
     * returned on <code>getStatus</code>. Connected to system, and end of working.
     * <code>getStatus</code>で使われる． システムに接続されてすべての仕事が終了した．
     * */
    static final String PROVIDER_END_WORKING = "Entity providing ended";


    //--[Functions to get about Provider Information]------------------------------------

    /**
     * check is the incoming record.
     * 新しいレコードが存在するか確かめる．
     *
     * @return if any record queued, return true. other false. レコードがある場合はTrue．
     * */
    boolean isIncomingRecordAvailable();

    /**
     * get the incoming record.
     * 新しいレコードを取得する．
     *
     * @return new record.
     * @apiNote check is the record available with <code>isIncomingRecordAvailable</code> before call this.
     * この関数を呼び出す前に<code>isIncomingRecordAvailable</code>で新しいレコードがあるか確かめてください．
     * */
    Record getIncomingRecord();

    /**
     * get this provider's id
     * このProviderのIDを取得する．
     * */
    Integer getID();

    /**
     * set this provider's id
     * このProviderのIDを設定する．
     * */
    void setID(Integer id);

    /**
     * get this provider's identification string.
     * このProviderを示す一意な文字列を返す．
     * */
    String getProviderIDString();

    /**
     * check is this provider working with online, so Networking.
     * このProviderがネットワーク越しのようなオンライン状態で仕事するものかを示す．
     * */
    boolean isOnline();

    /**
     * get remote host name if this provider working with network.
     * ネットワーク越しに動いている場合，相手のホスト名を取得する．
     *
     * @return Remote Host Name if this provider working with networking, else must be <code>null</code>.
     * */
    String getRemoteHostName();

    /**
     * get remote host port number if this provider working with network.
     * ネットワーク越しに動いている場合，相手のポート番号を取得する．
     *
     * @return Remote Host port number if this provider working with networking, else must be <code>null</code>.
     * */
    Integer getRemotePort();


    /**
     * check is this provider connected to any system.
     * このProviderがシステムに接続されているかを示す．
     * */
    boolean isConnected();

    /**
     * check is this provider working -- connecting, reading, transforming, and providing.
     *  this means this provider 'is not finished.'
     * このProviderが仕事中(接続中，読み込み中，変換中，提供中)か確かめる．
     *  これはこのProviderが「終了していない」ことを意味する．
     * */
    boolean isWorking();

    /**
     * check is this provider end up of working -- so no data incoming in future.
     * このProviderがもう仕事しない(以降どのようなデータも提供しない)かを確かめる．
     * */
    boolean isClosed();

    //--[Functions to get about Entities]------------------------------------
    /**
     * Get the last record's time step.
     * 現在読み出した最後のレコードのタイムステップを取得する．
     * */
    Integer getCurrentTimeStep();

    /**
     * Get the max steps of connected system.
     * 接続したシステムから提供される最大タイムステップを取得する．
     * */
    Integer getMaxTimeSteps();

    /**
     * Get the Map information are given by the system.
     * 与えられたマップ情報を取得する．
     * */
    MapInfo getMap();

    /**
     * Get the configs are given by the system.
     * 与えられたコンフィグ情報を取得する．
     * */
    Map<String, String> getConfig();

}
