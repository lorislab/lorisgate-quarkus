package org.lorislab.lorisgate.quarkus.deployment.devservices;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gen.org.lorislab.lorisgate.client.admin.v1.model.ClientV1DTO;
import gen.org.lorislab.lorisgate.client.admin.v1.model.RealmV1DTO;
import gen.org.lorislab.lorisgate.client.admin.v1.model.RoleV1DTO;
import gen.org.lorislab.lorisgate.client.admin.v1.model.UserV1DTO;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpHeaders;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class LorisgateClient implements AutoCloseable {

    static final String DEFAULT_CLIENT_ID = "quarkus-app";

    static final String DEFAULT_CLIENT_PUBLIC_ID = "quarkus-app-public";

    static final String DEFAULT_CLIENT_SECRET = "secret";

    private static final Logger log = LoggerFactory.getLogger(LorisgateClient.class);

    private static final int TIMEOUT = 300;

    private final String url;

    private final int timeout;

    private final Vertx vertx;

    private final WebClient webClient;

    public static LorisgateClient create(String url) {
        return create(url, TIMEOUT);
    }

    public static LorisgateClient create(String url, int timeout) {
        return new LorisgateClient(url, timeout);
    }

    private LorisgateClient(String url, int timeout) {
        this.url = url;
        this.timeout = timeout;
        this.vertx = Vertx.vertx();
        this.webClient = createWebClient(vertx);
    }

    public void createIfNotExistsRealm(RealmV1DTO realm) {
        var r = getRealm(realm.getName());
        if (r == null) {
            createRealm(realm);
            log.info("Realm '{}' created in the lorisgate server.", realm.getName());
        } else {
            log.warn("Realm '{}' already exists.", r.getName());
        }
    }

    public RealmV1DTO getRealm(String realm) {
        HttpResponse<Buffer> createRealmResponse = webClient.getAbs(adminRealmsUrl(realm))
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
                .send()
                .await().atMost(Duration.ofSeconds(timeout));

        if (createRealmResponse.statusCode() == HttpResponseStatus.NOT_FOUND.code()) {
            return null;
        }
        if (createRealmResponse.statusCode() != HttpResponseStatus.OK.code()) {
            throw new RuntimeException("Failed to get '" + realm + "' realm in lorisgate dev service, status: "
                    + createRealmResponse.statusCode() + ", body: " + createRealmResponse.bodyAsString());
        }
        System.out.println("Response: " + createRealmResponse.bodyAsString());
        return createRealmResponse.bodyAsJson(RealmV1DTO.class);
    }

    public void createRealm(RealmV1DTO realm) {
        HttpResponse<Buffer> createRealmResponse = webClient.postAbs(adminRealmsUrl())
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
                .sendBuffer(Buffer.buffer().appendString(Json.encode(realm)))
                .await().atMost(Duration.ofSeconds(timeout));

        if (createRealmResponse.statusCode() != HttpResponseStatus.CREATED.code()) {
            throw new RuntimeException("Failed to create quarkus realm in lorisgate dev service, status: "
                    + createRealmResponse.statusCode() + ", body: " + createRealmResponse.bodyAsString());
        }
    }

    private String adminRealmsUrl(String realm) {
        return url + "/admin/realms/" + realm;
    }

    private String adminRealmsUrl() {
        return url + "/admin/realms";
    }

    private WebClient createWebClient(Vertx vertx) {
        WebClientOptions options = new WebClientOptions();
        options.setTrustAll(true);
        options.setVerifyHost(false);
        return WebClient.create(new io.vertx.mutiny.core.Vertx(vertx), options);
    }

    @Override
    public void close() {
        if (webClient != null) {
            webClient.close();
        }
        if (vertx != null) {
            vertx.close();
        }
    }

    public static RealmV1DTO createDefaultRealm(String realm, LorisgateDevServicesConfig.DefaultRealmConfig config) {
        var result = new RealmV1DTO().displayName(realm).name(realm).enabled(config.enabled());

        if (config.createUsers()) {
            result.users(
                    Map.of(
                            "alice",
                            new UserV1DTO().enabled(true).name("alice").id("alice").password("alice").username("alice")
                                    .emailVerified(true)
                                    .email("alice@localhost")
                                    .roles(Set.of("admin", "user")),
                            "bob",
                            new UserV1DTO().enabled(true).name("bob").id("bob").password("bob").username("bob")
                                    .emailVerified(true)
                                    .email("bob@localhost")
                                    .roles(Set.of("user"))));
        }

        if (config.createClients()) {

            result.clients(
                    Map.of(
                            DEFAULT_CLIENT_ID,
                            new ClientV1DTO().clientId(DEFAULT_CLIENT_ID).clientSecret(DEFAULT_CLIENT_SECRET).confidential(true)
                                    .scopes(Set.of("openid", "profile", "email")),
                            "quarkus-app-public",
                            new ClientV1DTO().clientId(DEFAULT_CLIENT_PUBLIC_ID).confidential(false)
                                    .scopes(Set.of("openid", "profile", "email")).redirectUris(Set.of("*"))));
        }

        if (config.createRoles()) {
            result.roles(
                    Map.of("admin", new RoleV1DTO().name("admin").description("Admin role").enabled(true),
                            "user", new RoleV1DTO().name("user").description("User role").enabled(true)));
        }

        addRolesUsersClients(result, config.users(), config.roles(), config.clients());
        return result;
    }

    public static RealmV1DTO createRealm(String realm, LorisgateDevServicesConfig.RealmConfig config) {
        var result = new RealmV1DTO().displayName(realm).name(realm).enabled(config.enabled());
        addRolesUsersClients(result, config.users(), config.roles(), config.clients());
        return result;
    }

    private static void addRolesUsersClients(RealmV1DTO result,
            Map<String, LorisgateDevServicesConfig.RealmUserConfig> users,
            Map<String, LorisgateDevServicesConfig.RealmRoleConfig> roles,
            Map<String, LorisgateDevServicesConfig.RealmClientConfig> clients) {
        if (users != null && !users.isEmpty()) {
            for (var entry : users.entrySet()) {
                result.putUsersItem(entry.getKey(), createUser(entry.getKey(), entry.getValue()));
            }
        }

        if (roles != null && !roles.isEmpty()) {
            for (var entry : roles.entrySet()) {
                result.putRolesItem(entry.getKey(), createRole(entry.getKey(), entry.getValue()));
            }
        }

        if (clients != null && !clients.isEmpty()) {
            for (var entry : clients.entrySet()) {
                result.putClientsItem(entry.getKey(), createClient(entry.getKey(), entry.getValue()));
            }
        }
    }

    private static RoleV1DTO createRole(String name, LorisgateDevServicesConfig.RealmRoleConfig role) {
        return new RoleV1DTO().name(name).description(role.description()).enabled(role.enabled());
    }

    private static UserV1DTO createUser(String username, LorisgateDevServicesConfig.RealmUserConfig config) {
        return new UserV1DTO()
                .username(username)
                .name(config.name().orElse(null))
                .email(config.email().orElse(null))
                .password(config.password())
                .enabled(config.enabled())
                .emailVerified(config.emailVerified())
                .roles(config.roles());
    }

    private static ClientV1DTO createClient(String clientId, LorisgateDevServicesConfig.RealmClientConfig config) {
        return new ClientV1DTO()
                .clientId(clientId)
                .clientSecret(config.clientSecret())
                .confidential(config.confidential())
                .scopes(config.scopes())
                .redirectUris(config.redirectUris());
    }
}
