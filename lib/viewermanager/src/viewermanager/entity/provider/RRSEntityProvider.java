package viewermanager.entity.provider;


import org.apache.log4j.Logger;
import rescuecore2.Timestep;
import rescuecore2.components.*;
import rescuecore2.config.Config;
import rescuecore2.connection.Connection;
import rescuecore2.connection.ConnectionException;
import rescuecore2.messages.control.KVTimestep;
import rescuecore2.misc.java.LoadableTypeProcessor;
import rescuecore2.registry.Registry;
import rescuecore2.score.ScoreFunction;
import rescuecore2.standard.components.StandardViewer;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.WorldModel;
import viewermanager.ViewerManagerKeys;
import viewermanager.entity.*;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * RoboCupRescue Entity Provider
 *
 * Make a connection with RRS Kernel as Viewer, and provides received worldmodel as <code>Record</code>
 * RRSのカーネルにViewerとして接続し，受け取ったWorldModelを<code>Record</code>として提供するProviderです．
 * */
public class RRSEntityProvider implements IEntityProvider {

    /**
     * Map Information of current simulation.
     * 現在のシミュレーションでのマップ情報
     * */
    private MapInfo map;

    /**
     * Configs of current simulation.
     * 現在のシミュレーションでのコンフィグ
     * */
    private Map<String, String> config;

    /**
     * Queue of <code>Record</code>s received and transformed.
     * 受信して変換した<code>Record</code>を保持するキュー
     * */
    private List<Record> records;

    /**
     * Component Instance of RRS Viewer
     * RRS Viewerのコンポーネントインスタンス
     * */
    private Component viewerComponent;

    /**
     * simulation's current time step
     * シミュレーションの現在のタイムステップ
     * */
    private Integer time;

    /**
     * simulations's max time step
     * シミュレーションの最大タイムステップ
     * */
    private Integer maxTimeStep;

    /**
     * hostName of Kernel used on connecting
     * 接続時に指定したKernelのホスト名
     * */
    private String hostName;

    /**
     * port number of Kernel used on connecting
     * 接続時に指定したKernelのポート番号
     * */
    private int port;

    /**
     * Provider's ID
     * 自身のProviderのID
     * */
    private int id;

    /**
     * current status of Provider
     * 現在の状況
     * */
    private String status;

    /**
     * Future that connect Kernel finished.
     * カーネルへの接続が終了したことを示すFuture
     * */
    private Future<Boolean> _futureOpenProvider;
    private Logger logger;

    public RRSEntityProvider()
    {
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (Objects.isNull(logger)) {
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }

        map = null;
        config = null;
        records = null;
        viewerComponent = null;
        time = null;
        maxTimeStep = null;
        hostName = null;
        port = 0;
        id = 0;
        status = IEntityProvider.PROVIDER_NOT_CONNECTED;
        _futureOpenProvider = null;
    }


    public RRSEntityProvider(String hostName, int port) throws IOException
    {
        this();
        connect(hostName, port);
    }

    /**
     * Read RRS's Jar Files and collect register entry used in rescuecore2;
     * RRSのJarファイルを読み込み，rescuecore2で使われる情報を収集する
     * */
    private void processJarFiles(Config config) throws IOException
    {
        LoadableTypeProcessor pr = new LoadableTypeProcessor(config);
        pr.addFactoryRegisterCallbacks(Registry.SYSTEM_REGISTRY);
        pr.process();
    }

    /**
     * Lambda Method for make a connection with Kernel : used in <code>connect</code>
     * <code>connect</code>メソッドで使われる，Kernelとの接続を作成する関数を返す．
     *
     * @return lambda to make a connection with Kernel; Kernelとの接続用に使う関数
     * */
    private Callable<Boolean> processOpenProvider(ComponentLauncher launcher, int kernelWait) {
        return () -> {
            Component vComponent = null;
            logger.info("/rrs/provider: Connection Started");

            /* Get time awaiting for re-try */
            int waitfor = 0;
            int count = 0;
            try {
                waitfor = Integer.parseInt(System.getProperty(ViewerManagerKeys.VIEWER_KERNEL_WAIT_TIME
                        , ViewerManagerKeys.VIEWER_KERNEL_WAIT_DEFAULT_TIME));
            } catch (NumberFormatException ex) {
                logger.error("/RRSEntityProvider/connect?connectThread: cannot parse waiting time, use 100 ms", ex);
                waitfor = 100; /* */
            }

            /* re-try count */
            try {
                count = Integer.parseInt(System.getProperty(ViewerManagerKeys.VIEWER_KERNEL_WAIT_COUNT
                        , ViewerManagerKeys.VIEWER_KERNEL_WAIT_DEFAULT_COUNT));
            }
            catch (NumberFormatException ex) {
                logger.error("/RRSEntityProvider/connect?connectThread: cannot parse waiting count, use 100 times", ex);
                count = 100;
            }

            logger.debug(String.format("/RRSEntityProvider/connect?connectThread: {wait: %d, count: %d}", waitfor, count));

            // if re-connection: wait for ends-up and restart the kernel
            try {
                Thread.sleep(kernelWait);
            }
            catch (InterruptedException e) {
                logger.warn("/RRSEntityProvider/connect?connectThread: interrupted on waiting retry");
                count = 0;
            }

            /* try to connect */
            for (int i=0; i<count; i++) {
                try {
                    /* Create Component and try to connect */
                    vComponent = new InnerStandardViewer();
                    launcher.connect(vComponent); /* try to connect */

                    /* connection satisfied */
                    viewerComponent = vComponent;
                    break;
                } catch (Exception ex) {
                    logger.debug("/RRSEntityProvider/connect?connectThread: cannot connect to provider("
                            + hostName + ", " + port + ") " + i + "th times.");

                    /* Wait for 'Retry' */
                    try {
                        Thread.sleep(waitfor);
                    } catch (InterruptedException e) {
                        logger.warn("/RRSEntityProvider/connect?connectThread: interrupted on waiting retry");
                        break;
                    }
                    /* re-try */
                }
            } // rof

            logger.trace("/RRSEntityProvider/connect?connectThread: exit thread");
            if (viewerComponent == null) {
                /* cannot make a connection  */
                status = IEntityProvider.PROVIDER_END_WORKING;
                logger.info("/RRSEntityProvider/connect?connectThread: cannot connect to server.");
            }
            return viewerComponent != null; /* viewerComponent is not null when succeeded */
        };
    }

    /**
     * make a connection with Kernel
     * カーネルへの接続をおこなう
     *
     * @param hostName target hostName. 対象のホスト名
     * @param port target host port number. 対象のポート番号
     * @exception IOException, if connection failed with unhandled reasons.
     *  内部で処理されない理由の場合，IOExceptionは発生する可能性がある
     * */
    public void connect(String hostName, int port) throws IOException
    {
        connect(hostName, port, false);
    }

    /**
     * make a connection with Kernel with re-connection flag:
     * 再接続時のフラグを考慮して，カーネルへの接続をおこなう
     *
     * @param hostName target hostName. 対象のホスト名
     * @param port target host port number. 対象のポート番号
     * @param reconnect is the re-connect mode. 再接続モードか
     * @exception IOException, if connection failed with unhandled reasons.
     *  内部で処理されない理由の場合，IOExceptionは発生する可能性がある
     * */
    public void connect(String hostName, int port, boolean reconnect) throws IOException {
        Config config = new Config();

        /* お ま じ な い */
        /* load Properties from RRS Jars */
        config.setValue("loadabletypes.inspect.dir", "library/rescue");
        try {
            processJarFiles(config);
        } catch (IOException e) {
            logger.error("/RRSEntityProvider/connect: cannot find Jar Files", e);
            throw e;
        }
        ComponentLauncher launcher = new TCPComponentLauncher(hostName, port, config);
        logger.info("/rrs/provider: Connection Created to " + hostName + ":" + port);

        this.hostName = hostName;
        this.port = port;

        /* really make a connection with Kernel */
        ExecutorService executor = Executors.newSingleThreadExecutor();
        /* if the re-connect mode, wait for 10000 milliseconds before start connection */
        // TODO: Check the correct time for waiting before connection.
        _futureOpenProvider = executor.submit(processOpenProvider(launcher, (reconnect) ? 10000 : 0));

        // Preparing List as Asynchronous
        records = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * append transformed <code>Record</code> to the queue.
     * 変換済みの<code>Record</code>をキューに追加する．
     *
     * @param record transformed <code>Record</code>. 変換済みの<code>Record</code>
     * */
    private void appendRecord(Record record)
    {
        /* record is able to register */
        if(records != null && record != null)
        {
            /* set information keeping on Provider */
            records.add(record); /* register to list */
            this.time = record.time; /* update current time */

            /* end working when time is over */
            if (this.time >= this.maxTimeStep) {
                logger.debug("/rrs/provider: reach to maxTimeStep, Viewer'll Closed");
            }

            logger.debug("/rrs/provider: record append - " + records.size() +  " stacked");
        }
    }

    /**
     * set Initial Information like Map, Config, and World of Start time
     * 初期化として，MapやConfig，初期のWorldModelを取得，設定する．
     *
     * @param worldModel WorldModel on started time. 初期のWorld
     * @param config Config.
    * */
    private void setInformation(WorldModel<? extends StandardEntity> worldModel, Config config)
    {
        // In Default, this method called by InnerStandardViewer::postConnect
        if(worldModel == null || config == null) {
            return;
        }

        synchronized (this) {
            // Get Map Information
            MapInfo mapInfo = new MapInfo();
            Rectangle2D bounds = ((StandardWorldModel) worldModel).getBounds();
            mapInfo.width = bounds.getWidth();
            mapInfo.height = bounds.getHeight();

            /* transform all Area to AreaInfo, and collect as List */
            mapInfo.entities = worldModel.getAllEntities()
                    .stream()
                    .filter(standardEntity -> standardEntity instanceof Area)
                    .map(standardEntity -> new AreaInfo((Area) standardEntity))
                    .collect(Collectors.toList());

            // Set MapInfo
            this.map = mapInfo;

            // Set Config
            this.config = new HashMap<>();
            config.getAllKeys().forEach(a -> this.config.put(a, config.getValue(a)));

            // get MaxTimeStep
            String timestep = config.getValue("kernel.timesteps", "300");
            this.maxTimeStep = Integer.parseInt(timestep);

            // Start working
            this.status = IEntityProvider.PROVIDER_WORKING;
        }
        Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER).info("/rrs/provider: Information Set.");
    }


    //--[Interfaces: IEntityProvider]-------------------------------------------------------------------

    /**
     * check is this provider connected to any system.
     * このProviderがシステムに接続されているかを示す．
     *
     * Condition: viewerComponent != null (connection satisfied), and status is not END_WORKING.
     * */
    @Override
    public boolean isConnected() {
        return viewerComponent != null && !status.equals(IEntityProvider.PROVIDER_END_WORKING);
    }

    /**
     * check is this provider working -- connecting, reading, transforming, and providing.
     *  this means this provider 'is not finished.'
     * このProviderが仕事中(接続中，読み込み中，変換中，提供中)か確かめる．
     *  これはこのProviderが「終了していない」ことを意味する．
     *
     * Condition: status is NOT_CONNECTED (before working), or WORKING, or RRS Viewer living.
     * */
    @Override
    public boolean isWorking() {
        return Objects.equals(status, IEntityProvider.PROVIDER_NOT_CONNECTED)
                || Objects.equals(status, IEntityProvider.PROVIDER_WORKING)
                || isAlive();
    }

    /**
     * check is this provider end up of working -- so no data incoming in future.
     * このProviderがもう仕事しない(以降どのようなデータも提供しない)かを確かめる．
     *
     * Condition: is status END_WORKING
     * */
    @Override
    public boolean isClosed() {
        // info set at connection opened,
        //  and viewerComponent to be null on closed
        return status.equals(IEntityProvider.PROVIDER_END_WORKING);
    }

    /**
     * check is the RRS Viewer Living.
     * RRS Viewerがまだ有効かしらべる
     * */
    private boolean isAlive() {
        return (viewerComponent != null) && ((InnerStandardViewer)viewerComponent).isAlive();
    }

    /**
     * Shutdown all system and release resource.
     * すべてを終了して資源を開放する．
     * */
    @Override
    public void shutdown()
    {
        // if the connecting
        if (_futureOpenProvider != null) {
            // cancel connection
            _futureOpenProvider.cancel(true);
        }
        _futureOpenProvider = null;

        // if the RRS Viewer Living
        if (viewerComponent != null) {
            // Close RRS Viewer
            viewerComponent.shutdown();
        }
        viewerComponent = null;

        // set status END_WORKING
        status = IEntityProvider.PROVIDER_END_WORKING;
    }


    /**
     * Get the last record's time step.
     * 現在読み出した最後のレコードのタイムステップを取得する．
     * */
    @Override
    public Integer getCurrentTimeStep() {
        return time;
    }

    /**
     * Get the max steps of connected system.
     * 接続したシステムから提供される最大タイムステップを取得する．
     * */
    @Override
    public Integer getMaxTimeSteps() {
        return maxTimeStep;
    }

    /**
     * Get the Map information are given by the system.
     * 与えられたマップ情報を取得する．
     * */
    @Override
    public MapInfo getMap() {
        return map;
    }

    /**
     * Get the configs are given by the system.
     * 与えられたコンフィグ情報を取得する．
     * */
    @Override
    public Map<String, String> getConfig() {
        return config;
    }

    /**
     * get the incoming record.
     * 新しいレコードを取得する．
     *
     * @return new record.
     * @apiNote check is the record available with <code>isIncomingRecordAvailable</code> before call this.
     * この関数を呼び出す前に<code>isIncomingRecordAvailable</code>で新しいレコードがあるか確かめてください．
     * */
    @Override
    public boolean isIncomingRecordAvailable() {
        boolean result = false;

        if (_futureOpenProvider != null) { // Connecting
            // Try check connection is Done
            try {
                // throw exceptions when error occurred
                if (_futureOpenProvider.isDone()) {
                    result = _futureOpenProvider.get();
                    logger.info("/RRSEntityProvider/isIncomingRecord/IsProviderOpened: " +
                            "connection satisfied (" + hostName + "," + port + ")");

                    _futureOpenProvider = null;
                }
            } catch (InterruptedException e) {
                logger.error("/RRSEntityProvider/isIncomingRecord/IsProviderOpened: Opening provider cancelled"
                        , e);
                status = IEntityProvider.PROVIDER_END_WORKING;
                _futureOpenProvider = null;
            } catch (ExecutionException e) {
                logger.error("/RRSEntityProvider/isIncomingRecord/IsProviderOpened: Cannot resolve provider correctly"
                        , e);
                status = IEntityProvider.PROVIDER_END_WORKING;
                _futureOpenProvider = null;
            }
        }
        else { // Communicating
            // Check Viewer State
            if (viewerComponent != null && viewerComponent instanceof InnerStandardViewer) {
                InnerStandardViewer v = (InnerStandardViewer) viewerComponent;
                if (!v.isAlive()) {
                    // If the RRS Viewer Stepped, this Provider will be finished.
                    this.status = IEntityProvider.PROVIDER_END_WORKING;
                }
            }

            /* is the records registered, so not empty */
            result = !(records == null || records.isEmpty());
        } // fi

        return result;
    }

    /**
     * get the incoming record.
     * 新しいレコードを取得する．
     *
     * @return new record.
     * @apiNote check is the record available with <code>isIncomingRecordAvailable</code> before call this.
     * この関数を呼び出す前に<code>isIncomingRecordAvailable</code>で新しいレコードがあるか確かめてください．
     * */
    @Override
    public Record getIncomingRecord() {
        // If the record available
        if (!isIncomingRecordAvailable()) {
            return null;
        }

        // get Record top of queue
        Record rec = records.remove(0);
        logger.info("rrs/provider/viewer: return a Record, " + records.size() + " Records left");
        return rec;
    }


    //--[Implement: InnerClasses]--------------------------------------------------------------------
    /**
     * get this provider's id
     * このProviderのIDを取得する．
     * */
    @Override
    public Integer getID() {
        return id;
    }

    /**
     * set this provider's id
     * このProviderのIDを設定する．
     * */
    @Override
    public void setID(Integer id) {
        this.id = id;
    }

    /**
     * get this provider's identification string.
     * このProviderを示す一意な文字列を返す．
     *
     * TODO: Current Implementation will be not unique
     * */
    @Override
    public String getProviderIDString() {
        InetSocketAddress address = new InetSocketAddress(hostName, port);
        return address.toString();
    }

    /**
     * check is this provider working with online, so Networking.
     * このProviderがネットワーク越しのようなオンライン状態で仕事するものかを示す．
     * */
    @Override
    public boolean isOnline() {
        return true;
    }

    /**
     * get remote host name if this provider working with network.
     * ネットワーク越しに動いている場合，相手のホスト名を取得する．
     *
     * @return Remote Host Name if this provider working with networking, else must be <code>null</code>.
     * */
    @Override
    public String getRemoteHostName() {
        return hostName;
    }

    /**
     * get remote host port number if this provider working with network.
     * ネットワーク越しに動いている場合，相手のポート番号を取得する．
     *
     * @return Remote Host port number if this provider working with networking, else must be <code>null</code>.
     * */
    @Override
    public Integer getRemotePort() {
        return port;
    }

    /**
     * Get Server's Status String.
     * サーバの状況を文字列として取得する．
     *
     * @return Defined as const String in <code>IEntityProvider</code>.
     * */
    @Override
    public String getStatus() {
        return status;
    }


    //-----------------------------------------------------------------------------
    // Viewer EndPoint with Kernel to Read Data and Set to Provider
    //-----------------------------------------------------------------------------

    /**
     * Inner Standard Viewer - Implementation of RRS Viewer for Providing <code>Records</code>
     * RRS Viewerの<code>Record</code>提供のための実装
     * */
    private class InnerStandardViewer extends StandardViewer {

        /**
         * Transformed  WorldInfo
         * */
        Map<Integer, Entity> world;

        /**
         * Function that calculate RRS Standard Score
         * */
        ScoreFunction scoreFunction;

        /**
         * Connection of RRS Modules
         * RRSの通信接続用インスタンス
         * */
        Connection connection;

        Logger logger;

        InnerStandardViewer() {
            super();

            logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
            if (Objects.isNull(logger)) {
                logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
            }
            world = new HashMap<>();

            logger.debug("/rrs/provider/viewer: Viewer Instantiate");
        }

        /**
         * get is the connection alive.
         * 通信が有効かを確かめる
         * */
        public boolean isAlive() {
            return this.connection != null && connection.isAlive();
        }

        /**
         * RRS Kernelへ接続する (used by rescuecore2)
         * */
        @Override
        public void connect(Connection connection, RequestIDGenerator generator, Config config) throws ConnectionException, ComponentConnectionException, InterruptedException {
            super.connect(connection, generator, config);
            this.connection = connection;
        }

        /**
         * Be called on Connection satisfied.
         * 通信が確立された後に呼び出される
         * */
        @Override
        protected void postConnect() {
            super.postConnect();

            logger.debug("/rrs/provider/viewer: postConnect");

            // create score function
            String scoreFunctionName = config.getValue(rescuecore2.Constants.SCORE_FUNCTION_KEY);
            scoreFunction = rescuecore2.misc.java.JavaTools.instantiate(scoreFunctionName, ScoreFunction.class);
            if(scoreFunction != null) {
                scoreFunction.initialise(model, config);
            }

            // Set Information of Provider
            setInformation(this.model, this.config);
        }

        /**
         * update of each steps.
         * それぞれのステップでの情報の更新
         * */
        @Override
        protected void handleTimestep(KVTimestep timeStep) {
            super.handleTimestep(timeStep);

            // DEBUG: Messages
            logger.debug("rrs/provider/viewer: handleTimeStep");

            // Receive WorldModel
            Map<Integer, Entity> entityWorld = new HashMap<>();
            for (StandardEntity standardEntity : this.model) {
                entityWorld.put(standardEntity.getID().getValue(), new Entity(standardEntity));
            }

            // Receive Entities
            List<Entity> entityUpdate = new ArrayList<>();
            timeStep.getChangeSet().getDeletedEntities().forEach(a ->
            {
                Entity e = new Entity();
                e.id = a.getValue();
                e.deleted = true;
                entityUpdate.add(e);
            }
            );

            timeStep.getChangeSet().getChangedEntities().forEach(a ->
            {
                Entity older = world.get(a.getValue());
                Entity newer = entityWorld.get(a.getValue());

                Entity entity = null;
                if (older != null) {
                    entity = Entity.difference(older, newer);
                } else {
                    newer.created = true;
                    entity = newer;
                }

                if(entity != null && !entity.isEmpty()) {
                    entityUpdate.add(entity);
                }
            });

            // Receive Commands
            List<Action> actions = timeStep.getCommands()
                    .stream()
                    .map(Action::new)
                    .collect(Collectors.toList());

            // Create Record to Append
            Record record = new Record();
            record.time = timeStep.getTime();
            record.world = new ArrayList<>(entityWorld.values());
            record.changes = entityUpdate;
            record.commands = actions;


            // Save Score
            if(scoreFunction != null) {
                record.score = scoreFunction.score(model, new Timestep(timeStep.getTime()));
            }

            // Append New Record to Provider
            appendRecord(record);
            this.world = entityWorld;

        }
    }
}
