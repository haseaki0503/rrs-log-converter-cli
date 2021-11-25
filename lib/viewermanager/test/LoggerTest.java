import logger.JsonListAppender;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.junit.Test;
import viewermanager.ViewerManagerKeys;

public class LoggerTest {
    @Test
    public void LoggerTest() throws Exception {
        Logger logger = Logger.getLogger("main.MainLogger");

        logger.trace("testcase");
        logger.error("運用テスト");

        logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        logger.trace("test case");
        logger.error("this is error");

        Appender appender = logger.getAppender("list");
        if (appender instanceof logger.JsonListAppender) {
            JsonListAppender jsla = (JsonListAppender) appender;
            for (String str : jsla.getListLogs()) {
                System.out.println(str);
            }
        }
    }
}
