package viewermanager.manager;


import viewermanager.entity.provider.IEntityProvider;

import java.nio.ByteBuffer;
import java.util.List;

/*****
 * Default Response - Helper Classes to express results on <code>DefaultManager</code>
 * <code>DefaultManager</code>の結果を示すためのヘルパクラス
 * */
public class DefaultResponse {

    /**
     * To return Records. Recordを返すため
     * */
    public static class RecordResult {
        /**
         * 圧縮されたRecordのリスト
         * */
        public ByteBuffer[] record;
        /**
         * Viewerが再接続されたか
         * */
        public boolean reconnected;

        public RecordResult(ByteBuffer[] records) {
            this.record = records;
        }

        public RecordResult(List<ByteBuffer> records) {
            this.record = records.toArray(new ByteBuffer[0]);
        }

        public RecordResult(boolean reconnected) {
            this.reconnected = true;
        }
    }

    /**
     * To return ID pair. IDの組を返すため
     *
     * Usage:
     *  (new IDsResult()).setViewerId(0).setProviderId(0);
     * */
    public static class IDsResult {
        /**
         * Viewer ID
         * */
        public Integer viewerId;

        /**
         * Provider ID
         * */
        public Integer providerId;

        public IDsResult setViewerId(int id) {
            this.viewerId = id;
            return this;
        }
        public IDsResult setProviderId(int id) {
            this.providerId = id;
            return this;
        }
    }

    /**
     * auto creation <code>OnlineProviderResult</code> or <code>OfflineProviderResult</code> by given <code>IEntityProvider</code>
     * <code>IEntityProvider</code>から<code>OnlineProviderResult</code>/<code>OfflineProviderResult</code>を自動選択
     * */
    public static Object getProviderResult(IEntityProvider provider) {
        return getProviderResult(provider, null);
    }

    /**
     * auto creation <code>OnlineProviderResult</code> or <code>OfflineProviderResult</code> by given <code>IEntityProvider</code>
     * <code>IEntityProvider</code>から<code>OnlineProviderResult</code>/<code>OfflineProviderResult</code>を自動選択
     * */
    public static Object getProviderResult(IEntityProvider provider, Integer chainedId)
    {
        if (provider.isOnline()) {
            return new OnlineProviderResult(provider, chainedId);
        }
        else {
            return new OfflineProviderResult(provider);
        }
    }

    /**
     * To expression about Online Provider. Online Providerを表現するため
     * */
    public static class OnlineProviderResult {
        /** from IEntityProvider */
        public Integer providerId;
        /** ditto */
        public String status;
        /** ditto */
        public Boolean online;
        /** ditto */
        public String host;
        /** ditto */
        public Integer port;
        /** ditto */
        public Integer timeStep;
        /** ditto */
        public Integer maxTimeStep;
        /**
         * Provider ID that connect to the Viewer autonomous.
         * 次にViewerに接続されるProviderのID
         * */
        public Integer chainedProviderId;

        public OnlineProviderResult(IEntityProvider provider) {
            this.providerId = provider.getID();
            this.status = provider.getStatus();
            this.online = true;
            this.host = provider.getRemoteHostName();
            this.port = provider.getRemotePort();
            this.timeStep = provider.getCurrentTimeStep();
            this.maxTimeStep = provider.getMaxTimeSteps();
            this.chainedProviderId = null;
        }

        public OnlineProviderResult(IEntityProvider provider, Integer chainedId) {
            this(provider);
            this.chainedProviderId = chainedId;
        }
    }

    /**
     * To expression about Offline Provider. Offline Providerを表現するため
     * */
    public static class OfflineProviderResult {
        /** from IEntityProvider */
        public Integer providerId;
        /** ditto */
        public String status;
        /** ditto */
        public Boolean online;
        /** ditto */
        public String IDString;
        /** ditto */
        public Integer timeStep;
        /** ditto */
        public Integer maxTimeStep;

        public OfflineProviderResult(IEntityProvider provider) {
            this.providerId = provider.getID();
            this.status = provider.getStatus();
            this.online = false;
            this.IDString = provider.getProviderIDString();
            this.timeStep = provider.getCurrentTimeStep();
            this.maxTimeStep = provider.getMaxTimeSteps();
        }
    }

    /**
     * To Express lists. リストを表現するため
     * */
    public static class ListResult {
        public Object[] list;

        public ListResult(Object[] list) {
            this.list = list;
        }

        public ListResult(List<Object> list) {
            this.list = list.toArray();
        }
    }


}
