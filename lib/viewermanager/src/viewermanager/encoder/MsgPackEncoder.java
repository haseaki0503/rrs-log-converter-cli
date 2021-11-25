package viewermanager.encoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import viewermanager.ViewerManagerKeys;
import viewermanager.entity.Record;

import java.nio.ByteBuffer;

/**
 * MsgPackEncoder - <code>Encoder</code> use MsgPack to encoding.
 *  On Serialize, uses Jackson MessagePack Library.
 *
 * MsgPackを利用する<code>Encoder</code>
 * 直列化のためにJacksonのMessagePackライブラリを使用しています．
 * */
public class MsgPackEncoder implements Encoder {

    /**
     * Jackson Serializer Object
     * */
    private ObjectMapper mapper;
    Logger logger;

    public MsgPackEncoder() {
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (logger == null) {
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }
        mapper = new ObjectMapper(new MessagePackFactory());
    }

    /**
     * Encode <code>Record</code> to <code>ByteBuffer</code> (Byte Data).
     * <code>Record</code>を<code>ByteBuffer</code>(Byteデータ)に変換する.
     *
     * @param record <code>Record</code> to encode.
     * @return <code>ByteBuffer</code> encoded data when encode succeeded, or null when failed.
     *  */
    @Override
    public ByteBuffer encodeRecord(Record record) throws EncodeException {
        ByteBuffer result = null;
        byte[] data;

        // Encode Data
        if (record != null) {
            try {
                data = mapper.writeValueAsBytes(record);
                result = ByteBuffer.allocate(data.length);
                result.put(data);
                result.flip();
            } catch (JsonProcessingException e) {
                throw new EncodeException(e);
            }
        }

        return result;
    }
}
