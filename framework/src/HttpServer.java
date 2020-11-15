import rawhttp.core.RawHttp;
import rawhttp.core.body.StringBody;
import rawhttp.core.server.TcpRawHttpServer;

import java.util.Optional;
import java.util.function.Function;

final class HttpServer {
    private final RawHttp http = new RawHttp();
    private final TcpRawHttpServer server;

    HttpServer( int port ) {
        this.server = new TcpRawHttpServer( port );
    }

    void run( Function<String, String> handler ) {
        server.start( ( req ) -> {
            var body = handler.apply( req.getUri().getPath() );
            if ( body == null ) {
                return Optional.empty();
            }
            return Optional.ofNullable( http.parseResponse( "200 OK" )
                    .withBody( new StringBody( body, "text/plain" ) ) );
        } );
    }

    void stop() {
        server.stop();
    }

}
