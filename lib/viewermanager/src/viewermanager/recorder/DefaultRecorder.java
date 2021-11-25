package viewermanager.recorder;

import org.apache.log4j.Logger;
import viewermanager.ViewerManagerKeys;
import viewermanager.encoder.EncodeException;
import viewermanager.encoder.Encoder;
import viewermanager.entity.Record;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default Recorder - Reference Implementation of <code>Recorder</code>.
 * Give <code>Encoder</code> on the Constructor.
 *
 * <code>Recorder</code>のリファレンス実装，
 * <code>Encoder</code>をコンストラクタで指定して利用する．
 * */
public class DefaultRecorder implements Recorder {

    /**
     * encoded Record's List
     *
     * 変換された<code>Record</code>を保持するリスト．
     * */
    List<ByteBuffer> recordList;

    /**
     * current TimeStep taken from appended Record
     *
     * 最後に与えられた<code>Record</code>から得たTimeStep
     * */
    int currentTimeStep;

    /**
     * Encoder
     * */
    Encoder encoder;

    Logger logger;

    /**
     * Constructor of <code>DefaultRecorder</code>.
     * */
    public DefaultRecorder(Encoder encoder) {
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (Objects.isNull(logger)){
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }

        recordList = new ArrayList<>();
        currentTimeStep = 0;
        this.encoder = encoder;
    }


    /**
     * Append Record to List.
     * Appended records will be serialized to byte array.
     *
     * Listに<code>Record</code>を追加する．
     * 追加された<code>Record</code>はバイト配列に直列化されます．
     *
     * @param record record appended.
     * */
    @Override
    public void appendRecord(Record record) throws EncodeException {
        if (currentTimeStep >= record.time) {
            throw new IllegalArgumentException("Record time Reverted: " + String.format("{'currentTimeStep': %d, 'record.time': %d}", currentTimeStep, record.time));
        }

        // Encode Record
        ByteBuffer buffer = null;
        try {
            buffer = encoder.encodeRecord(record);
        } catch (EncodeException e) {
            logger.error("DefaultRecorder - Cannot encode Record", e);
            throw e;
        }

        // Save time and append to List
        if (buffer != null) {
            recordList.add(buffer);
            currentTimeStep = record.time;
            logger.debug(String.format("/recorder/default: record appended: {time: %d, length: %d}"
                    , record.time, buffer.array().length));
        }
    }

    /**
     * get Serialized Record by time.
     * 指定した<code>timeStep</code>の直列化済みレコードを取得する
     *
     * @param timeStep timeStep of record required.
     * @return record encoded if timeStep already encoded. Else null.
     * */
    @Override
    public ByteBuffer getRecord(int timeStep) {
        // Get one Record
        //timeStep -= 1;
        logger.debug(String.format("/recorder/default: record requested on time %d", timeStep));

        // Check is the valid Record Listed
        if (timeStep < recordList.size()) {
            // get record and copy it
            ByteBuffer buf = recordList.get(timeStep).duplicate();
            // go back pointer 0
            buf.clear();
            return buf;
        }

        // No Record Available
        return null;
    }

    /**
     * get Listed Record count.
     * リストに追加された<code>Record</code>の数を返す．
     *
     * @return list size of records
     * */
    @Override
    public int size() {
        return recordList.size();
    }

    /**
     * get all record listed.
     * リストに追加されたすべてのレコードの配列を返す．
     *
     * @return array of all records encoded, not null.
     * */
    @Override
    public ByteBuffer[] getRecords() {
        return recordList.toArray(new ByteBuffer[0]);
    }

    /**
     * get <code>Encoder</code> used.
     * 利用する<code>Encoder</code>を取得する．
     *
     * @return registered Encoder
     * */
    @Override
    public Encoder getEncoder() {
        return encoder;
    }

}
