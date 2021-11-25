package viewermanager.entity.provider;


import viewermanager.entity.MapInfo;
import viewermanager.entity.Record;

import java.util.Map;

/**
 * EntityProvider no data providing.
 * 何も提供しないEntityProvider
 * */
public class DummyProvider implements IEntityProvider {

    private Integer id;

    public DummyProvider() {
        this.id = null;
    }

    @Override
    public void shutdown() {
        // Nothing to do
    }

    @Override
    public Integer getID() {
        return id;
    }

    @Override
    public void setID(Integer id) {
        this.id = id;
    }

    @Override
    public String getProviderIDString() {
        return "Dummy Provider " + id;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public String getRemoteHostName() {
        return null;
    }

    @Override
    public Integer getRemotePort() {
        return null;
    }

    @Override
    public String getStatus() {
        return IEntityProvider.PROVIDER_END_WORKING;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isWorking() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    @Override
    public Integer getCurrentTimeStep() {
        return 0;
    }

    @Override
    public Integer getMaxTimeSteps() {
        return 0;
    }

    @Override
    public MapInfo getMap() {
        return null;
    }

    @Override
    public Map<String, String> getConfig() {
        return null;
    }

    @Override
    public boolean isIncomingRecordAvailable() {
        return false;
    }

    @Override
    public Record getIncomingRecord() {
        return null;
    }
}
