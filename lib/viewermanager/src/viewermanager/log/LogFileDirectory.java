package viewermanager.log;


import org.apache.log4j.Logger;
import viewermanager.ViewerManagerKeys;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class LogFileDirectory {

    /**
     * list of path
     * */
    private Set<String> dirs;

    public LogFileDirectory() {
        dirs = new HashSet<>();
    }

    /**
     * Add Path to list
     * */
    public void addPath(String pathStr) {
        if (pathStr != null) {
            File path = new File(pathStr);

            Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER).info(path.getAbsolutePath());
            // Check is the directory
            if (path.isDirectory())
            {
                // append to list
                dirs.add(path.getAbsolutePath());
                Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER)
                        .info(String.format("/log/path/add: path added -> '%s'", path.getAbsolutePath()));
            }
            else {
                Logger.getLogger(ViewerManagerKeys.DEFAULT_LOGGER)
                        .info(String.format("/log/path/add: cannot add path '%s'", path.getAbsolutePath()));
            }
        }
    }

    /**
     * get path list
     * */
    public String[] getPathList() {
        return dirs.toArray(new String[0]);
    }

    /**
     * remove path list
     * */
    public String removePath(String path) {
        if (dirs.contains(path) && dirs.remove(path)) {
            return path;
        }
        return null;
    }

    /**
     * get file lists on Paths
     * */
    public String[] getFilePaths() {
        List<String> files = new ArrayList<>();

        /// Match is has extension (.ext)
        final Pattern regExt = Pattern.compile(".+\\.(.{3,4})$", Pattern.CASE_INSENSITIVE);

        /// Split and get extension (.ext -> ext)
        final Pattern regExtSp = Pattern.compile("\\.");

        // for each dirs
        for (String directory : dirs) {
            // Create Log File List
            String[] list = (new File(directory)).list((dir, name) -> {
                if (!regExt.matcher(name).matches()) {
                    return false;
                }

                // get only Extension
                String[] split = regExtSp.split(name);
                if (split != null) {
                    return split[split.length-1].equals("log")
                            || split[split.length-1].equals("vlog");
                }
                return false;
            });
            // match like "abc.123" or "abc.1234", has dots and 3 chars at end of line.

            // find File and create List
            if (list != null) {
                String base = directory + File.separator; // /path/to/base/
                for (String s : list) {
                    files.add(base + s); // add '/path/to/base/path' to the list
                }
            }
        }

        // result as array
        return files.toArray(new String[0]);
    }

}
