package viewermanager.manager;

/**
 * hold Viewer's state
 * Viewerの状態を保持
 * */
public class ViewerState {

    /**
     * viewer's id
     * ViewerのID
     * */
    protected int viewerId;

    /**
     * timeStamp of record last send
     * 最後に送ったレコードのステップ番号
     * */
    protected int timeStep;

    /**
     * timeStamp of last request
     * 最後に要求があったタイムスタンプ
     * */
    protected long timeStamp;

    /**
     * provider id data recording
     * 現在紐付けられているProviderのID
     * */
    protected int providerId;


    /**
     * viewer status in server
     * Viewerがサーバ上でどのような扱いなのかを示す
     * */
    protected String status;

    /// Not Working. 仕事していない
    public static final String STATUS_STOP = "Stop";
    /// Connected, but not send any records. 接続済み，しかしレコードは送っていない
    public static final String STATUS_CONNECTED = "Connected";
    /// Connected, and sending records. 接続済みでレコードを送信している最中
    public static final String STATUS_SENDING = "Sending";
    /// Connected, and finished sending all records. 接続済みで，すべてのレコードを送信し終えた
    private static final String STATUS_FINISHED = "Finished";
    /// Disconnected. どのProviderにも接続していない
    public static final String STATUS_DISCONNECTED = "Disconnected";
    /// Connected, so after re-connected. 接続済みで，再接続された直後
    public static final String STATUS_RECONNECTED = "Reconnected";
    /// Timed out. タイムアウトしている
    public static final String STATUS_TIMEOUT = "Timeout";
    /// any error occurred. 何らかのエラーが発生した
    public static final String STATUS_ERROR = "Error";


    public ViewerState(int viewerId) {
        this.viewerId = viewerId;
        this.timeStep = 0;
        this.providerId = -1;
        status = STATUS_STOP;
    }

    /// Getter / Setter
    public int getViewerId() {
        return viewerId;
    }

    public int getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(int time) {
        this.timeStep = time;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getProviderId() {
        return providerId;
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
    }

    /**
     * Connect to Provider
     * Providerへの接続をおこなう
     *
     * @param providerId provider's id want to make connection. 接続したいProviderのID．
     * */
    public void connectProvider(int providerId) {
        setProviderId(providerId);
        setTimeStep(0);
        setTimeStamp(System.currentTimeMillis());
        this.status = STATUS_CONNECTED;
    }

    /**
     * Disconnect from Provider.
     * Providerから切断する．
     * */
    public void disconnectProvider() {
        this.status = STATUS_DISCONNECTED;
        this.providerId = -1;
    }

    /**
     * Notify Providing Finished.
     * Providerが提供を終了したと通知する．
     * */
    public void provideFinished() {
        this.status = STATUS_FINISHED;
    }

    /**
     * Set timed out.
     * タイムアウトに設定する．
     * */
    public void setTimedout() {
        this.status = STATUS_TIMEOUT;
    }

    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * Check is the connected to any provider.
     * 何らかのProviderに接続されているかをチェックする．
     * */
    public boolean isConnected() {
        return !(status.equals(STATUS_STOP) || status.equals(STATUS_DISCONNECTED));
    }
}
