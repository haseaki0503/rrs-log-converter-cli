package server.rest;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.util.Parameters;
import viewermanager.ViewerManagerKeys;
import viewermanager.manager.ServerResponse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * DefaultHandler is a implements of HttpHandler
 *  This class will serve message to parse, and pass to specified method.
 *
 * <code>HttpHandler</code>の実装であり，サーバが受け取ったメッセージから指定されたメソッドを呼び出します．
 **/
public class DefaultHandler extends HttpHandler {

    Logger logger;

    protected Method method;
    protected Class parameterClazz;
    private Object parameters;
    private Function<Object, ServerResponse> serviceDelegate;
    private ObjectMapper mapper;

    public DefaultHandler(Method method) {
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (logger == null) {
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }

        this.method = method;
        this.parameters = null;
        this.serviceDelegate = null;
        this.mapper = new ObjectMapper();
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public DefaultHandler(Method method, Class parameterClazz) {
        this(method);
        this.parameterClazz = parameterClazz;
    }

    public DefaultHandler(Method method, Function<Object, ServerResponse> server) {
        this(method);
        this.serviceDelegate = server;
    }

    public DefaultHandler(Method method, Supplier<ServerResponse> server) {
        this(method, (Object p) -> server.get());
    }

    public DefaultHandler(Method method, Class parameterClazz, Function<Object, ServerResponse> server) {
        this(method, parameterClazz);
        this.serviceDelegate = server;
    }

    public DefaultHandler(Method method, Class parameterClazz, Supplier<ServerResponse> server) {
        this(method, parameterClazz, (Object p) -> server.get());
    }

    /**
     * setMethod : set the http method for processing.
     * 受け取るHttpメソッドを指定します．
     *
     * @param method http method of processing, GET/POST/HEAD/DELETE/PUT.
     * @return this class instance
     * */
    public DefaultHandler setMethod(Method method) {
        this.method = method;
        return this;
    }

    /**
     * get http method processing.
     * 処理するHttpメソッドを取得します．
     * */
    public Method getMethod() {
        return this.method;
    }

    /**
     * setService : set the function of processing
     * 処理するための関数を指定します．
     *
     * @param serviceFunc the function for this handler.
     * @return this class instance
     * */
    public DefaultHandler setService(Function<Object, ServerResponse> serviceFunc) {
        this.serviceDelegate = serviceFunc;
        return this;
    }

    /**
     * get parameter clazz
     * パラメータを変換する先の<code>Class</code>を取得します．
     *
     * */
    public Class getParameterClazz() {
        return parameterClazz;
    }

    /**
     * setParameterClazz : set the parameter class
     *  current implements, allow the type of member for Integer,Double,Boolean and String
     * パラメータを変換する先の<code>Class</code>を設定します．
     *  現在の実装では，Integer，Double，String，Booleanの変換ができます．
     *
     * @param clazz the class for processing.
     * @return this class instance
     * */
    public DefaultHandler setParameterClazz(Class clazz) {
        this.parameterClazz = clazz;
        return this;
    }

    /**
     * service: implementation of HttpHandler::service
     * */
    @Override
    public void service(Request request, Response response) throws Exception {
        if (validateCall(response, request.getMethod(), method, request.getRequestURI())) {
            request.getParameterNames(); /* experimental:: to finalize parameters */

            /* convert parameter of request to instance of Clazz -> assign to `parameters` */
            readParameters(request.getParameters());
            /* call serve with `parameters` */
            ServerResponse resultServed = serve(parameters);
            /* write response with response */
            writeResponse(request, response, resultServed);
        }

        // Write out Log
        connectionLog(request, response);
    }

    /**
     * serve : run the service
     * 受け取ったパラメータで指定された関数を呼び出し，<code>ServerResponse</code>を受け取ります．
     * もしくは処理を実行し，<code>ServerResponse</code>を返します．
     *
     * @param params parameters object specified by parameterClazz.
     * @return valid ServerResponse if succeeded, other throws Exception, or returns null.
     * @implNote overwrite this method for complex implementation
     * */
    public ServerResponse serve(Object params) throws Exception
    { /* OVERRIDE OR SET FUNC */
        if (this.serviceDelegate != null) {
            return serviceDelegate.apply(params);
        }
        return null;
    }

    /**
     * readParameters : read parameters and parse to Clazz
     * 受け取ったパラメータをクラスオブジェクトに変換します．
     * @param parameters parameters paired from Grizzly
     * @implNote override this method for complex implementation
     * */
    public void readParameters(Parameters parameters) throws Exception
    {
        if (parameterClazz == null || parameters == null) return;

        // Fields of Clazz to convert
        Field[] fields = parameterClazz.getFields();
        Object param = parameterClazz.newInstance();
        logger.debug("parameterClazz: " + parameterClazz.getName());

        for (Field field : fields) {
            String parameter = parameters.getParameter(field.getName());
            if (parameter != null) {
                try {
                    // field available on input Parameters, try Parse
                    if (field.getType().equals(Integer.class)) {
                        // Convert to Integer
                        field.set(param, Integer.parseInt(parameter));
                    }
                    else if (field.getType().equals(Double.class)) {
                        // Convert to Double
                        field.set(param, Double.parseDouble(parameter));
                    }
                    else if (field.getType().equals(String.class)) {
                        // Assign as String
                        field.set(param, parameter);
                    }
                    else if (field.getType().equals(Boolean.class)) {
                        // Convert to Boolean: check with valid Signature
                        if (parameter.matches("(True|true|TRUE|1|On|On|ON)")) {
                            field.set(param, true);
                        }
                        else {
                            field.set(param, false);
                        }
                    }

                    logger.debug(String.format("> %s = %s", field.getName(), parameter));
                } catch (IllegalAccessException e) {
                    // Ignore
                }
            } // fi : parameter != null
        } // rof

        // set to param
        this.parameters = param;
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Inner Helper Functions
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /**
     * output connection logs.
     *  inform: uri(status): send Bytes count and content types
     * 通信ログを出力する．
     * */
    private void connectionLog(Request request, Response response)
    {
        logger.debug (String.format("%s %s(%d): send %d bytes with content %s"
                , request.getMethod().getMethodString()
                , request.getRequestURI(), response.getStatus()
                , response.getContentLength(), response.getContentType()));
    }

    /**
     * check is the calling method receivable on this Handler
     * 正しいHttpメソッドで呼び出されているかチェックする
     * */
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

    /**
     * write Response helper
     * 受け取った<code>ServerResponse</code>を使い，Httpレスポンスを作成する
     * */
    private void writeResponse(Request request, Response response, ServerResponse serverResponse) throws Exception
    {
        // both, not null.
        if (response == null || serverResponse == null) return;

        // write response from manager
        String res = mapper.writeValueAsString(serverResponse);

        response.setContentType("application/json");
        response.setContentLength(res.length());
        response.getWriter().write(res);
    }

    /**
     * parse parameters to json, the easier implementation
     * 受け取ったパラメータをJsonとして出力する．
     * */
    private String parameterJson(Request request) {
        List<String> parameters = new ArrayList<>();
        for (String name : request.getParameterNames()) {
            parameters.add(String.format("\"%s\": \"%s\"", name, request.getParameter(name)));
        }
        return "{" + String.join(", ", parameters) + "}";
    }

    /**
     * write error string for logger
     * 発生したエラーをLoggerに書き出し，HttpResponseに書き込む
     * */
    private void writeError (Request request, Response response, HttpStatus status, String message) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("\"uri\": \"" + request.getRequestURI() + "\"");
        if (message != null) {
            args.add("\"msg\": \"" + message + "\"");
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

}
