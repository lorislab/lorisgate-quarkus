package org.lorislab.lorisgate.quarkus.deployment.devservices;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface LorisgateDevServicesConfig {

    /**
     * OIDC configuration.
     */
    @WithName("oidc")
    OidcConfig oidc();

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

    /**
     * Volume mounts.
     */
    @WithName("volume-mounts")
    Map<String, String> volumeMounts();

    /**
     * MockServer's configuration class-path binding. Useful for the test and CI builds.
     * When set to {@code true}, a test-container {@code withClasspathResourceMapping} method is used.
     */
    @WithName("config-class-path")
    @WithDefault("false")
    boolean configClassPath();

    /**
     * Realms configuration.
     */
    @WithName("realms")
    Map<String, RealmConfig> realms();

    /**
     * Default realm configuration. This configuration is used to create a default realm with default clients and users.
     */
    @WithDefault("realm")
    DefaultRealmConfig realm();

    /**
     * Default realm configuration. This configuration is used to create a default realm with default clients and users.
     */
    interface DefaultRealmConfig {

        /**
         * Default realm name.
         */
        @WithName("name")
        @WithDefault("quarkus")
        String name();

        /**
         * Enabled or disable the realm.
         */
        @WithName("enabled")
        @WithDefault("true")
        boolean enabled();

        /**
         * Create default realm.
         */
        @WithName("create")
        @WithDefault("true")
        boolean create();

        /**
         * Default realm roles.
         * name: admin / description: Admin role / enabled: true
         * name: user / description: User role / enabled: true
         */
        @WithName("create-roles")
        @WithDefault("true")
        boolean createRoles();

        /**
         * Create default clients. Default clients:
         * quarkus-app / secret / confidential: true / scopes: "openid,profile,email" / redirectUris: null
         * quarkus-app-public / secret / confidential: false / scopes: "openid,profile,email" / redirectUris: *
         */
        @WithName("create-clients")
        @WithDefault("true")
        boolean createClients();

        /**
         * Create default users. Default users:
         * name: bob / givenName: bob / familyName: bob / email: bob@localhost / password: bob / roles: admin
         * name: alice / givenName: alice / familyName: alice / email: alice@localhost / password: alice / roles: admin,user
         */
        @WithName("create-users")
        @WithDefault("true")
        boolean createUsers();

        /**
         * Realm roles.
         */
        @WithName("roles")
        Map<String, RealmRoleConfig> roles();

        /**
         * Realm clients.
         */
        @WithName("clients")
        Map<String, RealmClientConfig> clients();

        /**
         * Realm users.
         */
        @WithName("users")
        Map<String, RealmUserConfig> users();
    }

    /**
     * OIDC configuration.
     */
    interface OidcConfig {

        /**
         * Set quarkus OIDC properties.
         */
        @WithName("enable-quarkus-oidc")
        @WithDefault("true")
        boolean enableQuarkusOidc();
    }

    /**
     * Realm configuration.
     */
    interface RealmConfig {

        /**
         * Enabled or disable the realm.
         */
        @WithName("enabled")
        @WithDefault("true")
        boolean enabled();

        /**
         * Realm roles.
         */
        @WithName("roles")
        Map<String, RealmRoleConfig> roles();

        /**
         * Realm clients.
         */
        @WithName("clients")
        Map<String, RealmClientConfig> clients();

        /**
         * Realm users.
         */
        @WithName("users")
        Map<String, RealmUserConfig> users();

    }

    interface RealmRoleConfig {

        /**
         * Description of the role.
         */
        @WithName("description")
        String description();

        /**
         * Enabled or disable the role.
         */
        @WithName("enabled")
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Realm user configuration.
     */
    interface RealmUserConfig {

        /**
         * Password of the user.
         */
        @WithName("password")
        String password();

        /**
         * Enabled or disable the user.
         */
        @WithName("enabled")
        @WithDefault("true")
        boolean enabled();

        /**
         * User roles.
         */
        @WithName("roles")
        Set<String> roles();

        /**
         * Name of the user.
         */
        @WithName("name")
        Optional<String> name();

        /**
         * Given name of the user.
         */
        @WithName("given-name")
        Optional<String> givenName();

        /**
         * Family name of the user.
         */
        @WithName("family-name")
        Optional<String> familyName();

        /**
         * Email of the user.
         */
        @WithName("email")
        Optional<String> email();

        /**
         * Enabled or disable the email verification for the user.
         */
        @WithName("email-verified")
        @WithDefault("true")
        boolean emailVerified();
    }

    /**
     * Realm client configuration.
     */
    interface RealmClientConfig {

        /**
         * Client name.
         */
        @WithName("client-secret")
        @WithDefault("secret")
        String clientSecret();

        /**
         * Enabled or disable the client.
         */
        @WithName("confidential")
        @WithDefault("true")
        boolean confidential();

        /**
         * Client scopes.
         */
        @WithName("scopes")
        Set<String> scopes();

        /**
         * Client redirect URIs.
         */
        @WithName("redirect-uris")
        Set<String> redirectUris();
    }
}
