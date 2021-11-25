package viewermanager.encoder;


import viewermanager.entity.Record;

import java.nio.ByteBuffer;

/**
 * Encode <code>Record</code> to <code>ByteBuffer</code> (Byte Data).
 * <code>Record</code>を<code>ByteBuffer</code>(Byteデータ)に変換する.
 *  */
public interface Encoder {

    /**
     * Encode <code>Record</code> to <code>ByteBuffer</code> (Byte Data).
     * <code>Record</code>を<code>ByteBuffer</code>(Byteデータ)に変換する.
     *
     * @param record <code>Record</code> to encode.
     * @return <code>ByteBuffer</code> encoded data when encode succeeded, or null when failed.
     *  */
    ByteBuffer encodeRecord(Record record) throws EncodeException;
}
