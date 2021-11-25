package server.rest;

import org.glassfish.grizzly.http.Method;
import viewermanager.entity.Record;
import viewermanager.manager.Manager;
import viewermanager.manager.RecordRequestException;
import viewermanager.manager.ServerRequest;
import viewermanager.manager.ServerResponse;

import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Request for Records by Viewer
 * Viewerからレコードの要求
 *
 * Parameters:
 *      - viewerId : ViewerのID
 *      - time : 要求するレコードの開始時間 (optional)
 *      - count : 要求するレコードの数 (optional)
 *
 * Arguments Set
 *      - (viewerId)
 *      - (viewerId, count)
 *      - (viewerId, time, count)
 * */
public class RecordHandler extends DefaultHandler{

    private Manager manager;
    public RecordHandler(Manager manager) {
        super(Method.GET, RecordParam.class);
        setService(this::serv);
        this.manager = manager;
    }

    public ServerResponse serv(Object o) {
        if (o == null || !(o instanceof RecordParam)) {
            return new ServerResponse(ServerResponse.STATUS_BADREQ, "need valid parameters");
        }

        RecordParam param = (RecordParam) o;
        if (param.viewerId == null) {
            logger.error("/rest/getRecord: request 'viewerId' is null.");
            return new ServerResponse(ServerResponse.STATUS_BADREQ, "['viewerId'] cannot be null.");
        }

        // Create Request
        ServerRequest request;
        long timestamp = System.currentTimeMillis();
        if (param.time != null) {
            // request {viewerId, time, count}
            request = new ServerRequest(param.viewerId, timestamp, param.time, param.count);
        }
        else if (param.count != null) {
            // request {viewerId, count}
           request = new ServerRequest(param.viewerId, timestamp, param.count);
        }
        else {
            // request {viewerId}
            request = new ServerRequest(param.viewerId, timestamp);
        }

        // try to get records
        return manager.requestRecords(request);
    }

    public static class RecordParam
    {
        public Integer viewerId;
        public Integer time;
        public Integer count;

    }
}
