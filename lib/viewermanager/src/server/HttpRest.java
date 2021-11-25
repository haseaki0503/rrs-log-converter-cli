package server;


import logger.JsonListAppender;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import server.rest.DefaultHandler;
import server.rest.RecordHandler;
import server.rest.RestHandlers;
import viewermanager.ViewerManagerKeys;
import viewermanager.manager.DefaultResponse;
import viewermanager.manager.Manager;
import viewermanager.manager.ServerResponse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Http Rest like Server
 * Http Rest風サーバ
 * */
public class HttpRest implements Server {

    /**
     * manager to control
     * 接続対象のManager
     * */
    protected Manager manager;

    /**
    * http server handle
     * HttpServerのインスタンス
    * */
    protected HttpServer server;

    /**
     * server shutdown Future
     * サーバ終了のFuture
     * */
    protected GrizzlyFuture<HttpServer> shutdown;

    Logger logger;
    Map<String, DefaultHandler> handles;

    /**
     * Constructor of Server, so settings up the interfaces.
     * サーバのコンストラクタ: Webインターフェイスの設定をおこなう
     * */
    public HttpRest(Manager manager) {
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (logger == null) {
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }

        this.manager = manager;

        // Create Server
        this.server = new HttpServer();

        // Get Port Number of accept port
        int port = 8080;
        try {
            port = Integer.parseInt(System.getProperty(ViewerManagerKeys.HTTP_PORT, ViewerManagerKeys.HTTP_DEFAULT_PORT));
        } catch (NumberFormatException ex) {
            logger.error("/httprest/open: cannot parse port", ex);
            port = 8080; /* default: 8080 */
        }

        // Create Network Listener to Routing
        NetworkListener nl = new NetworkListener("grizzly", "0.0.0.0", port);
        server.addListener(nl);

        // List up the Route of Interfaces
        handles = new HashMap<>();

        //// functions for server management
        handles.put("/server/shutdown",
                new DefaultHandler(Method.POST, manager::requestCloseServer));
        handles.put("/server/interfaces",
                new DefaultHandler(Method.GET, this::getInterfaces));
        handles.put("/server/errors",
                new DefaultHandler(Method.GET, ErrorStatus.class, this::getErrors));


        //// functions for provider
        handles.put("/provider/open",
                new RestHandlers.OpenProviderHandler(manager));
        handles.put("/provider/close",
                new RestHandlers.CloseProviderHandler(manager));
        handles.put("/provider/log/open",
                new RestHandlers.OpenLogHandler(manager));
        handles.put("/provider/log/list",
                new DefaultHandler(Method.GET, manager::requestLogList));
        handles.put("/provider/log/path/add",
                new RestHandlers.AddLogPathHandler(manager));
        handles.put("/provider/log/path",
                new DefaultHandler(Method.GET, manager::requestLogPaths));
        handles.put("/provider/list",
                new DefaultHandler(Method.GET, manager::requestProviderList));


        //// functions for viewer
        handles.put("/viewer/open",
                new RestHandlers.ViewerOpenHandler(manager));
        handles.put("/viewer/connect",
                new RestHandlers.ConnectionHandler(manager));
        handles.put("/viewer/disconnect",
                new RestHandlers.DisconnectionHandler(manager));
        handles.put("/viewer/record", //TODO: fix cannot finish communication correctly
                new RecordHandler(manager));
        handles.put("/viewer/list",
                new DefaultHandler(Method.GET, manager::requestViewerList));


        /// Register interface to server
        for (String uri : handles.keySet()) {
            server.getServerConfiguration().addHttpHandler(handles.get(uri), uri);
        }

        // Register static interfaces
        {
            // Read Path
            String pwd = new File(".").getAbsoluteFile().getParent();
            String static_path = System.getProperty(ViewerManagerKeys.HTTP_STATIC, ViewerManagerKeys.HTTP_DEFAULT_STATIC);
            String static_web = System.getProperty(ViewerManagerKeys.HTTP_WEB_HOME, ViewerManagerKeys.HTTP_DEFAULT_WEB_HOME);

            while (static_path.startsWith("/")) {
                static_path = static_path.substring(1);
            }
            while (static_web.startsWith("/")) {
                static_web = static_web.substring(1);
            }

            logger.info("/http/path: current path " + pwd);
            logger.info("/http/path: 'static' files in... " + static_path);
            logger.info("/http/path: web root in... " + static_web);
            // Set Path
            server.getServerConfiguration()
                    .addHttpHandler(new StaticHttpHandler(pwd, static_path), "/static");
            server.getServerConfiguration()
                    .addHttpHandler(new StaticHttpHandler(pwd, static_web),  "/");

            // for DEBUGGING: turn off this flag, Page File reload each request.
            server.getListener("grizzly").getFileCache().setEnabled(false);
        }
    }

    //*************************************************************************
    // Interface
    //*************************************************************************
    @Override
    public void start() throws IOException {
        // Start the server
        if (this.server != null) {
            this.server.start();

            int port = server.getListener("grizzly").getPort();
            logger.info("/http/start: Http Server Started on port " + port);
        }
    }

    @Override
    public void shutdown() {
        // Stop the server, and release these resources
        if (this.server != null) {
            shutdown = this.server.shutdown();
            this.server = null;
            this.manager = null;
            this.handles = null;
        }
    }

    @Override
    public boolean isClosed() {
        // Condition: `server` is null ('cos `server` is not null on Construct, and be null when Releasing)
        // and `shutdown` is not null ('cos `shutdown` will be not null then releasing)
        // and it well done.
        return server == null && shutdown != null && shutdown.isDone();
    }

    //*************************************************************************
    // Server Management Methods
    //*************************************************************************
    /**
     * List up the registered server routes;
     * サーバに登録された経路をすべて列挙する
     * */
    private ServerResponse getInterfaces() {
        List<String> interfaces = new ArrayList<>();

        for (String uri : handles.keySet())
        {
            DefaultHandler handler = handles.get(uri);
            String params = parseClazz(handler.getParameterClazz());
            String form = String.format("{\"uri\": \"%s\", \"method\": \"%s\", \"parameters\": %s}",
                    uri, handler.getMethod(), params);
            interfaces.add(form);
        }

        String message = "[" + String.join(", ", interfaces) + "]";

        return new ServerResponse(ServerResponse.STATUS_OK, ByteBuffer.wrap(message.getBytes()));
    }

    /**
     * Get Error List from JsonListAppender on Logger that mainLogger of ViewerManager.
     * ViewerManagerの主ロガーであるJsonListAppenderからエラーのリストを取得する．
     * */
    private ServerResponse getErrors(Object status) {
        if (status != null && status instanceof ErrorStatus) {
            ErrorStatus state = (ErrorStatus) status;

            // Get Errors
            String[] results = JsonListAppender.getLogs(logger);
            int to = results.length;
            int from = (state.count != null) ? to - state.count : 0;
            if (from < 0) from = 0;
            String[] res = Arrays.copyOfRange(results, from, to);

            return new ServerResponse(ServerResponse.STATUS_OK, new DefaultResponse.ListResult(res));
        }

        return new ServerResponse(ServerResponse.STATUS_BADREQ);
    }

    //*************************************************************************
    // Inner Helper Classes
    //*************************************************************************
    public static class ErrorStatus {
        public Integer count;
    }

    //*************************************************************************
    // Helper Methods
    //*************************************************************************
    /**
     * get Classes Fields list of Json Format (with Reflection)
     * Json形式でクラスのフィールド一覧を得る (リフレクション利用)
     * */
    private String parseClazz(Class clazz) {
        if (clazz == null) {
            return "null";
        }

        List<String> fields = new ArrayList<>();
        String str;

        for (Field field : clazz.getFields()) {
            fields.add("\"" + field.getName() + "\"");
        }

        return "[" + String.join(", ", fields) + "]";
    }

}
