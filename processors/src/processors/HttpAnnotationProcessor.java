package processors;

import raw.jse.http.GET;
import raw.jse.http.HttpEndpoint;
import raw.jse.http.POST;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes( "raw.jse.http.HttpEndpoint" )
@SupportedSourceVersion( SourceVersion.RELEASE_11 )
public final class HttpAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process( Set<? extends TypeElement> annotations,
                            RoundEnvironment roundEnv ) {
        if ( annotations.isEmpty() ) return false;

        var classNames = new ArrayList<Endpoint>();
        for ( var annotation : annotations ) {
            for ( var element : roundEnv.getElementsAnnotatedWith( annotation ) ) {
                processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE,
                        "found @HttpEndpoint at " + element );
                var httpEndpoint = element.getAnnotation( HttpEndpoint.class );
                classNames.add( new Endpoint( ( ( TypeElement ) element ), httpEndpoint.path() ) );
            }
        }

        try {
            writeMain( classNames );
        } catch ( IOException e ) {
            processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR,
                    "Could not write Main due to: " + e );
        }

        return true;
    }

    private List<? extends Element> getPublicMethods( TypeElement element,
                                                      Class<? extends Annotation> annotationType ) {
        return processingEnv.getElementUtils().getAllMembers( element ).stream()
                .filter( e -> e.getKind() == ElementKind.METHOD )
                .filter( e -> e.getModifiers().contains( Modifier.PUBLIC ) )
                .filter( e -> e.getAnnotationsByType( annotationType ).length > 0 )
                .collect( Collectors.toList() );
    }

    private void writeMain( List<Endpoint> endpoints ) throws IOException {
        var filer = processingEnv.getFiler();
        var fileObject = filer.createSourceFile( "http.Main" );
        var getMapName = "getHandlers";
        var postMapName = "postHandlers";

        try ( var writer = fileObject.openWriter() ) {
            writer.write( "package http;\n" +
                    "\n" +
                    "import rawhttp.core.RawHttp;\n" +
                    "import rawhttp.core.body.StringBody;\n" +
                    "import rawhttp.core.server.TcpRawHttpServer;\n" +
                    "\n" +
                    "import java.io.IOException;\n" +
                    "import java.nio.charset.StandardCharsets;\n" +
                    "import java.util.HashMap;\n" +
                    "import java.util.Optional;\n" +
                    "import java.util.function.Function;\n" +
                    "import java.util.function.Supplier;\n" +
                    "\n" +
                    "public class Main implements Runnable, AutoCloseable {\n" +
                    "    private static final int port = 8080;\n" +
                    "    private final RawHttp http = new RawHttp();\n" +
                    "    private final TcpRawHttpServer server;\n" +
                    "\n" +
                    "    public Main() {\n" +
                    "        this.server = new TcpRawHttpServer( port );\n" +
                    "    }\n" +
                    "    @Override public void run() {\n" );
            writeEndpointCreators( "        ", endpoints, writer );
            writer.write( '\n' );
            writeGetMap( "        ", getMapName, endpoints, writer );
            writer.write( '\n' );
            writePostMap( "        ", postMapName, endpoints, writer );
            writer.write( '\n' );
            writer.write( "" +
                    "        server.start( ( req ) -> {\n" +
                    "            String body = null;\n" +
                    "            var path = req.getUri().getPath();\n" +
                    "            if ( req.getMethod().equals( \"GET\" ) ) {\n" +
                    "                var h = " + getMapName + ".get( path );\n" +
                    "                if ( h != null ) {\n" +
                    "                    body = h.get();\n" +
                    "                }\n" +
                    "            } else if ( req.getMethod().equals( \"POST\" ) ) {\n" +
                    "                var h = " + postMapName + ".get( path );\n" +
                    "                if ( h != null ) {\n" +
                    "                    var reqBody = req.getBody().map(\n" +
                    "                            b -> {\n" +
                    "                                try {\n" +
                    "                                    return b.decodeBodyToString( StandardCharsets.UTF_8 );\n" +
                    "                                } catch ( IOException e ) {\n" +
                    "                                    throw new RuntimeException( e );\n" +
                    "                                }\n" +
                    "                            } ).orElse( \"\" );\n" +
                    "                    body = h.apply( reqBody );\n" +
                    "                }\n" +
                    "            }\n" +
                    "            if ( body == null ) {\n" +
                    "                return Optional.empty();\n" +
                    "            }\n" +
                    "            return Optional.of( http.parseResponse( \"200 OK\" )\n" +
                    "                    .withBody( new StringBody( body, \"text/plain\" ) ) );\n" +
                    "        } );" +
                    "    }\n" +
                    "    @Override public void close() {\n" +
                    "        server.stop();\n" +
                    "    }\n" +
                    "\n" +
                    "    public static void main(String... args) {\n" +
                    "        new Main().run();\n" +
                    "        System.out.println(\"Server running on port \" + port);" +
                    "    }\n" +
                    "}" );
        }
    }

    private void writeEndpointCreators( String indent, List<Endpoint> endpoints, Writer writer ) throws IOException {
        for ( Endpoint endpoint : endpoints ) {
            writer.write( indent + "var " + endpoint.classElement.getSimpleName() + " = new " +
                    endpoint.classElement.getQualifiedName() + "();\n" );
        }
    }

    private void writeGetMap( String indent, String name, List<Endpoint> endpoints, Writer writer ) throws IOException {
        writer.write( indent + "var " + name + " = new HashMap<String, Supplier<String>>();\n" );
        for ( Endpoint endpoint : endpoints ) {
            for ( Element method : getPublicMethods( endpoint.classElement, GET.class ) ) {
                var subPath = method.getAnnotation( GET.class ).path();
                writer.write( indent + name + ".put(" +
                        "\"" + joinPaths( endpoint.path, subPath ) + "\", " +
                        endpoint.classElement.getSimpleName() + "::" + method.getSimpleName().toString() + ");\n" );
            }
        }
    }

    private void writePostMap( String indent, String name, List<Endpoint> endpoints, Writer writer ) throws IOException {
        writer.write(
                indent + "var " + name + " = new HashMap<String, Function<String, String>>();\n" );
        for ( Endpoint endpoint : endpoints ) {
            for ( Element method : getPublicMethods( endpoint.classElement, POST.class ) ) {
                var subPath = method.getAnnotation( POST.class ).path();
                writer.write( indent + name + ".put(" +
                        "\"" + joinPaths( endpoint.path, subPath ) + "\", " +
                        endpoint.classElement.getSimpleName() + "::" + method.getSimpleName().toString() + ");\n" );
            }
        }
    }

    private String joinPaths( String p1, String p2 ) {
        if ( p2.isEmpty() || p2.equals( "/" ) ) {
            return p1;
        }
        var p1EndsWithSlash = p1.endsWith( "/" );
        var p2StartsWithSlash = p2.startsWith( "/" );
        if ( p1EndsWithSlash && p2StartsWithSlash ) {
            p2 = p2.substring( 1 );
        }
        if ( !p1EndsWithSlash && !p2StartsWithSlash ) {
            p1 = p1 + "/";
        }
        return p1 + p2;
    }

}

class Endpoint {
    final TypeElement classElement;
    final String path;

    public Endpoint( TypeElement classElement, String path ) {
        this.classElement = classElement;
        this.path = path;
    }
}