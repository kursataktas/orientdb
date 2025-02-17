package com.orientechnologies.orient.test.database.security;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.log.OLogger;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import java.io.File;
import org.junit.Test;

public class SystemUsersTest {
  private static final OLogger logger = OLogManager.instance().logger(SystemUsersTest.class);

  @Test
  public void test() {
    final String buildDirectory = System.getProperty("buildDirectory", ".");
    System.setProperty(
        "ORIENTDB_HOME", buildDirectory + File.separator + SystemUsersTest.class.getSimpleName());

    logger.info("ORIENTDB_HOME: %s", System.getProperty("ORIENTDB_HOME"));

    OrientDB orient =
        new OrientDB(
            "plocal:target/" + SystemUsersTest.class.getSimpleName(),
            OrientDBConfig.defaultConfig());

    try {
      orient.execute(
          "create database " + "test" + " memory users ( admin identified by 'admin' role admin)");

      orient.execute("create system user systemxx identified by systemxx role admin").close();
      ODatabaseSession db = orient.open("test", "systemxx", "systemxx");

      db.close();
    } finally {
      orient.close();
    }
  }
}
