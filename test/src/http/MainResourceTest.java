package http;

import app.MainResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainResourceTest {

    @Test
    @DisplayName( "GET requests should receive the Hello World response" )
    public void canSayHello() {
        var resource = new MainResource();
        assertEquals( "Hello world!\n", resource.hello() );
    }

}
