package viewermanager.recorder;


import viewermanager.encoder.EncodeException;
import viewermanager.encoder.Encoder;
import viewermanager.entity.Record;

import java.nio.ByteBuffer;

/**
 * Recorder - Serialize <code>Record</code> and List up these by TimeStep
 * Use <code>Encoder</code> to serialize record.
 *
 * <code>Record</code>を直列化し，TimeStepによって保持する(Map)
 * <code>Encoder</code>をレコードの直列化のために利用する．
 * */
public interface Recorder {

    /**
     * Append Record to List.
     * Appended records will be serialized to byte array.
     *
     * Listに<code>Record</code>を追加する．
     * 追加された<code>Record</code>はバイト配列に直列化されます．
     *
     * @param record record appended.
     * */
    void appendRecord(Record record) throws EncodeException;

    /**
     * get Serialized Record by time.
     * 指定した<code>timeStep</code>の直列化済みレコードを取得する
     *
     * @param timeStep timeStep of record required.
     * @return record encoded if timeStep already encoded. Else null.
     * */
    ByteBuffer getRecord(int timeStep);

    /**
     * get Listed Record count.
     * リストに追加された<code>Record</code>の数を返す．
     *
     * @return list size of records
     * */
    int size();

    /**
     * get all record listed.
     * リストに追加されたすべてのレコードの配列を返す．
     *
     * @return array of all records encoded, not null.
     * */
    ByteBuffer[] getRecords();

    /**
     * get <code>Encoder</code> used.
     * 利用する<code>Encoder</code>を取得する．
     *
     * @return registered Encoder
     * */
    Encoder getEncoder();
}


