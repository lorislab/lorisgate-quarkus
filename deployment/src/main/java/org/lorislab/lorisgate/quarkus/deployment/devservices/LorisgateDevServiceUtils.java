package org.lorislab.lorisgate.quarkus.deployment.devservices;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;

public class LorisgateDevServiceUtils {

    static WebClient createWebClient(Vertx vertx) {
        WebClientOptions options = new WebClientOptions();
        options.setTrustAll(true);
        options.setVerifyHost(false);
        return WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);
    }
}
