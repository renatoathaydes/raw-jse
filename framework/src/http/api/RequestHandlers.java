package http.api;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface RequestHandlers {
    default Map<String, Supplier<String>> getGetHandlers() {
        return Map.of();
    }

    default Map<String, Function<String, String>> getPostHandlers() {
        return Map.of();
    }
}
