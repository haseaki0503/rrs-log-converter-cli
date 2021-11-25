import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.junit.Test;
import server.Http;
import viewermanager.ViewerManagerKeys;
import viewermanager.manager.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ManagerTest {

    public static class ServerResponseInstance {
        public Integer viewerId;
        public Integer providerId;
        public Integer target;

        public Integer id;
        public String status;
        public Boolean online;
        public String host;
        public Integer port;
        public Integer timeStep;
        public Integer maxTimeStep;
        public String IDString;

        public Long timeStamp;

        public ServerResponseInstance() {
            viewerId = null;
            providerId = null;
            target = null;

            id = null;
            status = null;
            online = null;
            host = null;
            port = null;
            timeStep = null;
            maxTimeStep = null;
            IDString = null;

            timeStamp = null;
        }
    }

    private ServerResponseInstance getResponse(ByteBuffer data) throws Exception
    {
        if (data == null) return null;
        try {
            return mapper.readValue(data.array(), ServerResponseInstance.class);
        } catch (IOException e) {
            logger.error("Cannot read JSON");
            logger.info("Failed Record: " + new String(data.array()));
            throw e;
        }
    }

    private ServerResponseInstance[] getResponses (ByteBuffer data) throws Exception
    {
        try {
            return mapper.readValue(data.array(), ServerResponseInstance[].class);
        } catch (IOException e) {
            logger.debug("Cannot read JSON");
            logger.info("Failed Record: " + new String(data.array()));
            throw e;
        }
    }


    Logger logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
    ObjectMapper mapper = new ObjectMapper();


    @Test
    public void ManagerTest() throws InterruptedException, IOException {
        DefaultManager manager = new DefaultManager();
        Http http = new Http(manager);
        http.start();

        Thread thread = new Thread(manager::run);
        thread.start();
        thread.join();
    }

    private boolean checkResponse(ServerResponse response)
    {
        return response != null && (response.getStatus() >= 200 && response.getStatus() < 300);
    }

    private void setPath(Manager manager) throws Exception {
        ServerResponse response;
        response = manager.requestAddLogPath("/home/yuma/Downalods/");
        assert checkResponse(response);

        response = manager.requestAddLogPath("/home/yuma/dev/robocup/ViewerManager/logs");
        assert checkResponse(response);
    }

    @Test
    public void ViewerRegisterTest() throws Exception {
        DefaultManager manager = new DefaultManager();

        //  Open Viewer
//        ServerResponse response = manager.requestOpenViewer();
//        assert response != null;
//        assert response.getStatus() == ServerResponse.STATUS_OK || response.getStatus() == ServerResponse.STATUS_ACCEPTED;
//
//        if (response.getData() != null) {
//            logger.info(new String(response.getData().array()));
//        } else assert false;
//
//        // Retry
//        response = manager.requestOpenViewer();
//        assert response != null;
//        assert response.getStatus() == ServerResponse.STATUS_OK || response.getStatus() == ServerResponse.STATUS_ACCEPTED;
//        if (response.getData() != null) {
//            logger.info(new String(response.getData().array()));
//        } else assert false;
//
//        // Get Viewer List
//        response = manager.requestViewerList();
//        assert response != null;
//        assert response.getStatus() == ServerResponse.STATUS_ACCEPTED || response.getStatus() == ServerResponse.STATUS_OK;
//        if (response.getData() != null) {
//            logger.info(new String(response.getData().array()));
//        } else assert false;
    }

    @Test
    public void LogProviderRegisterTest() throws Exception {
//        DefaultManager manager = new DefaultManager();
//        ServerResponse response = null;
//        try {
//            Thread mainThread = new Thread(manager);
//            mainThread.start();
//
//            // Add Log Path
//            response = manager.requestAddLogPath("/home/yuma/dev/robocup/ViewerManager/logs");
//            assert response != null && (response.getStatus() == 200 || response.getStatus() == 202);
//
//            response = manager.requestAddLogPath("/home/yuma/Downloads");
//            assert response != null && (response.getStatus() == 200 || response.getStatus() == 202);
//
//            // Check Log Path
//            response = manager.requestLogList();
//            assert response != null && (response.getStatus() == 200);
//            logger.info(new String(response.getData().array()));
//
//            // Open Provider (Viewer Log)
//            response = manager.requestOpenLogProvider("/home/yuma/dev/robocup/ViewerManager/logs/joao_poseidon.vlog");
//            assert (response != null);
//            if (response.getData() != null)
//                logger.info(new String(response.getData().array()));
//            assert (response.getStatus() == 200 || response.getStatus() == 202);
//            logger.info(new String(response.getData().array()));
//
//            // Check is the record extractable
//            response = manager.requestOpenViewer();
//            assert (response != null) && (response.getStatus() == 200);
//            logger.info(new String(response.getData().array()));
//            // May be no. 0
//            // Connect to Provider
//            response = manager.requestConnection(0, 0);
//            assert (response != null);
//            assert (response.getStatus() < 300);
//            if (response.getMessage() != null)
//                logger.info(response.getMessage());
//
//            // Request
//            boolean satisfied = false;
//            do {
//                try {
//                    ServerRequest request = new ServerRequest(0, Instant.now().getEpochSecond());
//                    response = manager.requestRecords(request);
//                    assert (response != null) && (response.getStatus() == 200 || response.getStatus() == 202);
//                    if (response.getData().array().length > 2) {
//                        logger.info(new String(response.getData().array()).substring(0, 4));
//                        satisfied = true;
//                    }
//                    else {
//                        logger.debug(new String(response.getData().array()));
//                    }
//                } catch (RecordRequestException ex) {
//                    if (ex.getReason() != RecordRequestException.Records_Not_Availale) {
//                        throw ex;
//                    }
//                }
//                System.out.print(".");
//                Thread.sleep(10);
//            } while(!satisfied);
//
//            // Shutdown
//            manager.shutdown();
//            mainThread.join();
//        } catch (AssertionError error) {
//            if (response != null) {
//                logger.info("Failed with Code: " + response.getStatus());
//                if (response.getMessage() != null) {
//                    logger.info("> Message: " + response.getMessage());
//                }
//            }
//
//            Logger logger = manager.getLogger();
//            if (logger != null) {
//                String[] logs = JsonListAppender.getLogs(logger);
//                if (logs != null && logs.length > 0) {
//                    for (String log : logs) {
//                        System.err.println(log);
//                    }
//                }
//            }
//
//            throw error;
//        }
    } // end of function

//    @Test
//    public void ReconnectionTest() throws Exception {
//        DefaultManager manager = new DefaultManager();
//        ServerResponse response = null;
//        ServerResponseInstance instance = null;
//
//        try {
//            Thread thread = new Thread(manager);
//            thread.start();
//
//            // Add Log Path
//            setPath(manager);
//
//            // Open Provider
//            int providerId1 = -1;
//            response = manager.requestOpenLogProvider("/home/yuma/dev/robocup/ViewerManager/logs/paris.vlog");
//            checkResponse(response);
//            instance = getResponse(response.getData());
//            providerId1 = instance.id;
//            logger.debug("Opened Provider: " + providerId1);
//
//            int providerId2 = -1;
//            response = manager.requestOpenLogProvider("/home/yuma/dev/robocup/ViewerManager/logs/joao_mrl.vlog");
//            checkResponse(response);
//            instance = getResponse(response.getData());
//            providerId2 = instance.id;
//            logger.debug("Opened Provider: " + providerId2);
//
//            // Check Provider Info
//            response = manager.requestProviderList();
//            checkResponse(response);
//            System.out.println(new String(response.getData().array()));
//
//            // Open Viewer
//            int viewerId = -1;
//            response = manager.requestOpenViewer();
//            checkResponse(response);
//            instance = getResponse(response.getData());
//            viewerId = instance.viewerId;
//            logger.debug("Opened Viewer: " + viewerId);
//
//            Thread.sleep(1);
//            // Connection
//            response = manager.requestConnection(viewerId, providerId1);
//            checkResponse(response);
//
//            // Get Record 4 times
//            ServerRequest request;
//            for (int i=0; i < 4; i++) {
//                request = new ServerRequest(viewerId, System.currentTimeMillis(), 1);
//                response = manager.requestRecords(request);
//                checkResponse(response);
//                assert response.getData() != null;
//            }
//
//            // Check Viewer State
//            response = manager.requestViewerList();
//            checkResponse(response);
//            System.out.println(new String(response.getData().array()));
//
//            // Disconnect
//            response = manager.requestDisconnect(viewerId);
//            checkResponse(response);
//
//            // Get Record, expect disconnect
//            response = manager.requestRecords(new ServerRequest(viewerId, System.currentTimeMillis(), 1));
//            checkResponse(response);
//            assert response.getMessage() != null;
//            logger.info(response.getMessage());
//
//            Thread.sleep(100);
//            // Connection to provider2
//            response = manager.requestConnection(viewerId, providerId2);
//            checkResponse(response);
//
//            // Get Record 4 times
//            for (int i=0; i < 4; i++) {
//                request = new ServerRequest(viewerId, System.currentTimeMillis(), 4);
//                response = manager.requestRecords(request);
//                checkResponse(response);
//                assert response.getData() != null;
//            }
//
//            // Check Viewer State
//            response = manager.requestViewerList();
//            checkResponse(response);
//            assert response.getStatus() == 200;
//            System.out.println(new String(response.getData().array()));
//
//            // Shutdown
//            manager.requestCloseServer();
//            thread.join();
//        }
//        catch (AssertionError er) {
//            if (response != null) {
//                logger.info("Failed with Code: " + response.getStatus());
//                if (response.getMessage() != null) logger.info("> Message: " + response.getMessage());
//                if (response.getData() != null) logger.info("> Data: " + new String(response.getData().array()));
//            }
//
//            Logger logger = manager.getLogger();
//            if (logger != null) {
//                String[] logs = JsonListAppender.getLogs(logger);
//                if (logs != null && logs.length > 0) {
//                    for (String log: logs) System.err.println(log);
//                }
//            }
//
//            throw er;
//        } // end lf catch
//    }


}
