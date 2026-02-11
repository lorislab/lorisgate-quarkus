package org.lorislab.lorisgate.quarkus.it.oidc;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Test;

import gen.org.lorislab.lorisgate.quarkus.it.oidc.rs.model.TokenDTO;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(RequestRestController.class)
class RequestRestControllerTest {

    @Test
    void testClient() {
        var token = given()
                .when()
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(TokenDTO.class);

        assertThat(token).isNotNull();
        assertThat(token.getRaw()).isNotNull().isNotBlank();
        assertThat(token.getJson()).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, ?> json = (Map<String, ?>) token.getJson();

        System.out.println("Token JSON: " + json);

        @SuppressWarnings("unchecked")
        Map<String, ?> claims = (Map<String, ?>) json.get("claimsMap");

        assertThat(claims).isNotNull().isNotEmpty();
        assertThat(claims.get(Claims.azp.name())).isNotNull().isEqualTo("quarkus-app");
        assertThat(claims.get("typ")).isNotNull().isEqualTo("Bearer");
    }

}
