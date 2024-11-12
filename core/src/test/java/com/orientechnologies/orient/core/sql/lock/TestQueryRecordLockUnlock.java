package com.orientechnologies.orient.core.sql.lock;

import static org.assertj.core.api.Assertions.assertThat;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestQueryRecordLockUnlock {

  @Before
  public void before() {
    OGlobalConfiguration.STORAGE_PESSIMISTIC_LOCKING.setValue(OrientDBConfig.LOCK_TYPE_READWRITE);
  }

  @Test
  public void testLockReleaseAfterIncrement() throws InterruptedException {
    OrientDB orientDB = new OrientDB("memory:", OrientDBConfig.defaultConfig());
    final ORID id;
    orientDB.execute(
        "create database "
            + TestQueryRecordLockUnlock.class.getSimpleName()
            + " memory users(admin identified by 'adminpwd' role admin)");
    try (ODatabaseDocument db =
        orientDB.open(TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
      ODocument doc = new ODocument();
      doc.field("count", 0);
      doc = db.save(doc, db.getClusterNameById(db.getDefaultClusterId()));
      id = doc.getIdentity();
      db.commit();
    }
    int thread = 10;

    ExecutorService pool = Executors.newFixedThreadPool(thread);
    for (int i = 0; i < 10; i++) {
      pool.submit(
          new Runnable() {

            @Override
            public void run() {
              try (ODatabaseDocument db =
                  orientDB.open(
                      TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
                for (int j = 0; j < 10; j++) {
                  db.getLocalCache().deleteRecord(id);
                  String asql =
                      "update " + id.toString() + " set count += 1 where count < 50 lock record";
                  db.command(asql);
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          });
    }
    pool.shutdown();
    pool.awaitTermination(1, TimeUnit.HOURS);
    try (ODatabaseDocument db =
        orientDB.open(TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
      ODocument doc = db.load(id);
      assertThat(doc.<Integer>field("count")).isEqualTo(50);
    }
    orientDB.close();
  }

  @Test
  @Ignore
  public void testLockWithSubqueryRecord() throws InterruptedException {
    final ORID id;
    OrientDB orientDB = new OrientDB("memory:", OrientDBConfig.defaultConfig());
    orientDB.execute(
        "create database "
            + TestQueryRecordLockUnlock.class.getSimpleName()
            + " memory users(admin identified by 'adminpwd' role admin)");
    try (ODatabaseDocument db =
        orientDB.open(TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
      ODocument doc = new ODocument();
      doc.field("count", 0);
      doc = db.save(doc, db.getClusterNameById(db.getDefaultClusterId()));
      id = doc.getIdentity();
      db.commit();
    }
    int thread = 10;

    ExecutorService pool = Executors.newFixedThreadPool(thread);
    for (int i = 0; i < 10; i++) {
      pool.submit(
          new Runnable() {

            @Override
            public void run() {
              try (ODatabaseDocument db =
                  orientDB.open(
                      TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
                for (int j = 0; j < 10; j++) {
                  String asql =
                      "update (select from "
                          + id.toString()
                          + ") set count += 1 where count < 50 lock record";
                  db.command(asql).close();
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          });
    }
    pool.shutdown();
    pool.awaitTermination(1, TimeUnit.HOURS);
    try (ODatabaseDocument db =
        orientDB.open(TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
      ODocument doc = db.load(id);
      //      assertEquals(50, doc.field("count"));

      assertThat(doc.<Integer>field("count")).isEqualTo(50);
    }
    orientDB.close();
  }

  @Test
  public void testLockReleaseAfterIncrementOpenClose() throws InterruptedException {
    final ORID id;
    OrientDB orientDB = new OrientDB("memory:", OrientDBConfig.defaultConfig());
    orientDB.execute(
        "create database "
            + TestQueryRecordLockUnlock.class.getSimpleName()
            + " memory users(admin identified by 'adminpwd' role admin)");
    try (ODatabaseDocument db =
        orientDB.open(TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
      ODocument doc = new ODocument();
      doc.field("count", 0);
      doc = db.save(doc, db.getClusterNameById(db.getDefaultClusterId()));
      id = doc.getIdentity();
      db.commit();
    }
    int thread = 10;

    ExecutorService pool = Executors.newFixedThreadPool(thread);
    for (int i = 0; i < 10; i++) {
      pool.submit(
          new Runnable() {

            @Override
            public void run() {
              for (int j = 0; j < 10; j++) {
                try (ODatabaseDocument db =
                    orientDB.open(
                        TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
                  String asql =
                      "update " + id.toString() + " set count += 1 where count < 50 lock record";
                  String ex =
                      db.command("explain " + asql).getExecutionPlan().get().prettyPrint(0, 0);
                  System.out.println("start : " + ex);
                  db.command(asql).close();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            }
          });
    }
    pool.shutdown();
    pool.awaitTermination(1, TimeUnit.HOURS);
    try (ODatabaseDocument db =
        orientDB.open(TestQueryRecordLockUnlock.class.getSimpleName(), "admin", "adminpwd")) {
      ODocument doc = db.load(id);

      assertThat(doc.<Integer>field("count")).isEqualTo(50);
    }
    orientDB.close();
  }

  @After
  public void after() {
    OGlobalConfiguration.STORAGE_PESSIMISTIC_LOCKING.setValue("none");
  }
}
