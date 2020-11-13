import java.util.function.Function;

public final class Starter implements Function<String, String> {

    @Override
    public String apply( String path ) {
        return "You sent me path " + path + "!\n";
    }
}
