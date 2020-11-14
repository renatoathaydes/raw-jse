import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

final class Main {
    final AtomicReference<AppRuntime> appRef = new AtomicReference<>();
    final String[] classPath;
    final String appClassName;

    public Main( String classPath, String appClassName ) throws IOException {
        this.classPath = classPath.split( ":" );
        this.appClassName = appClassName;
        System.out.printf( "classpath=%s, runnableClass=%s%n", classPath, appClassName );
        loadAndStartApp();
        new WatchDir( this.classPath, this::swapApp );
    }

    public static void main( String[] args ) throws IOException {
        if ( args.length != 2 ) {
            System.err.println( "Usage: java Main <classpath> <runnable-class>" );
            System.exit( 1 );
        }
        var classPath = args[ 0 ];
        var appClassName = args[ 1 ];
        new Main( classPath, appClassName );
    }

    private void swapApp() {
        var app = appRef.get();
        if ( app != null ) {
            try {
                app.loader.close();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
            var closeable = app.closeable;
            if ( closeable != null ) try {
                app.closeable.close();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
        loadAndStartApp();
    }

    private void loadAndStartApp() {
        loadApp().ifPresent( app -> start( app ) );
    }

    private Optional<AppRuntime> loadApp() {
        URL[] urls = new URL[ classPath.length ];
        try {
            for ( int i = 0; i < classPath.length; i++ ) {
                urls[ i ] = new File( classPath[ i ] ).toURI().toURL();
            }
        } catch ( MalformedURLException e ) {
            System.err.println( "Error creating class loader: " + e );
            return Optional.empty();
        }

        var appClassLoader = new URLClassLoader( urls, ClassLoader.getPlatformClassLoader() );

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

    private void start( AppRuntime app ) {
        Object starter;
        try {
            starter = app.appClass.getConstructor().newInstance();
        } catch ( Exception e ) {
            throw new RuntimeException( "Unable to start up application", e );
        }

        if ( starter instanceof Runnable ) {
            new Thread( ( Runnable ) starter ).start();
            if ( starter instanceof AutoCloseable ) {
                app.closeable = ( AutoCloseable ) starter;
            }
        } else {
            System.err.println( "Error: Cannot run application of type " + starter.getClass().getName() );
        }
    }

}

final class AppRuntime {
    final Class<?> appClass;
    final URLClassLoader loader;

    // if the app is started correctly and is Closeable,
    // this field will hold the app instance so we can close it before swapping it out
    AutoCloseable closeable;

    public AppRuntime( Class<?> appClass, URLClassLoader loader ) {
        this.appClass = appClass;
        this.loader = loader;
    }
}
