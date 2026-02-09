package org.lorislab.lorisgate.quarkus.deployment.devservices;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface LorisgateDevServicesConfig {

    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     * <p>
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * a database when running in Dev or Test mode and when Docker is running.
     */
    @WithName("enabled")
    Optional<Boolean> enabled();

    /**
     * Indicates if the P6 server managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for P6 starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-p6} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithName("shared")
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-p6} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for p6 looks for a container with the
     * {@code quarkus-dev-service-p6} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-p6} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared p6 servers.
     */
    @WithName("service-name")
    @WithDefault("p6")
    String serviceName();

    /**
     * The container image name to use, for container based DevServices providers.
     */
    @WithName("image-name")
    Optional<String> imageName();

    /**
     * Helper to define the stop strategy for containers created by DevServices.
     * In particular, we don't want to actually stop the containers when they
     * have been flagged for reuse, and when the Test-containers configuration
     * has been explicitly set to allow container reuse.
     * To enable reuse, ass {@literal testcontainers.reuse.enable=true} in your
     * {@literal .testcontainers.properties} file, to be stored in your home.
     *
     * @see <a href="https://www.testcontainers.org/features/configuration/">Testcontainers Configuration</a>.
     */
    @WithName("reuse")
    @WithDefault("false")
    boolean reuse();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @WithName("port")
    OptionalInt port();

    /**
     * Enabled or disable log of the mock-server
     */
    @WithName("log")
    @WithDefault("false")
    boolean log();

    /**
     * Environment variables that are passed to the container.
     */
    @WithName("container-env")
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> containerEnv();
}
