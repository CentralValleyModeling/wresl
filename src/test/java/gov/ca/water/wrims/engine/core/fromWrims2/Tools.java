package gov.ca.water.wrims.engine.core.fromWrims2;

import java.io.*;

public class Tools {

    public static PrintWriter openFile(String dirPath, String fileName) throws IOException {

        File f = new File(dirPath, fileName);
        File dir = new File(f.getParent());
        dir.mkdirs();
        f.createNewFile();

        return new PrintWriter(new BufferedWriter(new FileWriter(f)));
    }

    public static PrintWriter openFile(String dirPath, String fileName, boolean isAppend) throws IOException {

        File f = new File(dirPath, fileName);
        File dir = new File(f.getParent());
        dir.mkdirs();
        f.createNewFile();

        return new PrintWriter(new BufferedWriter(new FileWriter(f,isAppend)));
    }
}
