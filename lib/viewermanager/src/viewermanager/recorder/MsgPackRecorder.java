package viewermanager.recorder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import viewermanager.ViewerManagerKeys;
import viewermanager.encoder.MsgPackEncoder;
import viewermanager.entity.Record;

import java.nio.ByteBuffer;

/**
 * <code>DefaultRecorder</code> uses <code>MsgPackEncoder</code> as <code>Encoder</code>.
 * <code>Encoder</code>として<code>MsgPackEncoder</code>を使う<code>DefaultRecorder</code>
 * */
public class MsgPackRecorder extends DefaultRecorder {
    public MsgPackRecorder() {
        super(new MsgPackEncoder());
    }
}
