import org.junit.Test;
import rescuecore2.config.Config;
import rescuecore2.log.FileLogReader;
import rescuecore2.log.LogReader;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.log.UpdatesRecord;
import rescuecore2.misc.java.LoadableTypeProcessor;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class RRSLogReaderTest {

    @Test
    public void LogReaderTest() throws Exception {
        String fileName = "/home/yuma/Downloads/joao2_MRL.log";
        Config config = new Config();
        // Open Reader File
        config.setValue("loadabletypes.inspect.dir", "library/rescue");
        LoadableTypeProcessor pr = new LoadableTypeProcessor(config);
        pr.addFactoryRegisterCallbacks(Registry.SYSTEM_REGISTRY);
        pr.process();

        LogReader reader = new FileLogReader(fileName, Registry.SYSTEM_REGISTRY);

        int maxTimestep = reader.getMaxTimestep();
        for (int i = 0; i <= maxTimestep+1; i++) {
        }
    }

}
