package logger;


import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * json format logging with Log4j
 * log4jを用いた，JSON形式のロガー
 * */
public class JsonListAppender extends AppenderSkeleton {

    /**
     * escape string for json
     * jsonの形式に文字列をエスケープする
     * */
    private static String escape(Object str) {
        return str.toString().replace("\"", "\\\"");
    }

    /**
     * default capacity of the log list
     * 保持するログの初期数
     * */
    private static final int DEFAULT_CAPACITY = 100000;

    /**
     * log list
     * 保持しているログ
     * */
    protected List<String> logs;

    /**
     * capacity of the log list
     * 保持するログの個数
     * */
    private int capacity = DEFAULT_CAPACITY; // if (capacity < 1), infinity

    /**
     * format functions of Log-Event of log4j to the JSON element string
     * log4jのログ情報をJSONの様相形式に変換する関数群
     * */
    private static final Map<String, Function<LoggingEvent, String>> FORMATS
            = new HashMap<String, Function<LoggingEvent, String>>()
    {{
        /* timeStamp: what time the event occurred in UNIX TIME */
        put("timestamp", (LoggingEvent event) -> String.format("\"timestamp\": %d", event.getTimeStamp()));
        /* level: log level like a DEBUG,INFO,ERROR */
        put("level", (LoggingEvent event) -> String.format("\"level\": \"%s\"", event.getLevel()));
        /* message: the message of log */
        put("message", (LoggingEvent event) -> String.format("\"message\": \"%s\"", escape(event.getMessage())));
        /* exception: exception info, exception className and Messages as JSON Object when exception given */
        put("exception", (LoggingEvent event) -> {
            if (event.getThrowableInformation() != null) {
                return escape(String.format("\"exception\": {\"name\": \"%s\", \"message\": \"%s\"}"
                        , event.getThrowableInformation().getThrowable().getClass().getName()
                        , event.getThrowableInformation().getThrowable().getMessage()));
            }
            return ""; });

    }};
    /**
     * what fields (timeStamp, level, message, exception) use for log
     * どのフィールド(timeStamp, level, message, exception)をログとして使うか
     * */
    private List<String> fields;

    public JsonListAppender() {
        super();
        logs = new ArrayList<>();
        fields = new ArrayList<>();
    }

    public JsonListAppender(final Layout layout)
    {
        super();
        setLayout(layout);
        activateOptions();
    }

    /**
     * get all logs held
     * すべてのログを取得する
     * */
    public List<String> getListLogs() {
        return logs;
    }

    /**
     * set capacity of log
     * ログの最大保持個数を設定する
     *
     * @apiNote this method is used in log4j.xml
     * */
    public void setCapacity(String capacity)
    {
        try {
            this.capacity = Integer.parseInt(capacity);
        }
        catch (NumberFormatException ex) {
            ex.printStackTrace();
            this.capacity = DEFAULT_CAPACITY;
        }
    }

    /**
     * set what field to use for log.
     *  multiple fields separated with comma(',').
     * ログとして使うフィールドを指定する
     *  複数のフィールドはコンマ(',')によって区切る
     *
     * @apiNote this method is used in log4j.xml. On multiple calls, fields added for the lists.
     * */
    public void setField(String field) {
        String[] labels = field.split(",");
        for (String lab : labels) {
            fields.add(lab.trim().toLowerCase());
        }
    }

    @Override
    public void activateOptions() {
        super.activateOptions();
    }

    /**
     * append the new log for the list
     * 新しいログを追加する
     * */
    @Override
    protected void append(LoggingEvent loggingEvent) {
        // fields to save
        List<String> fields = new ArrayList<>();
        String content;
        for (String field : this.fields) {
            // convert loggingEvent for JSON Element with used field information
            if (FORMATS.containsKey(field)) {
                content = FORMATS.get(field).apply(loggingEvent);
                if (!content.isEmpty())
                    fields.add(content);
            }
        }
        // format as a JSON
        logs.add("{" + String.join(", ", fields) + "}");

        // size trimming
        while (capacity > 0 && logs.size() > capacity) {
            logs.remove(0);
        }
    }

    @Override
    public void close() {
        logs.clear();
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * get al logs when the <code>logger</code> has <code>JsonListAppender</code>
     *
     * @return if the logger has a JsonListAppender, non null. otherwise must be null.
     * */
    public static String[] getLogs(Logger logger)
    {
        if (logger != null) {
            // specified this logger name written in log4j.xml
            Appender appender = logger.getAppender("list");
            if (appender instanceof JsonListAppender)
            {
                JsonListAppender jsonAppender = (JsonListAppender) appender;
                return jsonAppender.getListLogs().toArray(new String[0]);
            }
        }
        return null;
    }
}
