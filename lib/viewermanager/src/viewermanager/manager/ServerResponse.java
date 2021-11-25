package viewermanager.manager;


/**
 * Manager's Response for the Server
 * ManagerのServerへの返答
 * */
public class ServerResponse {

    /**
     * result code of status
     * 結果を示すコード
     * */
    protected int status;

    /* Success Codes */
    /**
     * status code: completed with no error
     * status code: 問題なく終了した
     * */
    public static final int STATUS_OK       = 200;
    /**
     * status code: request procedure started in async
     * status code: 非同期で受け入れられた
     * */
    public static final int STATUS_ACCEPTED = 202;

    /* Failed Codes */
    /**
     * status code: requested parameters are bad for processing
     * status code: 指定されたパラメータでは要求を満たせない
     * */
    public static final int STATUS_BADREQ   = 400;
    /**
     * status code: requested parameters are not found in server
     * status code: 要求を満たすリソースが見つからない
     */
    public static final int STATUS_NOTFOUND = 404;
    /**
     * status code: request targets are conflicted
     * status code: 要求が衝突している
     * */
    public static final int STATUS_CONFLICT = 409;
    /**
     * status code: hey, I'm tea pod
     * status code: 私がティーポッドだ
     * */
    public static final int STATUS_TEAPOD   = 418;
    /**
     * status code: some error occurred on the manager
     * status code: Managerの内部エラー
     * */
    public static final int STATUS_INTERNAL_ERROR = 500;
    /**
     * status code: request target is not implemented
     * status code: 要求された対象はまだ実装されていない
     * */
    public static final int STATUS_NOT_IMPL = 501;

    /**
     * message to client
     * クライアントへのメッセージ
     * */
    protected String message;

    /**
     * packable & serializable data to send client back
     * クライアントへ送信する，直列化可能なデータ
     *
     * @implNote reference implementation, use reflection and get all fields.
     * */
    protected Object data;

    public ServerResponse() {
        this(0, null, null);
    }

    public ServerResponse(int status) {
        this(status, null, null);
    }

    public ServerResponse(int status, String message) {
        this(status, message, null);
    }

    public ServerResponse(int status, Object data) {
        this(status, null, data);
    }

    public ServerResponse(int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

}








