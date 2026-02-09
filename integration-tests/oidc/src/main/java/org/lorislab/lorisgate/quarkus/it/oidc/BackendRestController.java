package org.lorislab.lorisgate.quarkus.it.oidc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwx.JsonWebStructure;

import gen.org.lorislab.lorisgate.quarkus.it.oidc.rs.BackendApiService;
import gen.org.lorislab.lorisgate.quarkus.it.oidc.rs.model.TokenDTO;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
public class BackendRestController implements BackendApiService {

    @Inject
    JsonWebToken jwt;

    @Override
    public Response testOidcService() {
        var claims = parseClaims(jwt.getRawToken());
        return Response.ok(new TokenDTO().raw(jwt.getRawToken()).json(claims)).build();
    }

    protected JwtClaims parseClaims(String token) {
        try {
            var jws = (JsonWebSignature) JsonWebStructure.fromCompactSerialization(token);
            return JwtClaims.parse(jws.getUnverifiedPayload());
        } catch (Exception ex) {
            throw new ClaimsException(ex);
        }
    }

    public static class ClaimsException extends RuntimeException {

        public ClaimsException(Throwable t) {
            super(t);
        }
    }
}
