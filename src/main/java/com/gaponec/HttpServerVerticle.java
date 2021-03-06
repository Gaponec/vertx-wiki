package com.gaponec;

import static io.netty.util.internal.StringUtil.isNullOrEmpty;

import com.github.rjeschke.txtmark.Processor;
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

import java.time.LocalDateTime;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  private static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";
  private static final Integer DEFAULT_HTTP_SERVER_PORT = 8080;
  private static final String EMPTY_PAGE_MARKDOWN = "Page is empty";

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
    String id = routingContext.request().getParam("id");
    JsonObject request = new JsonObject()
      .put("id", id);
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "delete-page");
    vertx.eventBus().request(wikiDbQueue, request, options, reply -> {
      if (reply.succeeded()) {
        routingContext.response().setStatusCode(303);
        routingContext.response().putHeader("Location", "/");
        routingContext.response().end();
      } else {
        routingContext.fail(reply.cause());
      }
    });
  }

  private void pageCreateHandler(RoutingContext routingContext) {
    String pageName = routingContext.request().getParam("name");
    String location = "/wiki/" + pageName;
    if (isNullOrEmpty(pageName)) {
      location = "/";
    }
    routingContext.response().setStatusCode(303);
    routingContext.response().putHeader("Location", location);
    routingContext.response().end();
  }

  private void pageUpdateHandler(RoutingContext routingContext) {
    String title = routingContext.request().getParam("title");
    JsonObject request = new JsonObject()
      .put("id", routingContext.request().getParam("id"))
      .put("title", title)
      .put("markdown", routingContext.request().getParam("markdown"));

    DeliveryOptions options = new DeliveryOptions();
    if ("yes".equals(routingContext.request().getParam("newPage"))) {
      options.addHeader("action", "create-page");
    } else {
      options.addHeader("action", "save-page");
    }

    vertx.eventBus().request(wikiDbQueue, request, options, reply -> {
      if (reply.succeeded()) {
        routingContext.response().setStatusCode(303);
        routingContext.response().putHeader("Location", "/wiki/" + title);
        routingContext.response().end();
      } else {
        routingContext.fail(reply.cause());
      }
    });
  }

  private void pageRenderingHandler(RoutingContext routingContext) {
    String requestPage = routingContext.request().getParam("page");
    JsonObject request = new JsonObject().put("page", requestPage);

    DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-page");
    vertx.eventBus().request(wikiDbQueue, request, options, reply -> {
      if (reply.succeeded()) {
        JsonObject body = (JsonObject) reply.result().body();

        boolean found = body.getBoolean("found");

        String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);
        routingContext.put("title", requestPage);
        routingContext.put("id", body.getInteger("id", -1));
        routingContext.put("newPage", found ? "no" : "yes");
        routingContext.put("rawContent", rawContent);
        routingContext.put("content", Processor.process(rawContent));
        routingContext.put("timestamp", LocalDateTime.now().toString());
        templateEngine.render(routingContext.data(), "templates/page.ftl", bufferAsyncResult -> {
          if (bufferAsyncResult.succeeded()) {
            routingContext.response().putHeader("Content-Type", "text/html");
            routingContext.response().end(bufferAsyncResult.result());
          } else {
            routingContext.fail(bufferAsyncResult.cause());
          }
        });
      } else {
        routingContext.fail(reply.cause());
      }
    });
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
