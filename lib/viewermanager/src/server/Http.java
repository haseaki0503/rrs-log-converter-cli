package server;


import com.fasterxml.jackson.databind.ObjectMapper;
import logger.JsonListAppender;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.grizzly.http.util.HttpStatus;
import server.rest.DefaultHandler;
import viewermanager.ViewerManagerKeys;
import viewermanager.manager.Manager;
import viewermanager.manager.RecordRequestException;
import viewermanager.manager.ServerRequest;
import viewermanager.manager.ServerResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class Http implements Server {

    /**
     * manager
     * */
    protected Manager manager;

    /**
     * http server handle
     * */
    protected HttpServer server;

    /**
     * shutdown Future
     * */
    protected GrizzlyFuture<HttpServer> shutdown;

    Logger logger;

    Map <String, HttpHandler> handlers;


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Main Functions
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    public Http(Manager manager) {
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.DEFAULT_LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (logger == null) {
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }
        this.manager = manager;

        this.server = HttpServer.createSimpleServer();

        handlers = new HashMap<>();
        handlers.put("/server/shutdown", new DefaultHandler(Method.GET, (Object p) -> manager.requestCloseServer()));
        handlers.put("", new DefaultHandler(Method.GET, (Object p) -> manager.requestLogList()));

        // Get Index ******************************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                String text = "<html><h1>Hello</h1></html>";
                response.setContentType("text/html");
                response.getWriter().write(text);
            }
        }, "/");

        // Get Record *****************************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpGetViewerRecord(request, response);
            }
        }, "/viewer/record");

        // Get Provider List **********************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpGetProviderList(request, response);
            }
        }, "/list/providers");

        // Get Viewer List ************************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpGetViewerList(request, response);
            }
        }, "/list/viewers");

        // Post Request Open Viewer ***************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpGetViewerOpen(request, response);
            }
        }, "/viewer/open");

        // Post Request Connection  ***************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpPostConnect(request, response);
            }
        }, "/viewer/connect");

        // Post Request Disconnect  ***************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpPostDisconnect(request, response);
            }
        }, "/viewer/disconnect");

        // Post Request Open Provider  ************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpPostOpenProvider(request, response);
            }
        }, "/provider/open");

        // Post Request Close Provider ************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpPostCloseProvider(request, response);
            }
        }, "/viewer/connect");

        // Post Request Close Server **************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpPostCloseServer(request, response);
            }
        }, "/viewer/connect");


        // Get Errors *****************************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpGetErrors(request, response);
            }
        }, "/errors");

        // Get Interfaces *************************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpGetInterfaces(request, response);
            }
        }, "/interfaces");

        // Get Interfaces *************************************************************************
        this.server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                httpPostLogAddPath(request, response);
            }
        }, "/log/addPath");


    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Http Method Call
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private void connectionLog(Request request, Response response)
    {
        logger.debug (String.format("%s(%d): send %d bytes with content %s"
                , request.getRequestURI(), response.getStatus()
                , response.getContentLength(), response.getContentType()));
    }

    public boolean validateCall(Response response, Method actual, Method expected, String path) {
        boolean result = true;
        if (actual != expected) {
            // redirect to Bad Request
            logger.info(String.format("call %s with %s, ignored", path, actual.getMethodString()));
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            result = false;
        }

        response.setCharacterEncoding("utf-8");
        return result;
    }


    private void writeResponse(Response response, ServerResponse serverResponse) throws Exception
    {
        // both, not null.
        if (response == null || serverResponse == null) return;

        // write response from manager
        String res = (new ObjectMapper()).writeValueAsString(serverResponse);

        response.setContentType("application/json");
        response.setContentLength(res.length());
        response.getWriter().write(res);
    }

    private String parameterJson(Request request) {
        List<String> parameters = new ArrayList<>();
        for (String name : request.getParameterNames()) {
            parameters.add(String.format("\"%s\": \"%s\"", name, request.getParameter(name)));
        }
        return "{" + String.join(", ", parameters) + "}";
    }


    private void writeError (Request request, Response response, HttpStatus status, String message) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("\"uri\": " + request.getRequestURI());
        if (message != null) {
            args.add("\"msg\": " + message);
        }
        if (request.getParameterNames().size() > 0) {
            args.add("\"parameters\": " + parameterJson(request));
        }
        String result = "{" + String.join(", ", args) + "}";

        response.setStatus(status);
        response.setContentType("application/json'");
        response.setContentLength(result.length());
        response.getWriter().write(result);
    }


    private void httpGetViewerRecord(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.GET, request.getRequestURI())) {
            // get parameters
            ServerRequest serverRequest = null;
            long timestamp = System.currentTimeMillis();

            // ViewerId must not be null
            InnerParameters parameters = new InnerParameters(request);
            if (parameters.viewerId == null) {
                writeError(request, response, HttpStatus.BAD_REQUEST_400,
                        "ViewerID not valid");
                logger.error(String
                        .format("%s # %s: %s"
                                , request.getRequestURI()
                                , request.getQueryString()
                                , "No ViewerID given"));
            }
            else {
                // Check What type of request
                if (parameters.time != null) {
                    // Call with {ViewerId, Time, Count}
                    if (parameters.count == null) {
                        writeError(request, response, HttpStatus.BAD_REQUEST_400,
                                "Request 'Count' from 'Time' without 'Count'");
                        logger.error(String
                                .format("%s # %s: %s"
                                        , request.getRequestURI()
                                        , request.getQueryString()
                                        , "No Count given for Call for {ViewerId, Time, Count}"));
                    }
                    else {
                        serverRequest = new ServerRequest(parameters.viewerId, timestamp, parameters.time, parameters.count);
                    }
                } else if (parameters.count != null) {
                    // Call with {ViewerId, Count}
                    serverRequest = new ServerRequest(parameters.viewerId, timestamp, parameters.count);
                } else {
                    // Call with {ViewerId} only
                    serverRequest = new ServerRequest(parameters.viewerId, timestamp);
                }

                if (serverRequest != null) {
                    // call manager control
                    try {
                        ServerResponse serverResponse = manager.requestRecords(serverRequest);
                        writeResponse(response, serverResponse);
                    } catch (RecordRequestException ex) {
                        int reason = ex.getReason();
                        String msg = "Unknown Error";
                        if (reason == RecordRequestException.Unknown_Viewer) {
                            msg = "Requested from Unknown Viewer";
                        } else if (reason == RecordRequestException.Records_Not_Availale) {
                            msg = "Requested Records not Available";
                        } else if (reason == RecordRequestException.Records_Not_Found) {
                            msg = "Requested Records Not Found";
                        } else if (reason == RecordRequestException.In_Future) {
                            msg = "Requested Record on Future";
                        }
                        logger.error(msg, ex);
                        writeError(request, response, HttpStatus.BAD_REQUEST_400, msg);
                    }
                }
            } // fi: else parameters.viewerId == null
        } // fi validate

        // Log
        connectionLog(request, response);
    }

    private void httpGetProviderList(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.GET, request.getRequestURI())) {
            // get provider list image
            ServerResponse res = manager.requestProviderList();
            writeResponse(response, res);
        }

        // Log
        connectionLog(request, response);
    }

    private void httpGetViewerList(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.GET, request.getRequestURI())) {
            // get viewer list image
            ServerResponse res = manager.requestViewerList();
            writeResponse(response, res);
        }

        // Log
        logger.debug(String.format("%s(%d): send %d bytes with content %s"
                , request.getRequestURI(), response.getStatus()
                , response.getContentLength(), response.getContentType()));
    }

    private void httpGetViewerOpen(Request request, Response response) throws Exception {
        if (validateCall(response, request.getMethod(), Method.POST, request.getRequestURI()))
        {
            ServerResponse serverResponse = manager.requestOpenViewer(false);
            writeResponse(response, serverResponse);
        }

        // Log
        connectionLog(request, response);
    }

    private void httpPostConnect(Request request, Response response) throws Exception {
        if (validateCall(response, request.getMethod(), Method.POST, request.getRequestURI()))
        {
            // Get Provider Id and ViewerId
            InnerParameters param = new InnerParameters(request);

            if (param.viewerId == null || param.providerId == null) {
                // both needed.
                writeError(request, response, HttpStatus.BAD_REQUEST_400,
                        "ViewerID and ProviderID not valid");
                logger.error(String
                        .format("%s # %s: %s"
                                , request.getRequestURI()
                                , request.getQueryString()
                                , "No ViewerID/ProviderID given"));
            }
            else {
                // Request
                ServerResponse serverResponse = manager.requestConnection(param.viewerId, param.providerId);
                writeResponse(response, serverResponse);
            }
        } // fi validate

        // Log
        connectionLog(request, response);
    }

    private void httpPostDisconnect(Request request, Response response) throws Exception {
        if (validateCall(response, request.getMethod(), Method.POST, request.getRequestURI()))
        {
            // Get Viewer ID
            InnerParameters param = new InnerParameters(request);

            if (param.viewerId == null) {
                // both needed.
                writeError(request, response, HttpStatus.BAD_REQUEST_400,
                        "ViewerID not valid");
                logger.error(String
                        .format("%s # %s: %s"
                                , request.getRequestURI()
                                , request.getQueryString()
                                , "No ViewerID given"));
            }
            else {
                // Request
                ServerResponse serverResponse = manager.requestDisconnect(param.viewerId);
                writeResponse(response, serverResponse);
            }
        } // fi validate

        // Log
        connectionLog(request, response);
    }

    private void httpPostOpenProvider(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.POST, request.getRequestURI()))
        {
            // Get Parameters
            InnerParameters param = new InnerParameters(request);
            ServerResponse serverResponse = null;

            if (param.host == null) {
                writeError(request, response, HttpStatus.BAD_REQUEST_400,
                        "HostName not provided");
                logger.error(String
                        .format("%s # %s: %s"
                                , request.getRequestURI()
                                , request.getQueryString()
                                , "No HostName given"));
            }
            else if (param.port != null) {
                serverResponse = manager.requestOpenProvider(param.host, param.port);
            }
            else {
                serverResponse = manager.requestOpenProvider(param.host, null);
            }
            writeResponse(response, serverResponse);
        } // fi validate

        // Log
        connectionLog(request, response);
    }

    private void httpPostCloseProvider(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.POST, request.getRequestURI()))
        {
            // Parse Parameters
            InnerParameters parameters = new InnerParameters(request);

            if (parameters.providerId == null) {
                writeError(request, response, HttpStatus.BAD_REQUEST_400,
                        "Provider not specified");
                logger.error(String
                        .format("%s # %s: %s"
                                , request.getRequestURI()
                                , request.getQueryString()
                                , "No ProviderID given"));
            }

            // Request
            ServerResponse serverResponse = manager.requestCloseProvider(parameters.providerId);
            writeResponse(response, serverResponse);
        }

        // Log
        connectionLog(request, response);
    }

    private void httpPostCloseServer(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.POST, request.getRequestURI()))
        {
            ServerResponse serverResponse = manager.requestCloseServer();
            writeResponse(response, serverResponse);
        }

        // Log
        connectionLog(request, response);
    }

    private void httpGetErrors(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.GET, request.getRequestURI())) {
            // parse parameters
            InnerParameters parameters = new InnerParameters(request);
            int count = (parameters.count != null) ? parameters.count : -1;

            // get Errors
            Appender listAppender = logger.getAppender("list");
            String result = "";
            if (listAppender instanceof JsonListAppender) {
                JsonListAppender appender = (JsonListAppender) listAppender;
                List<String> logs = appender.getListLogs();
                if (count > 0) {
                    // Slice size
                    logs = logs.subList(logs.size() - count, logs.size());
                }
                result = "[" + String.join(", ", logs) + "]";
            }

            // Create Response Text
            String data = String.format("{\"errors\": %s}", result);

            // Create Response
            response.setContentLength(data.length());
            response.setContentType("application/json");
            response.setStatus(HttpStatus.OK_200);
            response.getWriter().write(data);
        }

        // Log
        connectionLog(request, response);
    }

    private void httpGetInterfaces(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.GET, request.getRequestURI())) {
        }

        // Log
        connectionLog(request, response);
    }

    private void httpPostLogAddPath(Request request, Response response) throws Exception
    {
        if (validateCall(response, request.getMethod(), Method.POST, request.getRequestURI())) {
            if (request.getParameter("path") == null) {
                writeError(request, response, HttpStatus.BAD_REQUEST_400, "parameter 'path' cannot null");
            }
            else {
                String path = request.getParameter("path");
                ServerResponse serverResponse = manager.requestAddLogPath(path);
                writeResponse(response, serverResponse);
            }
        }

        // Log
        connectionLog(request, response);
    }


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Service Functions
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


    public void start() throws IOException {
        if (this.server != null) {
            this.server.start();

            logger.info("/http/start: Http Server Started on port '"+ NetworkListener.DEFAULT_NETWORK_PORT +"'");
        }
    }

    public void shutdown() {
        if (this.server != null) {
            shutdown = this.server.shutdown();
            this.server = null;
            this.manager = null;
        }
    }


    public boolean isClosed() {
        return (server == null) && (shutdown != null) && (shutdown.isDone());
    }



    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Inner Helper Classes
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    private class InnerParameters
    {
        /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
        // Fields
        /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

        public Integer viewerId;
        public Integer providerId;

        public Integer time;
        public Integer count;

        public String host;
        public Integer port;

        /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

        public InnerParameters(Request request) {
            parse(request);
        }

        /**
         * parse parameter from Parameters from HTTP
         *
         * if some error occurred, ignore it.
         * cannot parse, cannot read, not in input, and every errors can know with
         *  null these fields.
         * */
        public void parse(Request request)  {
            Field[] fields = this.getClass().getFields();

            for (Field field : fields) {
                String parameter = request.getParameter(field.getName());
                if (parameter != null) {
                    try {
                        // field available on input Parameters, try Parse
                        if (field.getType().equals(Integer.class)) {
                            field.set(this, parseInt(parameter));
                            logger.debug ("parse " + field.getName() + " to " + field.get(this));
                        }
                        else if (field.getType().equals(String.class)) {
                            field.set(this, parameter);
                        }
                    } catch (IllegalAccessException e) {
                        // Ignore
                    }
                } // fi : parameter != null
            } // rof
        }

        protected Integer parseInt(String str) {
            Integer param = null;
            try {
                param = Integer.parseInt(str);
            }
            catch (NumberFormatException ex) {
                logger.info("Cannot parse invalid parameter as Integer : " + str);
            }
            return param;
        }
    }

}
