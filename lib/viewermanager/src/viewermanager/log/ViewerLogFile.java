package viewermanager.log;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import viewermanager.ViewerManagerKeys;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * ViewerManager's Log File RW Helper Function
 * ViewerManagerのログファイルのヘルパ関数
 * */
public class ViewerLogFile {

    public static boolean logWrite(ViewerLogFileRecord record, String filename)
    {
        if(record == null) {
            return false;
        }

        String path = filename;
        if (filename == null)
        {
            // Create Path
            Calendar calendar = Calendar.getInstance();
            String date = String.format("%04d%02d%02d_%02d%02d%02d_%04d", calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND),
                    calendar.get(Calendar.MILLISECOND));

            String pathDir = System.getProperty(ViewerManagerKeys.VIEWER_LOG_DIR, ViewerManagerKeys.VIEWER_LOG_DEFAULT_DIR);
            String mapName = record.mapName;
            {
                File dir = new File(pathDir);
                if (!dir.exists()) {
                    boolean mkdir = dir.mkdir();
                    if (!mkdir) {
                        Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER).warn("ViewerLogFile - Cannot create LogDir");
                        return false;
                    }
                } else if (!dir.isDirectory()) {
                    Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER).warn("ViewerLogFile - LogDir is not directory");
                    return false;
                }

                File mapFile = new File(record.mapName);
                if (mapFile.getName() != null) {
                    mapName = mapFile.getName();
                }
            }

            path = pathDir
                    + File.separator
                    + mapName + "_" + date
                    + ".vlog";
        }

        // Write Out
        File file = new File(path);
        ObjectMapper packer = new ObjectMapper(new MessagePackFactory());
        try {
            packer.writeValue(file, record);
        } catch (IOException e) {
            Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER).warn("cannot write out logs", e);
        }

        return true;
    }

    /**
     * To Write to File
     * ファイルへの書き出しのための関数
     *
     * Output File is contained on <code>ViewerManagerKeys.VIEWER_LOG_DIR</code>,
     *  with Filename (mapName)_(year)(month)(day)_(hour)(minute)(second)_(millisec).vlog
     *  (mapName) is the last directory name of Config's mapDir, (e.g. "/maps/paris" -> "paris")
     *
     * @param record Records to Write out. 書き出すレコード
     * */
    public static boolean logWrite(ViewerLogFileRecord record)
    {
        return ViewerLogFile.logWrite(record, null);
    }


    /**
     * To Read from File
     * ファイルからの読み出しのための関数
     *
     * @param fileName fileName with path;
     * @return <code>ViewerLogFileRecord</code> contains data read by the <code>fileName</code>.
     * */
    public static ViewerLogFileRecord logRead(String fileName) throws LogFileException {
        File file = new File(fileName);
        if(!file.exists() && !file.isFile()) {
            throw new LogFileException(LogFileException.FILE_NOT_FOUND, "File not found or Not File: " + fileName);
        }

        ViewerLogFileRecord record = null;

        ObjectMapper unpacker = new ObjectMapper(new MessagePackFactory());
        try {
            record = unpacker.readValue(file, ViewerLogFileRecord.class);
        } catch (JsonParseException | JsonMappingException e) {
            Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER).warn("/log/viewer/reader: cannot unpack file : ", e);
            throw new LogFileException(LogFileException.FAIL_READ_LOG, e.getMessage(), e);
        } catch (IOException e) {
            Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER).warn("/log/viewer/reader: cannot read file : ", e);
            throw new LogFileException(LogFileException.FAIL_READ_LOG, e.getMessage(), e);
        }

        return record;
    }
}
