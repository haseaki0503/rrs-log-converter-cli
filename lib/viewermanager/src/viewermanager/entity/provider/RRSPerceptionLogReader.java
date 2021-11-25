package viewermanager.entity.provider;

import org.apache.log4j.Logger;
import rescuecore2.log.LogException;
import rescuecore2.log.PerceptionRecord;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;
import viewermanager.ViewerManagerKeys;
import viewermanager.entity.Action;
import viewermanager.entity.Perception;
import viewermanager.entity.Record;

import java.util.*;
import java.util.stream.Collectors;

public class RRSPerceptionLogReader extends RRSLogReader {


    static final List<StandardEntityURN> URNs = Arrays.asList(
            StandardEntityURN.AMBULANCE_TEAM,
            StandardEntityURN.AMBULANCE_CENTRE,
            StandardEntityURN.FIRE_BRIGADE,
            StandardEntityURN.FIRE_STATION,
            StandardEntityURN.POLICE_FORCE,
            StandardEntityURN.POLICE_OFFICE);


    @Override
    protected Record readLog(int time) {
        Record record = super.readLog(time);

        if(record == null) {
            return record;
        }

        // Make Perception by PerceptionRecord of RRS
        HashMap<Integer, Perception> perceptions = new HashMap<>();
        try {
            // Get WorldModel
            WorldModel<? extends StandardEntity> worldModel
                    = (WorldModel<? extends StandardEntity>) reader.getWorldModel(time);
            if(worldModel != null) {
                // for all Entities
                Set<EntityID> entitiesWithUpdates = reader.getEntitiesWithUpdates(time);
                for (EntityID entity : entitiesWithUpdates) {
                    // If Entity is Agent
                    if(URNs.contains(worldModel.getEntity(entity).getStandardURN())) {
                        // Get Perceptions
                        PerceptionRecord perceptionRecord = reader.getPerception(time, entity);
                        Perception perception = new Perception();
                        perception.id = perceptionRecord.getEntityID().getValue();
                        perception.updated = perceptionRecord.getChangeSet()
                                .getChangedEntities()
                                .stream().map(EntityID::getValue)
                                .collect(Collectors.toList());
                        perception.deleted = perceptionRecord.getChangeSet()
                                .getDeletedEntities()
                                .stream().map(EntityID::getValue)
                                .collect(Collectors.toList());
                        perception.heard = perceptionRecord.getHearing()
                                .stream().map(Action::new)
                                .collect(Collectors.toList());
                        perceptions.put(perception.id, perception);
                    }
                }
            }
        } catch (LogException e) {
            // Some Error Occurred
            Logger logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
            if (Objects.isNull(logger)) {
                logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
            }
            logger.warn("RRSPerceptionLogReader, Reading Log", e);
        }

        record.perceptions = perceptions;

        return record;
    }
}
