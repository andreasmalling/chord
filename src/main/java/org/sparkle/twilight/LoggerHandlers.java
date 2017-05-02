package org.sparkle.twilight;

import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * Created by Root on 2/5/2017.
 */
public class LoggerHandlers {

    private static SQLiteUtil sq = new SQLiteUtil("logs");
    private static FileHandler fh = new LoggerUtil("logs").getFh();

    public static void addHandlers(Logger logger) {
        //Add handlers to your liking here
        logger.addHandler(sq.SQLiteHandler);
        //logger.addHandler(fh);
    }
}
