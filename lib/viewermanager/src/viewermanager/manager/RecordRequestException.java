package viewermanager.manager;

public class RecordRequestException extends Exception {

    protected int reason;

    public static final int Unknown_Viewer = 1;
    public static final int Records_Not_Availale = 2;
    public static final int Records_Not_Found = 3;
    public static final int In_Future = 4;

    public RecordRequestException(int reason) {
        super();
        this.reason = reason;
    }

    public RecordRequestException(String msg) {
        super(msg);
    }

    public RecordRequestException(Throwable ex) {
        super(ex);
    }

    public RecordRequestException(String msg, Throwable ex) {
        super(msg, ex);
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }

}
