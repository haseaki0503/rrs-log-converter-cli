import org.junit.Test;
import viewermanager.ViewerManagerKeys;
import viewermanager.entity.Record;
import viewermanager.entity.provider.RRSEntityProvider;
import viewermanager.entity.provider.RRSLogReader;
import viewermanager.entity.provider.ViewerLogReader;

public class ProviderTest {

    @Test
    public void RRSProviderTest() throws Exception {
        System.setProperty(ViewerManagerKeys.VIEWER_KERNEL_WAIT_TIME, "100");
        RRSEntityProvider provider = new RRSEntityProvider();
        provider.connect("localhost", 7000);

        boolean closed = provider.isClosed();
        int count = 0;
        do {
            if (provider.isIncomingRecordAvailable()) {
                System.out.println("\n\n[Connected]\n");
                break;
            }
            System.out.print(".");
            count += 1;
            if (count > 100) {
                count = 0;
                System.out.println();
            }

            closed = provider.isClosed();
            Thread.sleep(10);
        } while(!closed);

        System.out.println("Is Closed: " + closed);
    }

    @Test
    public void RRSLogProviderTest() throws Exception
    {
        RRSLogReader reader = new RRSLogReader();
        reader.open("/home/yuma/Downloads/joao2_MRL.log");
        int max = reader.getMaxTimeSteps();
        System.out.println(max + " : Records Reading");

        do {
            if (reader.isIncomingRecordAvailable()) {
                Record record = reader.getIncomingRecord();
                System.out.println(record.time + " : Incoming + " + reader.getStatus());
            }
        } while(reader.isWorking());
    }

    @Test
    public void ViewerLogProviderTest() throws Exception
    {
        ViewerLogReader reader = new ViewerLogReader();
        reader.open("/home/yuma/dev/robocup/ViewerManager/logs/paris.vlog");
        int max = reader.getMaxTimeSteps();
        System.out.println(max + " : Records Reading");

        do {
            if (reader.isIncomingRecordAvailable()) {
                Record record = reader.getIncomingRecord();
                System.out.println(record.time + " : Incoming + "
                    + reader.getStatus() + "(" + reader.getCurrentTimeStep() + ")");
            }
        } while(reader.isWorking());
    }

}
