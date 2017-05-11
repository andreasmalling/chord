package org.sparkle.twilight;

import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteJDBCLoader;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Root on 27/4/2017.
 */
public class SQLiteUtil {

    private Connection connection = null;
    private SQLiteDataSource dataSource = null;
    public SQLiteHandler SQLiteHandler;

    public SQLiteUtil(String logname) {
        try {
            Class.forName("org.sqlite.JDBC");
            SQLiteJDBCLoader.initialize();
            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:./log/" + logname + ".db");
            Connection conn = ds.getConnection();
            Statement st = conn.createStatement();
            st.execute("CREATE TABLE IF NOT EXISTS Logs (ID INTEGER PRIMARY KEY ASC, NODE INTEGER, CLASS CHAR , METHOD CHAR, " +
                    "TIME TEXT, MESSAGE CHAR, VALUE INTEGER, LEVEL CHAR);");
            st.close();
            connection = conn;
            dataSource = ds;
            //dataSource.getParentLogger().addHandler(new ConsoleHandler());
            this.SQLiteHandler = new SQLiteHandler(dataSource);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public SQLiteDataSource getDataSource() {
        return dataSource;
    }
}
