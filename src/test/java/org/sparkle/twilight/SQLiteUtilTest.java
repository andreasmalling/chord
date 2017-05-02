package org.sparkle.twilight;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.validation.constraints.AssertFalse;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Created by Root on 27/4/2017.
 */
public class SQLiteUtilTest {

    private SQLiteUtil SU;

    @Before
    public void setUp() throws Exception {
        SU = new SQLiteUtil("test");
    }

    @Test
    public void TestDBConnection() throws SQLException {
        System.out.println("DB: "+SU.getDataSource().getUrl());
        assertFalse("Connection is not closed", SU.getConnection().isClosed());
        assertFalse("Connection is not closed2", SU.getDataSource().getConnection().isClosed());
    }

    @Test
    public void TestDBInsertWithLogger(){
       Logger l = Logger.getLogger(Class.class.getName());
       l.addHandler(SU.SQLiteHandler);
       l.setLevel(Level.FINE);
       l.fine("This is a test");
    }
}