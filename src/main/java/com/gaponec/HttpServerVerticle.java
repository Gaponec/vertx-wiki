package com.gaponec;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  private static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  private static final Integer DEFAULT_HTTP_SERVER_PORT = 8080;

  private FreeMarkerTemplateEngine templateEngine;

  @Override
  public void start(Promise<Void> promise) {
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
  }
}
