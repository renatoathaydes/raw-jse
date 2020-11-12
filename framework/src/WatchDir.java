// https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

final class WatchDir implements Closeable {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final Runnable onChange;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    WatchDir( Path dir, Runnable onChange ) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        this.onChange = onChange;

        executorService.submit( () -> {
            System.out.println( "Watching directory " + dir );
            try {
                registerAll( dir );
                processEvents();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        } );
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    private void registerAll( final Path start ) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree( start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs )
                    throws IOException {
                register( dir );
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    private void register( Path dir ) throws IOException {
        WatchKey key = dir.register( watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY );
        keys.put( key, dir );
    }

    private void processEvents() {
        for ( ; ; ) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch ( InterruptedException x ) {
                break;
            }

            Path dir = keys.get( key );
            if ( dir == null ) {
                System.err.println( "WatchKey not recognized!!" );
                continue;
            }

            for ( WatchEvent<?> event : key.pollEvents() ) {
                WatchEvent.Kind kind = event.kind();

                if ( kind == OVERFLOW ) {
                    continue;
                }

                Path name = ( Path ) event.context();
                Path child = dir.resolve( name );

                // print out event
                System.out.format( "%s: %s\n", event.kind().name(), child );

                try {
                    onChange.run();
                } catch ( Exception e ) {
                    e.printStackTrace();
                }

                if ( kind == ENTRY_CREATE ) {
                    try {
                        if ( Files.isDirectory( child, NOFOLLOW_LINKS ) ) {
                            registerAll( child );
                        }
                    } catch ( IOException x ) {
                        // ignore to keep sample readbale
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if ( !valid ) {
                keys.remove( key );

                // all directories are inaccessible
                if ( keys.isEmpty() ) {
                    break;
                }
            }
        }

        System.out.println( "Stopping watching root directory" );
        close();
    }

}