package com.gaponec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

  @BeforeEach
  void prepare(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(MainVerticle.class.getCanonicalName(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @Disabled
  @DisplayName("Check that the server has started")
  void checkServerHasStarted(Vertx vertx, VertxTestContext testContext) {
    WebClient webClient = WebClient.create(vertx);
    webClient.get(8080, "localhost", "/")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        assertTrue(response.body().length() > 0);
        assertTrue(response.body().contains("Hello Vertx"));
        testContext.completeNow();
      })));
  }
}
