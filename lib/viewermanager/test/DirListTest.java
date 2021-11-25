import org.junit.Test;
import viewermanager.log.LogFileDirectory;

public class DirListTest {


    @Test
    public void DirListTest() {
        LogFileDirectory dirs = new LogFileDirectory();
        dirs.addPath("/home/yuma/dev/robocup/ViewerManager/logs");
        dirs.addPath("/home/yuma/Downloads");
        for (String s : dirs.getFilePaths()) {
            System.out.println(s);
        }

        System.out.println();
        System.out.println(">>> 改行！ <<<");
        if(dirs.removePath("/home/yuma/dev/robocup/ViewerManager/logs") == null) {
            System.out.println("Delete Failed");
        }
        for (String s : dirs.getPathList()) {
            System.out.println(s);
        }

        System.out.println();
        System.out.println(">>> 改行！ <<<");
        for (String s : dirs.getFilePaths()) {
            System.out.println(s);
        }
    }
}
