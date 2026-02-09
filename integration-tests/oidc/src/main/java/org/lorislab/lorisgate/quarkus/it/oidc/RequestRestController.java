package org.lorislab.lorisgate.quarkus.it.oidc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import gen.org.lorislab.lorisgate.quarkus.it.oidc.client.api.BackendApi;
import gen.org.lorislab.lorisgate.quarkus.it.oidc.rs.RequestApiService;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
public class RequestRestController implements RequestApiService {

    @Inject
    @RestClient
    BackendApi backendApi;

    @Override
    public Response testOidcClient() {
        try (var response = backendApi.testOidcService()) {
            var data = response.readEntity(Object.class);
            return Response.ok(data).build();
        }
    }
}
