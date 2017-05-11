package org.sparkle.twilight;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;


public class SQLiteHandler extends Handler {

    private Connection connection;
    private SQLiteDataSource dataSource;

    public SQLiteHandler(SQLiteDataSource dataSource) {
        try {
            connection = dataSource.getConnection();
            this.dataSource = dataSource;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publish(LogRecord record) {
        try {
            String message = record.getMessage();
            String[] split = message.split("value: ");
            String part1 = split[0];
            String part2 = "0";
            if (split.length > 1) {
                part2 = split[1];
            }
                connection.createStatement().execute("INSERT INTO Logs (NODE ,CLASS, METHOD, TIME, MESSAGE, VALUE ,LEVEL) " +
                        "VALUES ('" + Main.BASE_PORT
                        + "','" + record.getSourceClassName()
                        + "','" + record.getSourceMethodName()
                        + "','" + new Timestamp(record.getMillis())
                        + "','" + part1
                        + "','" + part2
                        + "','" + record.getLevel().toString() + "');");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() {
        try {
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws SecurityException {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
