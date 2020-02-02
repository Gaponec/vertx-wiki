package com.gaponec;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  private JDBCClient jdbcClient;
  private FreeMarkerTemplateEngine templateEngine;

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
    HttpServer httpServer = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.post("/delete").handler(this::pageDeletionHandler);

    templateEngine = FreeMarkerTemplateEngine.create(vertx);

    httpServer
      .requestHandler(router)
      .listen(8080, asyncResult -> {
        if (asyncResult.succeeded()) {
          LOGGER.info("Http server running on port 8080");
          promise.complete();
        } else {
          LOGGER.error("Could not start http server", asyncResult.cause());
          promise.fail(asyncResult.cause());
        }
      });

    return promise.future();
  }

  private void pageDeletionHandler(RoutingContext routingContext) {
    jdbcClient.getConnection(connectionAsyncResult -> {
      if (connectionAsyncResult.succeeded()) {
        SQLConnection connection = connectionAsyncResult.result();
        connection.query(SQL_ALL_PAGES, result -> {
          connection.close();

          if (result.succeeded()) {
            List<String> pages = result.result()
              .getResults()
              .stream()
              .map(json -> json.getString(0))
              .sorted()
              .collect(Collectors.toList());

            routingContext.put("title", "Wiki home");
            routingContext.put("pages", pages);
            templateEngine.render(routingContext.data(), "templates/index.ftl", asyncResult -> {
              if (asyncResult.succeeded()) {
                routingContext.response().putHeader("Context-Type", "text/html");
                routingContext.response().end(asyncResult.result());
              } else {
                routingContext.fail(asyncResult.cause());
              }
            });
          } else {
            routingContext.fail(result.cause());
          }

        });
      } else {
        routingContext.fail(connectionAsyncResult.cause());
      }
    });
  }

  private void pageCreateHandler(RoutingContext routingContext) {

  }

  private void pageUpdateHandler(RoutingContext routingContext) {

  }

  private void pageRenderingHandler(RoutingContext routingContext) {

  }

  private void indexHandler(RoutingContext routingContext) {

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
