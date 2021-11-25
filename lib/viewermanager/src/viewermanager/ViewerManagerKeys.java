package viewermanager;


public class ViewerManagerKeys {
    public static final String LOGGER = "viewermanager.logger.main";
    public static final String DEFAULT_LOGGER = "viewer.main.MainLogger"; // Same Value on log4j.xml
    public static final String LOG_LEVEL = "viewermanager.logger.level";
    public static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String VIEWER_LOG_DIR = "viewer.manager.log.dir";
    public static final String VIEWER_LOG_DEFAULT_DIR = "logs";

    public static final String KERNEL_PORT = "rrs.viewer.default.port";
    public static final String KERNEL_DEFAULT_PORT = "7000";
    public static final String VIEWER_KERNEL_WAIT_TIME = "rrs.viewer.wait.ms";
    public static final String VIEWER_KERNEL_WAIT_DEFAULT_TIME = "1000";
    public static final String VIEWER_KERNEL_WAIT_COUNT = "rrs.viewer.wait.count";
    public static final String VIEWER_KERNEL_WAIT_DEFAULT_COUNT = "60";

    public static final String HTTP_PORT = "http.port";
    public static final String HTTP_DEFAULT_PORT = "8080";
    public static final String HTTP_STATIC = "http.path.static";
    public static final String HTTP_DEFAULT_STATIC = "/web/static";
    public static final String HTTP_WEB_HOME = "http.path.web";
    public static final String HTTP_DEFAULT_WEB_HOME = "/web";

    public static java.util.Map<String, String> getDefaultPairs() {
        java.util.HashMap<String, String> map = new java.util.HashMap<>();

        map.put(LOGGER, DEFAULT_LOGGER);
        map.put(LOG_LEVEL, DEFAULT_LOG_LEVEL);
        map.put(VIEWER_LOG_DIR, VIEWER_LOG_DEFAULT_DIR);
        map.put(KERNEL_PORT, KERNEL_DEFAULT_PORT);
        map.put(VIEWER_KERNEL_WAIT_TIME, VIEWER_KERNEL_WAIT_DEFAULT_TIME);
        map.put(VIEWER_KERNEL_WAIT_COUNT, VIEWER_KERNEL_WAIT_DEFAULT_COUNT);
        map.put(HTTP_PORT, HTTP_DEFAULT_PORT);
        map.put(HTTP_STATIC, HTTP_DEFAULT_STATIC);
        map.put(HTTP_WEB_HOME, HTTP_DEFAULT_WEB_HOME);

        return map;
    }

    public static java.util.Properties getProperties() {
        java.util.Properties prop = new java.util.Properties();

        java.util.Map<String, String> map = getDefaultPairs();
        for (String key : map.keySet()) {
            prop.setProperty(key, map.get(key));
        }

        return prop;
    }
}
