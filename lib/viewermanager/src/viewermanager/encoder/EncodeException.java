package viewermanager.encoder;



public class EncodeException extends Exception {

    public EncodeException(Throwable e) {
        super(e);
    }

    public EncodeException(String msg) {
        super(msg);
    }

    public EncodeException(String msg, Throwable e) {
        super(msg, e);
    }
}
