package com.gaponec;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  private static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";
  private static final Integer DEFAULT_HTTP_SERVER_PORT = 8080;

  private String wikiDbQueue = "wikidb.queue";

  private FreeMarkerTemplateEngine templateEngine;

  @Override
  public void start(Promise<Void> promise) {

    wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");

    HttpServer httpServer = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.post("/delete").handler(this::pageDeletionHandler);

    templateEngine = FreeMarkerTemplateEngine.create(vertx);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, DEFAULT_HTTP_SERVER_PORT);
    httpServer
      .requestHandler(router)
      .listen(portNumber, asyncResult -> {
        if (asyncResult.succeeded()) {
          LOGGER.info("Http server running on port" + portNumber);
          promise.complete();
        } else {
          LOGGER.error("Could not start http server", asyncResult.cause());
          promise.fail(asyncResult.cause());
        }
      });
  }

  private void pageDeletionHandler(RoutingContext routingContext) {

  }

  private void pageCreateHandler(RoutingContext routingContext) {

  }

  private void pageUpdateHandler(RoutingContext routingContext) {

  }

  private void pageRenderingHandler(RoutingContext routingContext) {

  }

  private void indexHandler(RoutingContext routingContext) {
    DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("action", "all-pages");

    vertx.eventBus().request(wikiDbQueue, new JsonObject(), deliveryOptions, reply -> {
      if (reply.succeeded()) {
        JsonObject body = (JsonObject) reply.result().body();
        routingContext.put("title", "wiki home");
        routingContext.put("pages", body.getJsonArray("pages").getList());
        templateEngine.render(routingContext.data(), "templates/index.ftl", asyncResultBuffer -> {
          if (asyncResultBuffer.succeeded()) {
            routingContext.response().putHeader("Content-Type", "text/html");
            routingContext.response().end(asyncResultBuffer.result());
          } else {
            routingContext.fail(asyncResultBuffer.cause());
          }
        });
      } else {
        routingContext.fail(reply.cause());
      }
    });
  }
}
