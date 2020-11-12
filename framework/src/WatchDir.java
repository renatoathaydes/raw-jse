// https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

final class WatchDir implements Closeable {

    private final WatchService watcher = FileSystems.getDefault().newWatchService();
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private final Runnable onChange;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    WatchDir( Path dir, Runnable onChange ) throws IOException {
        this.onChange = onChange;

        System.out.println( "Watching directory " + dir );
        try {
            registerAll( dir );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        executorService.scheduleAtFixedRate( new Runnable() {
            boolean changed;

            @Override
            public void run() {
                // only run callback if the files changed, but not since last run
                var wasChanged = changed;
                changed = processEvents();
                if ( wasChanged && !changed ) {
                    runCallback();
                }
            }
        }, 5, 2, TimeUnit.SECONDS );
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    private void runCallback() {
        System.out.println( "Running callback" );
        try {
            onChange.run();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
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
        var key = dir.register( watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY );
        keys.put( key, dir );
    }

    private boolean processEvents() {
        var changed = false;
        var key = watcher.poll();

        if ( key == null ) {
            return changed;
        }

        Path dir = keys.get( key );
        if ( dir == null ) {
            System.err.println( "WatchKey not recognized!!" );
            return changed;
        }

        for ( WatchEvent<?> event : key.pollEvents() ) {
            var kind = event.kind();

            if ( kind == OVERFLOW ) {
                continue;
            }

            Path name = ( Path ) event.context();
            Path child = dir.resolve( name );

            // print out event
            System.err.format( "%s: %s\n", event.kind().name(), child );

            changed = true;

            if ( kind == ENTRY_CREATE ) {
                try {
                    if ( Files.isDirectory( child, NOFOLLOW_LINKS ) ) {
                        registerAll( child );
                    }
                } catch ( IOException e ) {
                    System.err.printf( "Unable to start watching %s due to %s%n", child, e );
                }
            }
        }

        boolean valid = key.reset();
        if ( !valid ) {
            keys.remove( key );
        }

        return changed;
    }

}
