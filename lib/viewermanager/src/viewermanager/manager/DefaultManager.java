package viewermanager.manager;

import org.apache.log4j.Logger;
import viewermanager.ViewerManagerKeys;
import viewermanager.encoder.EncodeException;
import viewermanager.encoder.MsgPackEncoder;
import viewermanager.entity.Record;
import viewermanager.entity.provider.*;
import viewermanager.log.LogFileDirectory;
import viewermanager.log.LogFileException;
import viewermanager.recorder.DefaultRecorder;
import viewermanager.recorder.Recorder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;


public class DefaultManager implements Manager {

    /**
     * viewer's id => viewer's status
     * ViewerStateを保持，ProviderIdで検索
     * */
    Map<Integer, ViewerState> viewerStates;

    /**
     * provider's id => provider's records
     * Recorderを保持，ProviderIdで検索
     * */
    Map<Integer, Recorder> viewerRecorders;

    /**
     * provider's list
     * Providerを保持,ProviderIdで検索
     * */
    Map<Integer, IEntityProvider> providers;

    /**
     * provider inheritance on Online Provider.
     *  If the current providing finished, the next id uses as next provider.
     *  It'll be forwarded until the newest provider.
     * Online Provierのとき，Provderを継続する．
     *  現在のProviderが終わったときに，設定してある次のProviderが使われる．
     *  もし次の先があるなら，最新のものまで進められる．
     *
     * Ex) + is a viewer's current provider
     *   | P ID | Chain ID |   State   |
     *  +|  1   |    2     | PROVIDING |
     *   |  2   |    3     |  FINISHED |
     *   |  3   |    -     | RECEIVING |
     *  If the Privder1 ll have been finished providing,
     *    Next Provider is Provider3, so Provider2 is finished.
     *  Provider1が提供を終えたとき，次に使われるのはProvider3である(Provider2は終了済み).
     * */
    Map<Integer, Integer> providerChain;

    /**
     * status of Server, 0==Initial, 1==Running, 2==Ending, 3==End
     * 現在のManagerの状態
     * */
    Integer serverState;
    public static final int STATE_INITIAL = 0; /// Initial, Not Running. 初期化後で動いていない
    public static final int STATE_RUNNING = 1; /// Working also Running. 仕事中で動いている
    public static final int STATE_ENDING = 2;  /// Ending, but Running.  終了中で動いている
    public static final int STATE_ENDED = 3;   /// Ended, Not Running.   終了後で動いていない

    /**
     * for logs
     * Logのディレクトリを保持，ファイル一覧を取得
     * */
    LogFileDirectory logFileDirectory;

    /**
     * current number of Providers
     * 現在，割り当てた最後のProviderId
     * */
    int numProvider;

    Logger logger;
    public Logger getLogger() {
        return logger;
    }

    /**
     * Default Constructor.
     * コンストラクタ
     * */
    public DefaultManager() {
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (Objects.isNull(logger)) {
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }

        viewerStates = new HashMap<>();
        viewerRecorders = new HashMap<>();
        providers = new HashMap<>();
        providerChain = new HashMap<>();
        logFileDirectory = new LogFileDirectory();
        serverState = STATE_INITIAL;
        numProvider = 0;
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Record Request
    // Recordに関する処理
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
     * Process Viewer's request of record.
     * Return {@code ServerResponse} includes records requested.
     * If records requested is not available, included records must be listed empty.
     *
     * ViewerからのRecord要求を処理する.
     * 要求されたレコードが入った {@code ServerResponse} を返す．
     * 要求されたレコードが存在しない場合，空のリストを利用する．
     *
     * @param request Request from Viewer. Viewerからの要求.
     * @return {@code ServerResponse} includes records. レコードを含む {@code ServerResponse}.
     */
    @Override
    public ServerResponse requestRecords(ServerRequest request)
    {
        int viewerId = (request.getViewerId() != null) ? request.getViewerId() : Integer.MAX_VALUE;

        ServerResponse response;

        /* Get Information to create response */
        ViewerState viewerState = null;
        synchronized (this) {
            /* Check Viewer Available */
            if (!viewerStates.containsKey(viewerId)) {
                logger.info("/manager/request/record: Record Requested form Unknown Viewer " + viewerId);
                return new ServerResponse(ServerResponse.STATUS_BADREQ, "Unknown Viewer : {\"viewerId\": " + viewerId + "}");
            }

            /* Get Viewer Session */
            viewerState = viewerStates.get(viewerId);

            /* Check is the Session Status */
            if (viewerState.getStatus().equals(ViewerState.STATUS_DISCONNECTED)
                     || viewerState.getStatus().equals(ViewerState.STATUS_RECONNECTED))
            {
                // Send Client to Disconnection
                logger.info("/manager/request/record: Notify disconnection to Client");
                viewerState.setStatus(ViewerState.STATUS_SENDING);
                viewerStates.put(viewerState.getViewerId(), viewerState);

                return new ServerResponse(ServerResponse.STATUS_OK, "Disconnected"
                        , new DefaultResponse.RecordResult(true));
            }

            /* Get Recorder */
            Recorder recorder = viewerRecorders.get(viewerState.getProviderId());
            if (recorder == null) {
                // No connection
                logger.info("/manager/request/record: Record Requested from Viewer " + viewerId + " has not connected");
                return new ServerResponse(ServerResponse.STATUS_OK, "not connected");
            }
            else if(recorder.size() <= 0) {
                // No Records
                logger.info("/manager/request/record: Record Requested from Viewer " + viewerId + " has no Data ");
                return new ServerResponse(ServerResponse.STATUS_OK);
            }

            /* Get Records */
            int time = (request.getTime() != null) ? request.getTime() : viewerState.timeStep;
            int count = (request.getCount() != null) ? request.getCount() : recorder.size();

            if (time < 0 || time > recorder.size()) { /* not valid time from */
                return new ServerResponse(ServerResponse.STATUS_OK, "No Records Available");
            }
            else if (time == recorder.size()) {
                logger.info("/manager/request/record: required timeStep is the last of Records");
                return new ServerResponse(ServerResponse.STATUS_OK);
            }

            List<ByteBuffer> records = new ArrayList<>();
            for (int i=0; i<count; i++) {
                ByteBuffer record = recorder.getRecord(time+i);
                if (record == null) { break; }
                records.add(record);
            }
            /* Update request timeStamp */
            viewerState.setTimeStamp(request.timeStamp);

            // set current state
            viewerStates.put(viewerState.getViewerId(), viewerState);

            /* Check Provider */
            IEntityProvider provider = providers.get(viewerState.getProviderId());

            // Save State of Records
            int lastTime = viewerState.getTimeStep() + records.size(); // latestTime = current_time + actual_count
            viewerState.setTimeStep(lastTime);

            // If switch Provide Finished of last data
            if (provider != null && lastTime >= provider.getMaxTimeSteps())
            {
                // providing Ended
                viewerState.provideFinished();

                // Next Providing
                Integer id = provider.getID(); // current provider id
                while (providerChain.containsKey(id)) {
                    id = providerChain.get(provider.getID());
                }
                viewerState.connectProvider(id);
                logger.info("/manager/get/records: Viewer " + viewerId + " connection switched to Provider " + id);
            }
            else {
                viewerState.setStatus(ViewerState.STATUS_SENDING);
            }

            viewerStates.put(viewerState.getViewerId(), viewerState);
            response = new ServerResponse(ServerResponse.STATUS_OK,
                    new DefaultResponse.RecordResult(records));

            logger.info("/manager/get/records: Viewer " + viewerId + ", get records "  + records.size());
            logger.debug("> " + formatViewer(viewerState));
        } // :end of synchronized

        return response;
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Viewer Open
    // Viewerの新規追加についての処理
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
     * Process request for new open Viewer, and assign viewerId.
     * If the parameter {@code manual} is {@code false}, Manager assigns opened Provider to incoming Viewer automatically.
     * {@code manual} が {@code false}の時は，すでに開かれているProviderに自動的に接続する．
     * Otherwise, Manager only process open, and assign ID.
     * In this case, {@code ServerResponse} returned may contains no ProviderId.
     *
     * 新しく開かれたViewerについての処理を行い，ViewerIDを割り当てる．
     * その他の場合，開いてIDを割り当てるのみおこなう．
     * この場合，返却される{@code ServerResponse}はProviderIDを含んでいなくても構わない.
     *
     * @param manual is the 'manual connection' to Provider. Providerへの手動接続をおこなうかどうか．
     * @return {@code ServerResponse} includes information of opened Viewer's Id, and Provider's Id if it's connected.
     * @return 割り当てられたViewerのIDを含む {@code ServerResponse}. Providerに接続された場合はProviderのIDを含む.
     */
    @Override
    public ServerResponse requestOpenViewer(boolean manual)
    {
        // create new state
        int id = viewerStates.size();
        ViewerState state = new ViewerState(id);

        String msg = "/manager/viewer/open: Viewer Arrived (" + id + ")";
        synchronized (this) {
            // Connect with assign
            if (!manual) {
                // Search Online Processing Provider
                List<IEntityProvider> online = providers.values()
                        .stream().filter(IEntityProvider::isOnline)
                        .filter((a) -> !a.isClosed())
                        .collect(Collectors.toList());
                logger.debug("/manager/open/viewer: online provider "
                        + online.stream()
                        .map(IEntityProvider::getID).map(Object::toString)
                        .reduce((a, b) -> a + ", " + b).orElse(""));
                online.sort(Comparator.comparing(IEntityProvider::getID));
                if (!online.isEmpty()) {
                    state.connectProvider(online.get(0).getID());
                    msg += " with connection to Provider " + state.getProviderId();
                }
            }
            viewerStates.put(state.getViewerId(), state);
        }
        // Log
        logger.info(msg);

        // Create response, list of provider opened
        DefaultResponse.IDsResult result = (new DefaultResponse.IDsResult()).setViewerId(state.getViewerId());
        if (!manual) {
            result.setProviderId(state.getProviderId());
        }

        return new ServerResponse(ServerResponse.STATUS_OK, result);
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Provider Control
    // Providerの操作に関する処理
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /**
     * Open new Provider and connect to Kernel.
     * If the {@code port} is {@code null}, use default port (may be 7000).
     * With the reconnection required from <code>Provider.connect</code>
     *
     * 新しいProviderを開き，Kernelに接続する.
     * {@code port}が{@code null}のときは，標準ポート(7000)を使う．
     * <code>Provider.connect</code>に要求されるrecconectを受けつける．
     *
     * @param host hostname of kernel. カーネルのホスト名.
     * @param port port number of kernel. カーネルのポート番号.
     * @param reconnect is the reconnect mode. 再接続か．
     * @return {@code ServerResponse} contains a information of opened Provider.
     *   開かれたProviderの情報を持った{@code ServerResponse}.
     */
    private ServerResponse requestOpenProvider(String host, Integer port, boolean reconnect) {
        ServerResponse result;

        if (host == null) {
            result = new ServerResponse(ServerResponse.STATUS_BADREQ
                    , "hostname needed for Open Provider");
        }
        else {
            if (port == null) {
                port = Integer.parseInt(System.getProperty(
                            ViewerManagerKeys.KERNEL_PORT
                            , ViewerManagerKeys.KERNEL_DEFAULT_PORT));
            }

            try {
                // Open Provider
                RRSEntityProvider provider = new RRSEntityProvider();
                provider.connect(host, port, reconnect);

                // Resister Provider to List
                int id = 0;
                synchronized (this) {
                    id = numProvider;
                    provider.setID(id);
                    providers.put(provider.getID(), provider);
                    numProvider ++;
                }

                // Create Response
                Object res = new DefaultResponse.OnlineProviderResult(provider);
                result = new ServerResponse(ServerResponse.STATUS_ACCEPTED, res);
            } catch (IOException e) {
                // Failed to Open
                result = new ServerResponse(ServerResponse.STATUS_INTERNAL_ERROR
                        ,"Kernel Open Error");
            } // :try
        } // :else host != null

        return result;
    }

    /**
     * Open new Provider and connect to Kernel.
     * If the {@code port} is {@code null}, use default port (may be 7000).
     *
     * 新しいProviderを開き，Kernelに接続する.
     * {@code port}が{@code null}のときは，標準ポート(7000)を使う．
     *
     * @param host hostname of kernel. カーネルのホスト名.
     * @param port port number of kernel. カーネルのポート番号.
     * @return {@code ServerResponse} contains a information of opened Provider.
     *   開かれたProviderの情報を持った{@code ServerResponse}.
     */
    @Override
    public ServerResponse requestOpenProvider(String host, Integer port) {
        return requestOpenProvider(host, port, false);
    }

    /**
     * Close opened Provider.
     *
     * Providerを閉じる．
     *
     * @param providerId ID of Provider closing. 閉じるProviderのID.
     * @return {@code ServerResponse} contains a closed ProviderId.
     *  閉じられたProviderIdを含む{@code ServerResponse}.
     * */
    @Override
    public ServerResponse requestCloseProvider(int providerId) {
        // Operate Close Provider
        IEntityProvider provider = null;
        synchronized (this) {
            // Get Pull Provider
            if (providers.containsKey(providerId)) {
                provider = providers.get(providerId);
                if (provider != null) {
                    if (providerChain.containsKey(providerId)) {
                        providerChain.remove(providerId);
                    }
                    provider.shutdown();
                }
                providers.remove(providerId);
            }
        }

        ServerResponse response;
        if (provider == null) {
            // Provider is not available
            response = new ServerResponse(ServerResponse.STATUS_OK
                    , "Target Provider is not available"
                    , new DefaultResponse.IDsResult().setProviderId(providerId));
        }
        else {
            // Accept
            response = new ServerResponse(ServerResponse.STATUS_ACCEPTED
                    , new DefaultResponse.IDsResult().setProviderId(providerId));
        }

        return response;
    }


    /**
     * Open new Provider, and connect to Log File.
     *
     * 新しいProviderを開き，ログファイルに接続する.
     *
     * @param path the path of LogFile opening. 開くログファイルのパス.
     * @reutrn {@code ServerResponse} contains a opened Provider's information.
     *  開かれたProviderの情報を含む{@code ServerResponse}.
     * */    @Override
    public ServerResponse requestOpenLogProvider(String path)
    {
        if (path == null) {
            logger.info("/manager/open/provider/log: path null");
            return new ServerResponse(ServerResponse.STATUS_BADREQ,
                    "path cannot null");
        }
        File p = new File(path);
        ServerResponse response;

        logger.info("/manager/open/provider/log: path '" + path + "'");
        // is the filePath is File
        if (!p.isFile()) {
            response = new ServerResponse(ServerResponse.STATUS_BADREQ
                    , String.format("Invalid path - file not found: %s", path));
            logger.info("/manager/open/provider/log: file not found \"" + path + "\"");
        }
        else {
            // Open Log Provider
            ProviderLogReader logProvider = null;
            if (path.endsWith(".log")) {
                // File will be the RRS's Log File
                logProvider = new RRSLogReader();
                logger.info("/manager/open/provider/log: opened as RRS Log Provider");
            }
            else if (path.endsWith(".vlog"))
            {
                // File will be the Viewer Log File
                logProvider = new ViewerLogReader();
                logger.info("/manager/open/provider/log: opened as Viewer Log Provider");
            }
            else {
                // TO"DO: set unknown state
                // File will is unknown Format Log File
                // Keep logProvider == null;
                logger.info("/manager/open/provider/log: unknown file path");
            }

            // Open Log File and Register Provider
            if (logProvider != null) {
                try {
                    // Open File
                    logProvider.open(path);

                    // set provider id and register to list
                    int id = 0;
                    synchronized (this) {
                        id = numProvider;
                        logProvider.setID(id);
                        providers.put(id, logProvider);
                        numProvider++;
                    }

                    // Create Response
                    Object data = DefaultResponse.getProviderResult(logProvider);
                    response = new ServerResponse(ServerResponse.STATUS_ACCEPTED, data);
                } catch (LogFileException e) {
                    logger.error("/manager/open/provider/log: Failed to Open Log File", e);
                    if (Objects.equals(e.getReason(), LogFileException.FILE_IS_NOT_LOG)) {
                        response = new ServerResponse(ServerResponse.STATUS_BADREQ
                                , "Target path is not Log File");
                    }
                    else {
                        response = new ServerResponse(ServerResponse.STATUS_INTERNAL_ERROR
                                , "Failed to Open or Read Log File");
                    }
                } // :try
            } // :fi logProvider != null
            else {
                response = new ServerResponse(ServerResponse.STATUS_BADREQ);
                logger.error("/manager/open/provider/log: Unknown Error (maybe not supported format log)");
            }
        } // :else isFile available

        return response;
    }

    /**
     * Get list of Log files under the registered directory.
     *
     * 登録されたディレクトリ内に存在するログファイルの一覧を得る.
     *
     * @return {@code ServerResponse} contains a list of log files paths.
     *  ログファイルのパスの一覧を含んだ{@code ServerResponse}.
     */
    @Override
    public ServerResponse requestLogList()
    {
        String[] list = logFileDirectory.getFilePaths();

        return new ServerResponse(ServerResponse.STATUS_OK, new DefaultResponse.ListResult(list));
    }

    /**
     * Register directory that may contains log file.
     *
     * ログファイルを含むディレクトリを登録する.
     *
     * @param path path of log directory. ログのディレクトリ.
     * @return . TODO: determine this specifies;;
     * */
    @Override
    public ServerResponse requestAddLogPath(String path) {
        logFileDirectory.addPath(path);

        return new ServerResponse(ServerResponse.STATUS_ACCEPTED);
    }

    /**
     * Get list of directories of Log Files Path registered.
     *
     * 登録されたログファイルのディレクトリの一覧を得る.
     *
     * @return {@code ServerResponse} contains a list of log directory paths.
     *  ログファイルのディレクトリ一覧を含んだ{@code ServerResponse}.
     * */
    @Override
    public ServerResponse requestLogPaths() {
        String[] list = logFileDirectory.getFilePaths();

        return new ServerResponse(ServerResponse.STATUS_OK, new DefaultResponse.ListResult(list));
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Create and Close Connection
    // Connectionの作成と切断
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /**
     * Connect specified viewer and provider.
     *
     * 指定されたViewerとProviderを接続する.
     *
     * @param viewerId ID of Viewer connecting. 接続するViewerのID.
     * @param providerId ID of Provider connecting. 接続するProviderのID.
     * @return {@code ServerResponse} contains a information of connection. 接続の情報を含む{@code ServerResponse}.
     */
    @Override
    public ServerResponse requestConnection(int viewerId, int providerId) {
        // Request Make Connection

        // Get ViewerState
        ViewerState viewerState = null;
        IEntityProvider provider = null;
        Recorder recorder = null;
        ServerResponse response;
        synchronized (this) {
            // Get ViewerState from list
            if (viewerStates.containsKey(viewerId)) {
                // get viewer state
                viewerState = viewerStates.get(viewerId);
            }
            if (providers.containsKey(providerId)) {
                // get provider
                provider = providers.get(providerId);
            }
            if (viewerRecorders.containsKey(providerId)) {
                // get provider recorder
                recorder = viewerRecorders.get(providerId);
            }


            // Check viewer and provider available
            if (viewerState == null) {
                // Viewer is not available
                logger.warn("/manager/connection/open: requested viewer is not available");
                response = new ServerResponse(ServerResponse.STATUS_BADREQ
                        , "Viewer is not available");
            }
            else if (provider == null && recorder == null) {
                // Provider is not available
                logger.warn("/manager/connection/open: requested provider is not available");
                response = new ServerResponse(ServerResponse.STATUS_BADREQ
                        , "Provider is not available");
            }
            else {
                if (viewerState.getProviderId() == providerId) {
                    logger.warn("/manager/connection/open: requested connection is already satisfied");
                    response = new ServerResponse(ServerResponse.STATUS_OK);
                }
                else {
                    if (viewerState.getProviderId() > 0) {
                        // Already Connected
                        logger.warn("/manager/connection/open: requested connection with disconnection"
                                + String.format("{viewerId: %d, providerId: %d}", viewerState.getViewerId(), viewerState.getProviderId()));
                    }
                    // Connect to
                    // Register
                    viewerState.connectProvider(providerId);
                    viewerStates.put(viewerState.getViewerId(), viewerState);
                    logger.info(String.format("/manager/connection/open:" +
                                    " connection satisfied {viewerId: %d, providerId: %d}"
                            , viewerId, providerId));

                    // Create Response with inform
                    Object res = new DefaultResponse.IDsResult()
                            .setProviderId(providerId)
                            .setViewerId(viewerId);
                    response = new ServerResponse(ServerResponse.STATUS_OK, res);
                } // :providerId > 0
            }
        } // :end of synchronized

        return response;
    }

    /**
     * Disconnect specified viewer from connected provider
     *
     * 指定されたViewerをProviderから切断する.
     *
     * @param viewerId ID of Viewer disconnecting. 切断するViewerのID.
     * @return {@code ServerResponse} contains a information of connection disconnected.
     *  切断された接続の情報を含む {@code ServerResponse}.
     */
    @Override
    public ServerResponse requestDisconnect(int viewerId) {
        ServerResponse response;
        ViewerState viewerState = null;
        Integer providerId = null;

        synchronized (this) {
            if (viewerStates.containsKey(viewerId)) {
                viewerState = viewerStates.get(viewerId);
                // Set as Disconnected
                if (viewerState.isConnected()) {
                    providerId = viewerState.providerId;
                    viewerState.disconnectProvider();
                    viewerStates.put(viewerState.getViewerId(), viewerState);
                }
            }
        }

        if (viewerState == null) {
            response = new ServerResponse(ServerResponse.STATUS_BADREQ
                    , "Target Viewer is not available");
        }
        else if (providerId == null) {
            // Target Provider not found
            response = new ServerResponse(ServerResponse.STATUS_ACCEPTED
                    , new DefaultResponse.IDsResult().setViewerId(viewerState.getViewerId()));
        }
        else {
            // Disconnected relation the Viewer and the Provider
            response = new ServerResponse(ServerResponse.STATUS_OK
                    , new DefaultResponse.IDsResult()
                        .setProviderId(providerId)
                        .setViewerId(viewerState.getViewerId())
            );
        }

        return response;
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Get Lists
    // 一覧の取得
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /**
     * Return the list of Information includes Provider connected.
     * If no Provider connected, Return empty list.
     *
     * 現在接続されているすべてのProviderの情報を返す．
     * 1つのProviderも接続されていない場合，空のリストを返す．
     *
     * @return {@code ServerResponse} includes list of all Provider. すべてのProviderの情報を含んだ {@code ServerResponse}.
     */
    @Override
    public ServerResponse requestProviderList() {
        // Create message list of providers

        List<Object> information = new ArrayList<>();
        Integer chainedId = null;
        synchronized (this) {
            // get information of all provider
            for (IEntityProvider provider : providers.values()) {
                // if the provider chained, create information with chainedId
                if (providerChain.containsKey(provider.getID())) {
                    chainedId = providerChain.get(provider.getID());
                }

                information.add(DefaultResponse.getProviderResult(provider, chainedId));
                // getProviderResult(ProviderInfo, ChainedId);
                chainedId = null;
            }
        }
        // Return information listed
        return new ServerResponse(ServerResponse.STATUS_OK
                , new DefaultResponse.ListResult(information));
    }

    /**
     * Return the list of Information includes Viewer connected.
     * If no Viewer connected, Return empty list.
     *
     * 現在接続されているすべてのViewerの情報を返す．
     * 1つのViewerも接続されていない場合，空のリストを返す．
     *
     * @return {@code ServerResponse} includes list of all Viewers.
     *  すべてのViewerの情報を含んだ {@code ServerResponse}.
     */
    @Override
    public ServerResponse requestViewerList() {
        // List up all information of Viewer
        List<Object> information = new ArrayList<>();
        synchronized (this) {
            information.addAll(viewerStates.values());
        }

        // Return information listed
        return new ServerResponse(ServerResponse.STATUS_OK
                , new DefaultResponse.ListResult(information));
    }


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Close Server Request
    // Managerの終了要求
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /**
     * Finish the Manager.
     *
     * Managerを終了させる.
     *
     * @return ACCEPTED.
     * */
    @Override
    public ServerResponse requestCloseServer() {
        // Signal for Main Thread to end this server
        synchronized (this) {
            if (serverState != STATE_ENDED) {
                serverState = STATE_ENDING;
            }
            logger.info("/manager/close: Server will Shutdown");
        }

        //ACCEPTED
        return new ServerResponse(ServerResponse.STATUS_ACCEPTED);
    }


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Main Loop and Exit
    // 初期化から更新，終了まで(May be Threaded)
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /**
     * Start the Manager.
     *
     * Managerを起動する.
     * */
    @Override
    public void run() {
        /* Initial Section */
        synchronized (this) {
            /* Check is the server is not running yet */
            if (serverState != STATE_INITIAL) {
                return;
            }
        }
        /* Initialize Needed */
        // TODO: Implements main Initialization


        /* Start Main Loop */
        int state;
        synchronized (this) {
            /* Be state running */
            serverState = STATE_RUNNING;
            state = serverState;
            logger.info("/manager: started");
        }

        /* Main Loop */
        do {
            synchronized (this) {
                /* Check Provider Update */
                for (Integer providerId : providers.keySet()) {
                    IEntityProvider provider = providers.get(providerId);

                    if (provider.isClosed()) {
                        /* the provider still closed */
                        continue;
                    }

                    /* Check New Record */
                    if (provider.isIncomingRecordAvailable()) {
                        /* Recording */
                        logger.debug("/manager: incoming record available - " + provider.getID());
                        Recorder recorder = null;
                        if (!viewerRecorders.containsKey(providerId)) {
                            /* create new recorder and append it to list */
                            recorder = new DefaultRecorder(new MsgPackEncoder());
                            viewerRecorders.put(providerId, recorder);
                            logger.debug("/manager: new recorder created for - " + provider.getID());
                        }
                        else {
                            /* get recorder online */
                            recorder = viewerRecorders.get(providerId);
                        } // :fi providerId contains on recorders

                        /* Append Record to Recorder */
                        try {
                            while (provider.isIncomingRecordAvailable()) {
                                /* get record from list */
                                Record record = provider.getIncomingRecord();
                                if (record != null)
                                {
                                    // Set Config/Map on first Record
                                    if (recorder.size() == 0) {
                                        record.config = provider.getConfig();
                                        record.map = provider.getMap();
                                    }

                                    /* append to recorder */
                                    recorder.appendRecord(record);
                                }
                            }
                        } catch (EncodeException e) {
                            logger.error("Encoder Failed", e);
                        }
                    } // :fi incomingRecord available

                    /* Check is the provider working. */
                    if (!provider.isWorking()) {
                        /* Provider end of own job */
                        /* shutdown(close) provider */
                        provider.shutdown();

                        if (provider.isOnline()) {
                            // Request Re-connection and Add to Provider Chain
                            ServerResponse response =
                                    requestOpenProvider(provider.getRemoteHostName(), provider.getRemotePort(), true);

                            if (response != null && response.getData() instanceof DefaultResponse.OnlineProviderResult) {
                                DefaultResponse.OnlineProviderResult result
                                        = (DefaultResponse.OnlineProviderResult)response.getData();
                                providerChain.put(providerId , result.providerId);
                                logger.info("/manager: create chained provider => Provider " + result.providerId);
                            }
                        }

//                        providers.remove(providerId); /* remove from list */
                        logger.info("/manager: recording end => provider '" + providerId + "'");
                    }
                } // :rof each providers.keySet()

                /* check each viewer */
                // TODO: Implement?


            } // :end of Critical Section

            // Sleep a little
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                logger.info("Manager: Thread Sleep Interrupted");
            }

            // Check state for continue or quit loops
            synchronized (this) {
                state = serverState;
            }
        } while (state == STATE_RUNNING);


        // Release Resources
        synchronized (this) {
            providers.clear();
            providers = null;
            viewerStates.clear();
            viewerStates = null;
            viewerRecorders.clear();
            viewerRecorders = null;

            serverState = STATE_ENDED;
            logger.info("/manager: stop");
        } // :end of synchronized
    }

    /**
     * Stop all Provider, and Release all Resources.
     *
     * すべてのProviderを停止させ，リソースを開放する.
     * */
    @Override
    public void shutdown() {
        synchronized (this)
        {
            if (serverState != STATE_ENDED) {
                serverState = STATE_ENDING;
            }
        }
    }


    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    // Inner Helper Functions
    // ヘルパ関数
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /**
     * format viewer's information as pretty printable
     * Viewerの情報を見やすく印字する
     * */
    private String formatViewer(ViewerState viewerState)
    {
        if (viewerState == null) {
            return "{}";
        }

        return String.format("{" +
                        "\"id\": %d" +
                        ", \"timeStep\": %d" +
                        ", \"timeStamp\": %d" +
                        ", \"status\": \"%s\"" +
                        ", \"providerId\": %s" +
                        "}"
                , viewerState.getViewerId()
                , viewerState.getTimeStep()
                , viewerState.getTimeStamp()
                , viewerState.getStatus()
                , (viewerState.isConnected()) ? "" + viewerState.getProviderId() : "null");
    }
}
