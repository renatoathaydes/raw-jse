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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

@SupportedAnnotationTypes( "raw.jse.http.HttpEndpoint" )
@SupportedSourceVersion( SourceVersion.RELEASE_11 )
public final class HttpAnnotationProcessor extends AbstractProcessor {

    private static final Pattern handlersLinePattern =
            Pattern.compile( "\\s+/\\*\\s+ADD HANDLERS HERE\\s+\\*/\\s*" );

    @Override
    public boolean process( Set<? extends TypeElement> annotations,
                            RoundEnvironment roundEnv ) {
        if ( annotations.isEmpty() ) return false;

        var classNames = getEndpoints( annotations, roundEnv );

        try {
            try ( var reader = readCodegenTemplateRequestHandlers() ) {
                writeMain( classNames, reader.lines().iterator() );
            }
        } catch ( IOException e ) {
            processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR,
                    "Could not write Main due to: " + e );
        }

        return true;
    }

    private BufferedReader readCodegenTemplateRequestHandlers() throws IOException {
        return Files.newBufferedReader( Paths.get( "framework", "src", "http",
                "CodegenTemplateRequestHandlers.java" ) );
    }

    private List<Endpoint> getEndpoints( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
        return annotations.stream()
                .flatMap( annotation -> roundEnv.getElementsAnnotatedWith( annotation ).stream() )
                .map( element -> {
                    processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE,
                            "found @HttpEndpoint at " + element );
                    var httpEndpoint = element.getAnnotation( HttpEndpoint.class );
                    return new Endpoint( ( ( TypeElement ) element ), httpEndpoint.path() );
                } ).collect( toList() );
    }

    private List<? extends Element> getPublicMethods( TypeElement element,
                                                      Class<? extends Annotation> annotationType ) {
        return processingEnv.getElementUtils().getAllMembers( element ).stream()
                .filter( e -> e.getKind() == ElementKind.METHOD )
                .filter( e -> e.getModifiers().contains( Modifier.PUBLIC ) )
                .filter( e -> e.getAnnotationsByType( annotationType ).length > 0 )
                .collect( toList() );
    }

    private void writeMain( List<Endpoint> endpoints, Iterator<String> templateLines ) throws IOException {
        var filer = processingEnv.getFiler();
        var fileObject = filer.createSourceFile( "http.AppRequestHandlers" );

        try ( var writer = fileObject.openWriter() ) {
            var handlersLineFound = false;
            while ( templateLines.hasNext() ) {
                var line = templateLines.next();
                if ( !handlersLineFound && handlersLinePattern.matcher( line ).find() ) {
                    handlersLineFound = true;
                    var indent = " ".repeat( ( int ) line.chars().takeWhile( c -> c == ' ' ).count() );
                    writeEndpointCreators( indent, endpoints, writer );
                    writer.write( '\n' );
                    writeHandlers( indent, endpoints, writer );
                } else {
                    writer.write( line.replace( "CodegenTemplateRequestHandlers", "AppRequestHandlers" ) );
                    writer.write( '\n' );
                }
            }
        }
    }

    private void writeEndpointCreators( String indent, List<Endpoint> endpoints, Writer writer ) throws IOException {
        for ( Endpoint endpoint : endpoints ) {
            writer.write( indent + "var " + endpoint.getVarName() + " = new " +
                    endpoint.classElement.getQualifiedName() + "();\n" );
        }
    }

    private void writeHandlers( String indent, List<Endpoint> endpoints, Writer writer ) throws IOException {
        for ( Endpoint endpoint : endpoints ) {
            for ( Element method : getPublicMethods( endpoint.classElement, GET.class ) ) {
                var subPath = method.getAnnotation( GET.class ).path();
                writeHandlers( indent, method, endpoint, "getHandlers", subPath, writer );
            }
            for ( Element method : getPublicMethods( endpoint.classElement, POST.class ) ) {
                var subPath = method.getAnnotation( POST.class ).path();
                writeHandlers( indent, method, endpoint, "postHandlers", subPath, writer );
            }
        }
    }

    private void writeHandlers( String indent, Element method, Endpoint endpoint, String varName, String subPath, Writer writer ) throws IOException {
        writer.write( indent + varName + ".put(" +
                "\"" + joinPaths( endpoint.path, subPath ) + "\", " +
                endpoint.getVarName() + "::" + method.getSimpleName().toString() + ");\n" );
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

    String getVarName() {
        return classElement.getSimpleName().toString().toLowerCase();
    }
}