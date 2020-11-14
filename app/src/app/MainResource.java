package app;

import raw.jse.http.GET;
import raw.jse.http.HttpEndpoint;
import raw.jse.http.POST;

@HttpEndpoint( path = "/" )
public final class MainResource {
    @GET
    public String index() {
        return "Welcome to the main resource.\n";
    }

    @GET( path = "hello" )
    public String hello() {
        return "Hello world!\n";
    }

    @POST
    public String index( String body ) {
        return "Got body: " + body;
    }
}
