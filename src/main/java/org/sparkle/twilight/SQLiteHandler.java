package org.sparkle.twilight;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


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
            connection.createStatement().execute("INSERT INTO Logs (CLASS, METHOD, TIME, MESSAGE, LEVEL) " +
                    "VALUES ('"+ record.getSourceClassName()
                    +"','"+record.getSourceMethodName()
                    +"','"+record.getMillis()
                    +"','"+record.getMessage()
                    +"','"+record.getLevel().toString()+"');");
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
