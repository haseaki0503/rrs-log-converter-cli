package viewermanager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import server.HttpRest;
import viewermanager.entity.Record;
import viewermanager.entity.provider.RRSLogReader;
import viewermanager.log.ViewerLogFile;
import viewermanager.log.ViewerLogFileRecord;
import viewermanager.manager.DefaultManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public class Main {

    DefaultManager manager;
    HttpRest http;

    Logger logger;

    public Main()
    {
        // Get Logger
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (logger == null) {
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }

        // Create Manager and Server Instance
        manager = new DefaultManager();
        http = new HttpRest(manager);
    }

    public void run() throws Exception
    {
        // Start Manager on new Thread
        logger.info("Start Manager");
        Thread thread = new Thread(() -> manager.run());
        thread.start();
        logger.info("Manager waked");

        http.start();
        logger.info("Server Started");
        thread.join(); // wait for manager finishes
        http.shutdown();
        manager.shutdown();
    }

    /**
     * Command Line Parser - Arguments Class
     * */
    public static class Parser {
        @Option(name="-c", aliases = {"--config"}, metaVar = "config", usage="Config File Path")
        public String config = null; // config file name

        @Option(name="-p", aliases = {"--port"}, metaVar = "port", usage="Port Number")
        public int port = 8080; // http wait port

        @Option(name="-d", usage="Create Default Config, with `config` or working dir")
        public boolean defaultConfig = false; // is the default config generation

        @Option(name="-l", aliases = {"--log"}, metaVar = "InputFileName", usage="Convert logfile to .vlog")
        public String logFile = null;

        @Option(name="-o", aliases = {"--output"}, metaVar = "OutputFileName", usage="Output File Name for logFile Converting")
        public String outputFilename = null;
    };

    private static void initConfig(Parser item) {
        // About Config
        viewermanager.misc.Config config;
        if (item.defaultConfig) {
            // Write out config
            config = viewermanager.misc.Config.createDefaultConfig();
            config.store(item.config);
            System.exit(0);
        }
        else if (item.config != null) {
            // Load Config
            config = new viewermanager.misc.Config();
            if (!config.load(item.config)) {
                System.err.println("Config Load Error");
                System.exit(1);
            }
        }
        else {
            config = viewermanager.misc.Config.createDefaultConfig();
        }

        // Config Set
        for (String key : config.keySet()) {
            Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER).debug("Config: " + key + " : " + config.get(key));
            System.setProperty(key, config.get(key));
        }

        Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER ,ViewerManagerKeys.DEFAULT_LOGGER))
                .setLevel(Level.toLevel(System.getProperty(ViewerManagerKeys.LOG_LEVEL, ViewerManagerKeys.DEFAULT_LOG_LEVEL)));
    }

    public static void main(String[] args) throws Exception
    {
        // Initialize System Properties
        Parser item = new Parser();
        CmdLineParser parser = new CmdLineParser(item);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
            parser.printUsage(System.out);
            System.exit(1);
        }
        initConfig(item);

        // Is Log convert mode
        if (Objects.nonNull(item.logFile) && !item.logFile.isEmpty()) {
            Logger log = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
            log.info(":::::::::::Log Convert Mode:::::::::::");
            log.info(String.format("Convert \"%s\" %s", item.logFile, (item.outputFilename != null) ? "to ViewerLog \"" + item.outputFilename + "\"" : ""));

            // Check is the LogFile
            RRSLogReader reader = new RRSLogReader();
            try {
                reader.open(item.logFile);
                log.info("Log File Opened");
            } catch (Exception ex) {
                log.error(String.format("Input File %s is not vaild log file!", item.logFile), ex);
                return;
            }

            ViewerLogFileRecord logRecord = new ViewerLogFileRecord(reader);
            while (reader.isIncomingRecordAvailable()) {
                Record record = reader.getIncomingRecord();
                logRecord.put(record);
            }
            log.info("File Writing...");
            ViewerLogFile.logWrite(logRecord, item.outputFilename);
            log.info("Done!");
            reader.shutdown();

            return;
        }

        // Start Serving
        try {
            (new Main()).run();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
