import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

final class Main {
    final AtomicReference<AppRuntime> appRef = new AtomicReference<>();
    final HttpServer server = new HttpServer( 8080 );
    final File classPathDir;
    final String appClassName;

    public Main( File classPathDir, String appClassName ) throws IOException {
        this.classPathDir = classPathDir;
        this.appClassName = appClassName;
        System.out.printf( "classpath=%s, runnableClass=%s%n", classPathDir, appClassName );
        loadAndStartApp();
        new WatchDir( classPathDir.toPath(), this::swapApp );
    }

    public static void main( String[] args ) throws IOException {
        if ( args.length != 2 ) {
            System.err.println( "Usage: java Main <classpath-dir> <runnable-class>" );
            System.exit( 1 );
        }
        var classPathDir = new File( args[ 0 ] );
        var appClassName = args[ 1 ];
        new Main( classPathDir, appClassName );
    }

    private void swapApp() {
        var app = appRef.get();
        if ( app != null ) {
            try {
                app.loader.close();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        loadAndStartApp();
    }

    private void loadAndStartApp() {
        loadApp().ifPresent( app -> start( app.appClass ) );
    }

    private Optional<AppRuntime> loadApp() {
        URLClassLoader appClassLoader;
        try {
            appClassLoader = new URLClassLoader( new URL[]{
                    classPathDir.toURI().toURL()
            }, ClassLoader.getPlatformClassLoader() );
        } catch ( MalformedURLException e ) {
            System.err.println( "Error creating class loader: " + e );
            return Optional.empty();
        }

        try {
            var app = new AppRuntime(
                    Class.forName( appClassName, true, appClassLoader ),
                    appClassLoader );
            appRef.set( app );
            return Optional.of( app );
        } catch ( ClassNotFoundException e ) {
            System.err.println( "Starter not found: " + e );
            return Optional.empty();
        }
    }

    private void start( Class<?> starterClass ) {
        Object starter;
        try {
            starter = starterClass.getConstructor().newInstance();
        } catch ( Exception e ) {
            throw new RuntimeException( "Unable to start up application", e );
        }

        Function<String, String> handler;
        if ( starter instanceof Function ) {
            //noinspection unchecked
            handler = ( Function<String, String> ) starter;
        } else {
            System.err.println( "Error: Cannot run application of type " + starter.getClass().getName() );
            handler = ignore -> null;
        }

        new Thread( () -> server.run( handler ) ).start();
    }

}

final class AppRuntime {
    final Class<?> appClass;
    final URLClassLoader loader;

    public AppRuntime( Class<?> appClass, URLClassLoader loader ) {
        this.appClass = appClass;
        this.loader = loader;
    }
}
