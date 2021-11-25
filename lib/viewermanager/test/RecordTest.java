import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.msgpack.core.ExtensionTypeHeader;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;
import viewermanager.encoder.EncodeException;
import viewermanager.encoder.Encoder;
import viewermanager.entity.Entity;
import viewermanager.entity.Record;
import viewermanager.recorder.DefaultRecorder;
import viewermanager.recorder.MsgPackRecorder;
import viewermanager.recorder.Recorder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecordTest {
    @Test
    public void EncodeTest() throws EncodeException {
        Record record = new Record();
        record.time = 1;
        record.world = Arrays.asList(new Entity(), new Entity(), new Entity());

        {
            record.world.get(0).id = 1;
            record.world.get(0).hp = 100;
            record.world.get(0).travel = 1000;

            record.world.get(1).id = 2;
            record.world.get(1).created = true;
            record.world.get(1).broken = 99;

            record.world.get(2).id = 3;
            record.world.get(2).type = "type";
        }

        Encoder encoder = new TestEncoder();
        ByteBuffer buffer = encoder.encodeRecord(record);
        String str = new String(buffer.array());
        System.out.println(str);
    }

    @Test
    public void RecordTest() throws EncodeException {
        List<Record> recordList = new ArrayList<>();

        // Create Record
        {
            Record record = new Record();
            record.time = 1;
            record.world = new ArrayList<>();
            record.score = 10.0;

            Entity e = new Entity();
            e.id = 1;
            e.hp = 100;
            e.damage = 0;

            recordList.add(record);
        }

        for (int i = 2; i < 6; i++) {
            Record record = new Record();
            record.time = i;
            record.changes = new ArrayList<>();

            Entity e = new Entity();
            e.id = 1;
            e.hp = (100 - 10*i);
            e.damage = (10);
            e.travel = 10 * i;
            record.changes.add(e);

            recordList.add(record);
        }

        System.out.println(recordList.size());

        // Create Record Encoder
        Recorder recorder = new DefaultRecorder(new TestEncoder());
        for (int i=0; i<recordList.size(); i++) {
            Record e = recordList.get(i);
            recorder.appendRecord(e);
            ByteBuffer record = recorder.getRecord(e.time);

            // get test
            if (record != null && record.hasArray())
                System.out.println(new String(record.array()));
        }
//
//        System.out.println("===================recordFrom Test=======================");
//        ByteBuffer[] recordFrom = recorder.getRecordFrom(1, 4);
//        for (ByteBuffer b: recordFrom) {
//            if (b != null && b.hasArray()) {
//                String s = new String(b.array());
//                System.out.println(s);
//            }
//        }
//
//        System.out.println("===================recordFrom Test=======================");
//        recordFrom = recorder.getRecordFrom(0, 0);
//        for (ByteBuffer b: recordFrom) {
//            if (b != null && b.hasArray()) {
//                String s = new String(b.array());
//                System.out.println(s);
//            }
//        }
    }


    class TestEncoder implements Encoder {

        ObjectMapper mapper;

        public TestEncoder() {
            mapper = new ObjectMapper();
        }

        @Override
        public ByteBuffer encodeRecord(Record record) throws EncodeException {

            ByteBuffer buffer = null;
            try {
                byte[] b = mapper.writeValueAsBytes(record);
                buffer = ByteBuffer.wrap(b);
                System.out.println(String.format("Encoded: %s", new String(b)));
            } catch (JsonProcessingException e) {
                throw new EncodeException(e);
            }

            return buffer;
        }
    }



}
