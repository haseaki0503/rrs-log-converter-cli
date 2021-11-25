import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.junit.Test;
import server.Http;
import server.HttpRest;
import server.rest.DefaultHandler;
import viewermanager.ViewerManagerKeys;
import viewermanager.manager.DefaultManager;
import viewermanager.manager.ServerResponse;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HttpServTest {
    @Test
    public void HttpServTest() {
        HttpServer server = HttpServer.createSimpleServer();
        server.getServerConfiguration().addHttpHandler(
                new HttpHandler() {
                    public void service(Request request, Response response) throws Exception {
                        String data = "<html><head><meta charset='utf-8'><title>Title</title></head><body><h1>Test</h1></body></html>";
                        response.setContentType("text/html");
                        response.setContentLength(data.length());
                        response.getWriter().write(data);
                    }
                },
                "/index");

        try {
            server.start();
            System.out.println("Press any key to stop the session...");
            System.in.read();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static class TestStr {
        public Integer id;
        public String name;
        public Double point;
    }

    @Test
    public void HttpDefaultHandlerTest() throws Exception {
        HttpServer server = HttpServer.createSimpleServer();
        server.getServerConfiguration().addHttpHandler(new DefaultHandler(Method.GET, TestStr.class, (Object e) ->
        {
            ServerResponse res = null;
            if (e != null && e instanceof TestStr) {
                TestStr ts = (TestStr) e;
                res = new ServerResponse(200, ByteBuffer.wrap(String.format("{id: %d, name: \"%s\", point: %f}"
                        , ts.id, ts.name, ts.point).getBytes()));
            }
            else {
                res = new ServerResponse(ServerResponse.STATUS_BADREQ, "v(^_^)v");
            }

            return res;
        }), "/test");

        server.start();
        System.in.read();
        server.shutdownNow();
    }

    @Test
    public void HttpRestTest() throws Exception {
        DefaultManager manager = new DefaultManager();
        Thread thread = new Thread(manager::run);
        HttpRest http = new HttpRest(manager);
        http.start();

        thread.run();
        try {
            thread.join();
            manager.shutdown();
            http.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void HttpRestManagerTest() throws Exception {
        System.setProperty(ViewerManagerKeys.HTTP_STATIC, "/mod/web/static");
        System.setProperty(ViewerManagerKeys.HTTP_WEB_HOME, "/mod/web");

        DefaultManager manager = new DefaultManager();
        manager.requestAddLogPath("/home/yuma/dev/robocup/ViewerManager/logs");
        manager.requestAddLogPath("/home/yuma/Downloads");
        manager.requestOpenViewer(false);
        manager.requestOpenViewer(false);
        manager.requestOpenLogProvider("/home/yuma/dev/robocup/ViewerManager/logs/paris.vlog");
        Thread thread = new Thread(manager::run);
        HttpRest http = new HttpRest(manager);
        http.start();

        thread.run();
        try {
            thread.join();
            manager.shutdown();
            http.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void HttpAccessTest() throws IOException {
        DefaultManager manager = new DefaultManager();
        Http http = new Http(manager);
        http.start();

        try {
            System.out.println("Server Started");
            System.in.read();
        } catch (Exception ex) {
            System.err.println(ex);
        }

    }

}