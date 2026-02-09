package org.lorislab.lorisgate.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "lorislab.lorisgate")
public interface LorisgateServerConfig {

    String ENDPOINT = "lorislab.lorisgate.endpoint";

    String HOST = "lorislab.lorisgate.host";

    String PORT = "lorislab.lorisgate.port";

    String CLIENT_HOST = "lorislab.lorisgate.client.host";

    String CLIENT_PORT = "lorislab.lorisgate.client.port";

    /**
     * Host of the MockServer
     */
    @WithName("host")
    @WithDefault("localhost")
    String host();

    /**
     * Port of the MockServer
     */
    @WithName("port")
    @WithDefault("1080")
    String port();

    /**
     * Endpoint of the MockServer
     */
    @WithName("endpoint")
    @WithDefault("http://localhost:8080")
    String endpoint();

    /**
     * Host of the MockServer for the MockServerClient
     */
    @WithName("client.host")
    @WithDefault("localhost")
    String clientHost();

    /**
     * Port of the MockServer for the MockServerClient
     */
    @WithName("client.port")
    @WithDefault("1080")
    String clientPort();
}
