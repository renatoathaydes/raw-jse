package http;

import http.api.RequestHandlers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Template for codegen by {@code HttpAnnotationProcessor}.
 */
public final class CodegenTemplateRequestHandlers implements RequestHandlers {

    private final Map<String, Supplier<String>> getHandlers;
    private final Map<String, Function<String, String>> postHandlers;

    public CodegenTemplateRequestHandlers() {
        getHandlers = new HashMap<>();
        postHandlers = new HashMap<>();

        /* ADD HANDLERS HERE */
    }

    @Override
    public Map<String, Supplier<String>> getGetHandlers() {
        return getHandlers;
    }

    @Override
    public Map<String, Function<String, String>> getPostHandlers() {
        return postHandlers;
    }
}
