package com.noomtech.chatserver.tests.utilities;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class TestUtilities {


    public static void runSQLScript(Connection connection, String script) {
        Arrays.stream(script.replace("\n", "").split(";")).forEach(sql -> {
            try(var statement = connection.createStatement()) {
                statement.execute(sql);
            }
            catch(SQLException sqlE) {
                throw new RuntimeException(sqlE);
            }
        });
    }
}
