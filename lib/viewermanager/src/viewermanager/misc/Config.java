package viewermanager.misc;

import org.apache.log4j.Logger;
import viewermanager.ViewerManagerKeys;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Config : read Config File to System.Property, or Restore to File it;
 * Config : ファイルからSystem.Propertyに読み込み/書き込みを行うHelper
 *
 * Format - 形式
 * > key: value
 * > key : value
 * > key: |value With Spaces|
 * + Note: Spaces around the key/value are Trimmed on reading.
 * +  But Spaces inner value string are not trimmed on reading.
 * + 注意: 読み込み時にkey/valueの周りのスペースは除去されますが，
 * +  値の文字列内のスペースは除去されません．
 * > # Comment
 * > Key: value #Comment
 * + Note: Strings after '#' are ignored as a comment
 * + 注意: '#'以降はコメントとして無視されます．
 *
 * Example:
 * # Miyamoto Key Set
 * name: Miyamoto
 * age: 20
 * */
public class Config {
    private static final String[] header =
            {
                      "#############################################"
                    , "# Viewer Manger Config"
                    , "#############################################"
                    , ""
            };

    private Map<String, String> pairs;

    Logger logger;

    /**
     * Constructor
     * */
    public Config() {
        logger = Logger.getLogger(System.getProperty(ViewerManagerKeys.LOGGER, ViewerManagerKeys.DEFAULT_LOGGER));
        if (Objects.isNull(logger)){
            logger = Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER);
        }

        pairs = new HashMap<>();
    }

    /**
     * Get keySet of Property Read
     * 読み込んだPropertyのキー名一覧を返す
     * */
    public Set<String> keySet() {
        return pairs.keySet();
    }

     /**
     * Get values of Property Read
     * 読み込んだPropertyの値一覧を返す
     * */
    public Collection<String> values() {
        return pairs.values();
    }

    /**
     * Get value by key from Property Read
     * 読み込んだPropertyの中から，キー名に一致するものを返す
     * */
    public String get(String key) {
        return pairs.get(key);
    }

    /**
     * parse from string, and update config dictionary
     * 文字列から取り込んで，Configの更新をおこなう
     * */
    public /* private */ void parseLine(String line) {
        final Pattern lineReg = Pattern.compile("^(.+):(.+)$", Pattern.MULTILINE);
        final Pattern strReg = Pattern.compile("^[\"'](.*)[\"']]");

        // Remove Comments
        if (line.matches("^(.*)(\\t )*#(.*)$")) {
            line = line.replaceAll("(#+)(.*)$", "");
        }

        // Match line
        Matcher m = lineReg.matcher(line);

        while (m.find()) {
            assert m.groupCount() == 2;

            String key = m.group(1).trim();
            String value = m.group(2).trim();
            pairs.put(key, value);
        }
    }

    /**
     * load config from file
     * 指定されたファイルからConfigを読み出す
     * */
    public boolean load(String path) {
        // Path is null
        if (path == null) {
            return false;
        }

        // get Path
        Path p = Paths.get(path);
        if (!Files.exists(p) || !Files.isReadable(p)) {
            // File cannot read
            return false;
        }

        try {
            // Read All Lines
            List<String> lines = Files.readAllLines(p, Charset.defaultCharset());
            // Load all lines
            lines.forEach(this::parseLine);
        } catch (IOException e) {
            logger.error("Cannot load Config", e);
            return false;
        }

        return true;
    }

    /**
     * Write out the config to the file
     * 指定されたフィアルにConfigを書き出す．
     * */
    public boolean store(String path) {
        // Path is null
        if (path == null) {
            path = ".";
        }

        // get Path
        Path p = Paths.get(path);
        if (Files.isDirectory(p)) {
            // Path is Directory : add file name
            p = Paths.get(path, "manager.cfg");
        }
        if (!Files.isWritable(p.getParent())) {
            // Directory cannot writable
            return false;
        }

        List<String> lines = new ArrayList<>();
        for (String key : pairs.keySet()) {
            lines.add(key + ": " + pairs.get(key));
        }

        Collections.sort(lines);
        try {
            Files.write(p, Arrays.asList(header), Charset.defaultCharset());
            Files.write(p, lines, Charset.defaultCharset(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("Cannot load Config", e);
            return false;
        }

        return true;
    }

    /**
     * convert Config to <code>java.lang.Properties</code>
     * Configを<code>java.lang.Properties</code>に変換する
     * */
    public Properties toProperties() {
        Properties p = new Properties();
        for (String key : pairs.keySet()) {
            p.setProperty(key, pairs.get(key));
        }
        return p;
    }

    /**
     * create Config from <code>viewermangaer.ViewerManagerKeys</code>
     * <code>viewermangaer.ViewerManagerKeys</code>からConfigを作成する.
     * */
    public static Config createDefaultConfig() {
        Config config = new Config();
        config.pairs = ViewerManagerKeys.getDefaultPairs();
        return config;
    }
}
