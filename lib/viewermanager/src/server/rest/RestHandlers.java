package server.rest;


import org.glassfish.grizzly.http.Method;
import viewermanager.manager.Manager;
import viewermanager.manager.ServerResponse;

import java.io.IOException;

/**
 * implements of HttpInterfaces on HttpRest, these Requires parameter arguments.
 * パラメータを引数として要求するHttpインターフェイスの実装．
 * */
public class RestHandlers {

    /**
     * Request of Viewer Open
     * Viewerを開く要求
     *
     * Parameter:
     *      - auto : is automatic connection: 自動的に有効なProviderに接続するか
     * */
    public static class ViewerOpenHandler extends DefaultHandler
    {
        private Manager manager; /* manager to register */
        public ViewerOpenHandler(Manager manager) {
            super(Method.GET, OpenParams.class);
            this.manager = manager;
        }

        @Override
        public ServerResponse serve(Object params) throws Exception {
            boolean req = false;
            if (params != null && params instanceof OpenParams) {
                OpenParams param = (OpenParams) params;
                req = (param.auto != null) ? param.auto : false; // check is the autonomous connection is true
            }
            return manager.requestOpenViewer(req);
        }

        public static class OpenParams {
            public Boolean auto;
        }
    }

    /**
     * Request for Connection, Viewer and Provider
     * ViewerとProviderを接続する要求
     *
     * Parameter:
     *      - viewerid : 接続するViewerId
     *      - providerId : 接続するProviderId
     * */
    public static class ConnectionHandler extends DefaultHandler
    {
        private Manager manager;
        public ConnectionHandler(Manager manager) {
            super(Method.POST, ConnectionParams.class);
            this.setService(this::serv);
            this.manager = manager;
        }

        public ServerResponse serv(Object o) {
            if (o != null && o instanceof ConnectionParams) {
                ConnectionParams param = (ConnectionParams)o;
                if (param.viewerId != null && param.providerId != null)
                { /* both needed! */
                    return manager.requestConnection(param.viewerId, param.providerId);
                }
            }

            return new ServerResponse(ServerResponse.STATUS_BADREQ, "['viewerId', 'providerId'] both cannot be null.");
        }

        public static class ConnectionParams {
            public Integer viewerId;
            public Integer providerId;
        }
    }

    /**
     * Request for Disconnect Viewer and Provider
     * ViewerとProviderの切断の要求
     *
     * Parameter:
     *      - viewerId : 切断するViewerID
     * */
    public static class DisconnectionHandler extends DefaultHandler
    {
        private Manager manager;
        public DisconnectionHandler(Manager manager) {
            super(Method.POST, DisconnectionParameter.class);
            setService(this::serv);
            this.manager = manager;
        }

        public ServerResponse serv(Object o) {
            if (o != null && o instanceof DisconnectionParameter)
            {
                DisconnectionParameter param = (DisconnectionParameter)o;
                if (param.viewerId != null) { /* Viewer ID needed. */
                    return manager.requestDisconnect(param.viewerId);
                }
            }

            return new ServerResponse(ServerResponse.STATUS_BADREQ, "['viewerId'] cannot be null.");
        }

        public static class DisconnectionParameter
        {
            public Integer viewerId;
        }
    }

    /**
     * Request for Open Provider
     * 新しいProviderを開く要求
     *
     * Parameters:
     *      - host : 接続するKernelのホスト名
     *      - port : 接続するKernelのポート番号
     * */
    public static class OpenProviderHandler extends DefaultHandler
    {
        private Manager manager;
        public OpenProviderHandler(Manager manager) {
            super(Method.POST, OPParameter.class);
            setService(this::serv);
            this.manager = manager;
        }

        public ServerResponse serv(Object o)
        {
            if (o != null && o instanceof OPParameter)
            {
                OPParameter param = (OPParameter)o;
                if (param.host != null) { /* hostName needed */
                    try {
                        return manager.requestOpenProvider(param.host, param.port);
                    } catch (IOException e) {
                        logger.error("/rest/provider/open: failed to open provider", e);
                    }
                }
            }

            return new ServerResponse(ServerResponse.STATUS_BADREQ, "['host'] cannot be null.");
        }

        public static class OPParameter {
            public String host;
            public Integer port;
        }
    }

    /**
     * Request for Close Provider
     * Providerを閉じる要求
     *
     * Parameter:
     *      - providerId : 閉じるProviderId
     * */
    public static class CloseProviderHandler extends DefaultHandler {
        private Manager manager;
        public CloseProviderHandler(Manager manager) {
            super(Method.POST, CloseParameter.class);
            setService(this::serv);
            this.manager = manager;
        }

        public ServerResponse serv(Object o) {
            if (o != null && o instanceof CloseParameter) {
                CloseParameter param = (CloseParameter)o;
                if (param.providerId != null) {
                    try {
                        return manager.requestCloseProvider(param.providerId);
                    } catch (IOException e) {
                        logger.error("/rest/provider/close: failed to close provider", e);
                        return new ServerResponse(ServerResponse.STATUS_INTERNAL_ERROR);
                    }
                }
            }
            return new ServerResponse(ServerResponse.STATUS_BADREQ, "['providerId'] cannot be null.");
        }

        public static class CloseParameter {
            public Integer providerId;
        }
    }

    /**
     * Request for Open Log Provider
     * ログを開く要求
     *
     * Parameters:
     *      - path : ログファイルのあるファイルパス
     * */
    public static class OpenLogHandler extends DefaultHandler
    {
        private Manager manager;
        public OpenLogHandler(Manager manager) {
            super(Method.POST, PathParam.class);
            setService(this::serv);
            this.manager = manager;
        }

        public ServerResponse serv(Object o) {
            if (o != null && o instanceof PathParam) {
                PathParam param = (PathParam) o;
                try {
                    return manager.requestOpenLogProvider(param.path);
                } catch (IOException e) {
                    logger.error("/rest/log/open: cannot be open file", e);
                }
            }

            return new ServerResponse(ServerResponse.STATUS_BADREQ, "['path'] cannot be null.");
        }

    }

    /**
     * Request for add Log Path
     * ログパスを追加する要求
     *
     * Parameters:
     *      - path : 追加するログパス
     * */
    public static class AddLogPathHandler extends DefaultHandler
    {
        private Manager manager;
        public AddLogPathHandler(Manager manager) {
            super(Method.POST, PathParam.class);
            setService(this::serv);
            this.manager = manager;
        }

        public ServerResponse serv(Object o) {
            if (o != null && o instanceof PathParam) {
                PathParam param = (PathParam)o;
                try {
                    return manager.requestAddLogPath(param.path);
                } catch (IOException e) {
                    logger.error("/rest/log/path/add: cannot be add path", e);
                }
            }
            return new ServerResponse(ServerResponse.STATUS_BADREQ, "['path'] cannot be null.");
        }
    }

    /**
     * path parameter
     * パスを受け取るパラメータ
     * */
    public static class PathParam {
        public String path;
    }
}
