import org.apache.log4j.Logger;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineParser;
import viewermanager.*;
import viewermanager.misc.Config;

import java.nio.file.*;
import java.io.*;
import java.util.Objects;
import java.util.function.Consumer;


public class ArgsTest {

    @Test
    public void ArgsTest() throws Exception {
        viewermanager.Main.Parser item = new viewermanager.Main.Parser();
        CmdLineParser parser = new CmdLineParser(item);

        if (item.config != null) {
            Path path = Paths.get(item.config);
            if (Files.exists(Paths.get(item.config))) {
                BufferedReader reader = Files.newBufferedReader(path);
                System.getProperties().load(reader);
            }
        }
    }

    @Test
    public void ConfigTest() throws Exception {
        Config config = new Config();
        config.parseLine("TestCase1: 123 4");
        config.parseLine("TestCase2 : \"Hello World\"");

        config.parseLine("");
        config.parseLine("TestCase3: test\nTestCase4: Debug");
        config.parseLine("TestCase5: test #TestCase6: Debug");
        config.parseLine("# TestCase7: Test");

        assert Objects.equals(config.get("TestCase1"), "123 4");
        assert Objects.equals(config.get("TestCase2"), "\"Hello World\"");
        assert Objects.equals(config.get("TestCase3"), "test");
        assert Objects.equals(config.get("TestCase4"), "Debug");
        assert Objects.equals(config.get("TestCase5"), "test");
        assert Objects.isNull(config.get("testCase6"));
        assert Objects.isNull(config.get("testCase7"));
    }
}
