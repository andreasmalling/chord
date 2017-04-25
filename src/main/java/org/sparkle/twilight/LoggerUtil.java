package org.sparkle.twilight;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

/**
 * Created by Root on 25/4/2017.
 */
public class LoggerUtil {

    private static FileHandler fh;

    public LoggerUtil(String BASE_PORT) {
        try {
            fh = new FileHandler("./log/"+BASE_PORT+".log");
            fh.setFormatter(new SimpleFormatter());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FileHandler getFh() {
        return fh;
    }

}
