package org.lorislab.lorisgate.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "lorislab.lorisgate")
public interface LorisgateServerConfig {

    String OIDC_AUTH_URL = "lorislab.lorisgate.oidc.auth-server-url";

    String OIDC_CLIENT_ID = "lorislab.lorisgate.oidc.client-id";

    String OIDC_CLIENT_SECRET = "lorislab.lorisgate.oidc.client-secret";

    String ENDPOINT = "lorislab.lorisgate.endpoint";

    String HOST = "lorislab.lorisgate.host";

    String PORT = "lorislab.lorisgate.port";

    String CLIENT_HOST = "lorislab.lorisgate.client.host";

    String CLIENT_PORT = "lorislab.lorisgate.client.port";

    /**
     * Host of the server
     */
    @WithName("host")
    @WithDefault("localhost")
    String host();

    /**
     * Port of the server
     */
    @WithName("port")
    @WithDefault("8080")
    String port();

    /**
     * Endpoint of the server
     */
    @WithName("endpoint")
    @WithDefault("http://localhost:8080")
    String endpoint();

    /**
     * Host of the server for the lorisgate client
     */
    @WithName("client.host")
    @WithDefault("localhost")
    String clientHost();

    /**
     * Port of the server for the lorisgate client
     */
    @WithName("client.port")
    @WithDefault("8080")
    String clientPort();

    /**
     * OIDC configuration.
     */
    @WithName("oidc")
    OidConfig oidc();

    /**
     * OIDC configuration.
     */
    interface OidConfig {

        /**
         * Url of the auth server.
         */
        @WithName("auth-server-url")
        String authServerUrl();

        /**
         * Url of the token server.
         */
        @WithName("client-id")
        @WithDefault("quarkus-app")
        String clientId();

        /**
         * Url of the token server.
         */
        @WithName("client-secret")
        @WithDefault("secret")
        String clientSecret();
    }
}
