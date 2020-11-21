import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

class Build {
    static final File annotationsSrc = new File( "annotations/src" );
    static final File frameworkSrc = new File( "framework/src" );
    static final File processorsSrc = new File( "processors/src" );
    static final File processorsResources = new File( "processors/resources" );
    static final File appSrc = new File( "app/src" );
    static final File annotationsDist = new File( "dist/annotations" );
    static final File frameworkDist = new File( "dist/framework" );
    static final File processorsDist = new File( "dist/processors" );
    static final File appDist = new File( "dist/app" );
    static final List<String> runtimeFrameworkClasses = List.of(
            "http/HttpServer.class", "http/api/RequestHandlers.class" );

    public static void main( String[] args ) {
        timing( "Build SUCCESS", () -> {
            System.out.println( "Building..." );
            prepareCleanDir( frameworkDist, annotationsDist, processorsDist, appDist );
            compile( findJavaSources( frameworkSrc ), frameworkDist, "framework/libs/*" );
            copyDir( frameworkDist, appDist, runtimeFrameworkClasses );
            compile( findJavaSources( annotationsSrc ), annotationsDist );
            compile( findJavaSources( processorsSrc ), processorsDist, annotationsDist.getPath() );
            copyDir( processorsResources, processorsDist );
            compile( findJavaSources( appSrc ), appDist,
                    annotationsDist.getPath(),
                    appDist.getPath(), processorsDist.getPath(), "framework/libs/*" );
        } );
    }

    private static List<File> findJavaSources( File rootDir ) {
        return findJavaSources( rootDir, Set.of() );
    }

    private static List<File> findJavaSources( File rootDir, Set<String> excludes ) {
        if ( !rootDir.isDirectory() ) {
            failBuild( "Not a directory: " + rootDir );
        }
        try {
            return Files.walk( rootDir.toPath() )
                    .map( Path::toFile )
                    .filter( File::isFile )
                    .filter( f -> f.getName().endsWith( ".java" ) )
                    .filter( f -> !excludes.contains( f.getName() ) )
                    .collect( Collectors.toList() );
        } catch ( IOException e ) {
            throw failBuild( "Error trying to get Java sources at %s: %s", rootDir, e );
        }
    }

    private static void compile( List<File> files, File destinationDir, String... classpath ) {
        var cmd = new ArrayList<String>();
        cmd.add( "javac" );
        files.stream().map( File::getPath ).collect( toCollection( () -> cmd ) );
        cmd.addAll( List.of( "-d", destinationDir.getPath() ) );
        if ( classpath.length > 0 ) {
            cmd.addAll( List.of( "-cp", String.join( ":", classpath ) ) );
        }
        runCommand( cmd );
    }

    private static void runCommand( List<String> cmd ) {
        timing( "Command '" + String.join( " ", cmd ) + "' executed", () -> {
            int code;
            try {
                code = new ProcessBuilder( cmd )
                        .inheritIO().start().waitFor();
            } catch ( InterruptedException | IOException e ) {
                throw failBuild( "Cannot run command %s: %s", cmd.get( 0 ), e );
            }
            if ( code != 0 ) {
                failBuild( "Command failed (exitCode=%d): %s",
                        code, String.join( " ", cmd ) );
            }
        } );
    }

    private static void prepareCleanDir( File... dirs ) {
        for ( File dir : dirs ) {
            try {
                FileUtils.deleteDirectory( dir );
            } catch ( IOException e ) {
                failBuild( "Error deleting directory: " + dir + ": " + e );
            }
        }
    }

    private static void copyDir( File source, File target ) {
        copyDir( source, target, List.of() );
    }

    private static void copyDir( File source, File target, List<String> includes ) {
        var includePaths = includes.stream()
                .map( inc -> new File( source, inc ).getPath() )
                .collect( toSet() );
        try {
            FileUtils.copyDirectory( source, target, pathname -> {
                System.out.println( "Checking path: " + pathname );
                if ( includes.isEmpty() ) return true;
                if ( pathname.isDirectory() ) {
                    System.out.println( "Path is dir: " + pathname );
                    var pathDir = pathname + "/";
                    return includePaths.stream().anyMatch( inc -> inc.startsWith( pathDir ) );
                } else {
                    System.out.println( "Path is file: " + pathname );
                    return includePaths.contains( pathname.getPath() );
                }
            } );
        } catch ( IOException e ) {
            failBuild( "Error copying directory: " + source + ": " + e );
        }
    }

    private static void timing( String action, Runnable runnable ) {
        long start = System.nanoTime();
        runnable.run();
        System.out.printf( "%s in %.2f seconds%n", action,
                ( System.nanoTime() - start ) * Math.pow( 10, -9 ) );
    }

    private static RuntimeException failBuild( String format, Object... args ) {
        System.err.printf( "Build FAILED: " + format, args );
        throw new RuntimeException( "Build failure" );
    }
}