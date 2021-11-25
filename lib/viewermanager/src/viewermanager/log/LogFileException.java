package viewermanager.log;


public class LogFileException extends Exception {

    public static String FILE_NOT_FOUND = "File not Found";
    public static String FILE_IS_NOT_LOG = "File is not Log File";
    public static String FAIL_READ_LOG = "Failed to Read Log File correctly";
    public static String FAIL_OPEN = "Failed to Open File";

    private String reason;
    public LogFileException (String reason, String msg) {
        super(msg);
        this.reason = reason;
    }

    public LogFileException (String reason, String msg, Throwable cause) {
        super(msg, cause);
        this.reason = reason;
    }


    public String getReason() {
        return reason;
    }

}
