package org.lorislab.lorisgate.quarkus.it.odic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("test")
public class TestRestController {

    @GET
    public Response test() {
        return Response.ok().build();
    }
}
