package pl.bmalinowski.iwedzakv2.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class URLTest {
    @ParameterizedTest
    @ValueSource(strings = {"http://192.168.55.125:8000",
            "http://192.168.55.125",
            "http://1.168.55.125",
            "http://1.1.0.0",
    })
    public void testIpParser_passed() {
        assertEquals(new URL("http://192.168.55.125:8000"), URL.parseOrNull("http://192.168.55.125:8000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://192.168.55.125:8000",
            "http://192.168.55."
    })
    public void testIpParser_failed() {
        assertEquals(new URL("http://192.168.55.125:8000"), URL.parseOrNull("http://192.168.55.125:8000"));
    }

}