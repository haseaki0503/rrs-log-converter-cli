package viewermanager.entity.provider;

import viewermanager.log.LogFileException;

/**
 * Helper Class for EntityProvider Reading Log Files
 * ログファイルを読み込むためのEntity Providerのヘルパクラス
 * */
public abstract class ProviderLogReader implements IEntityProvider {

    /**
     * Provider's ID
     * ProviderのID
     * */
    protected Integer id;

    public ProviderLogReader() {
        id = null;
    }

    /**
     * open log file.
     * ログファイルを開く
     *
     * @exception LogFileException any error occurred.
     * */
    public abstract void open(String filename) throws LogFileException;

    /**
     * get this provider's id
     * このProviderのIDを取得する．
     * */
    @Override
    public Integer getID() {
        return id;
    }

    /**
     * set this provider's id
     * このProviderのIDを設定する．
     * */
    @Override
    public void setID(Integer id) {
        this.id = id;
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
     * get remote host name if this provider working with network.
     * ネットワーク越しに動いている場合，相手のホスト名を取得する．
     *
     * @return Remote Host Name if this provider working with networking, else must be <code>null</code>.
     * */
    @Override
    public String getRemoteHostName() {
        return null;
    }

    /**
     * get remote host port number if this provider working with network.
     * ネットワーク越しに動いている場合，相手のポート番号を取得する．
     *
     * @return Remote Host port number if this provider working with networking, else must be <code>null</code>.
     * */
    @Override
    public Integer getRemotePort() {
        return null;
    }

    /**
     * check is this provider connected to any system.
     * このProviderがシステムに接続されているかを示す．
     * */
    @Override
    public boolean isConnected() {
        return false;
    }
}
