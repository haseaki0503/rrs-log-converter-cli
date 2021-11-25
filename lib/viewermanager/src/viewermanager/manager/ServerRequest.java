package viewermanager.manager;

/**
 * Records Request for the Manager from Server (Client)
 * サーバ(クライアント)からManagerへのレコードの要求
 * */
public class ServerRequest {

    /**
     * client viewer's id
     * クライアントのViewerのID
     * */
    protected Integer viewerId;

    /**
     * timeStamp when the Server get this request
     * いつサーバがリクエストを受け取ったかを示すタイムスタンプ’
     * */
    protected Long timeStamp;

    /**
     * count of Records wanted
     * 要求されているレコードの数
     * */
    protected Integer count;

    /**
     * timeStep of the top of requested records
     * 要求されている先頭のレコードのステップ番号
     *
     * Ex)
     *  timeStep = 5, and count = 2: want to Records 5, and 6 (count 2)
     * */
    protected Integer time;

    /**
     * create Request empty
     * */
    public ServerRequest() {
        viewerId = null;
        timeStamp = null;
        count = null;
    }

    /**
     * create Request with viewer's id, arrived timeStamp
     * ViewerのIDと要求を受け取った時間でリクエストの作成をする
     *
     * @param viewerId viewer's id
     * @param timeStamp timestamp request arrived to server
     * */
    public ServerRequest(int viewerId, long timeStamp) {
        this();
        this.viewerId = viewerId;
        this.timeStamp = timeStamp;
    }

    /**
     * create Request with viewer's id, arrived timeStamp, requested record count
     * ViewerのIDと要求を受け取った時間，要求されているレコード数でリクエストの作成
     *
     * @param viewerId viewer's id
     * @param timeStamp timestamp request arrived to server
     * @param count requested record count of maximum
     * */
    public ServerRequest(int viewerId, long timeStamp, int count) {
        this(viewerId, timeStamp);
        this.count = count;
    }

    /**
     * create Request with viewer's id, arrived timeStamp, record requested time from, record count
     * ViewerのIDと要求を受け取った時間，要求されているレコード番号と数でリクエストを作成
     *
     * @param viewerId viewer's id
     * @param timeStamp timestamp request arrived to server
     * @param time  viewer's requested timeStep of record from
     * @param count requested record count of maximum
     * */
    public ServerRequest(int viewerId, long timeStamp, int time, int count) {
        this(viewerId, timeStamp, count);
        this.time = time;
    }

    /**
     * get viewer's id
     * */
    public Integer getViewerId() {
        return viewerId;
    }

    /**
     * get timestamp request arrived
     * */
    public Long getTimeStamp() {
        return timeStamp;
    }

    /**
     * get requested record count
     * */
    public Integer getCount() {
        return count;
    }

    /**
     * get requested record time from
     * */
    public Integer getTime() {
        return time;
    }

}
