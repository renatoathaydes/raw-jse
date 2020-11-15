final class NativeMain {
    public static void main( String[] args ) {
        System.out.println( "Starting server on port 8080" );
        var app = new Starter();
        new HttpServer( 8080 ).run( app );
    }
}
