package com.gaponec;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  private JDBCClient jdbcClient;

  private static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages " +
    "(Id integer identity primary key, Name varchar(255) unique, Content clob)";
  private static final String SQL_GET_PAGE = "select Id, Content from Pages where Name = ?";
  private static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
  private static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
  private static final String SQL_ALL_PAGES = "select Name from Pages";
  private static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";

  @Override
  public void start(Promise<Void> promise) {
    Future<Void> steps = prepareDatabase().compose(v -> createHttpServer());
    steps.setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        promise.complete();
      } else {
        promise.fail(asyncResult.cause());
      }
    });
  }

  private Future<Void> createHttpServer() {
    Promise<Void> promise = Promise.promise();
    return promise.future();
  }

  private Future<Void> prepareDatabase() {
    Promise<Void> promise = Promise.promise();

    jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:file:db/wiki")
      .put("driver_class", "org.hsqldb.jdbcDriver")
      .put("max_pool_size", 30));

    jdbcClient.getConnection(asyncResult -> {
      if (asyncResult.failed()) {
        LOGGER.error("Could not open database connection", asyncResult.cause());
        promise.fail(asyncResult.cause());
      } else {
        SQLConnection connection = asyncResult.result();
        connection.execute(SQL_CREATE_PAGES_TABLE, result -> {
          connection.close();
          if (result.failed()) {
            LOGGER.error("Database preparation error", result.cause());
          } else {
            promise.complete();
          }
        });
      }
    });

    return promise.future();
  }
}
