package pl.bmalinowski.iwedzakv2.services.impl;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import pl.bmalinowski.iwedzakv2.model.Payload;

class DataFetcherAdapterTest {


    @Test
//    @DisplayName("1 + 1 = 2")
    void parseResponseTest() {
        final DataFetcherAdapter sut = new DataFetcherAdapter(null);

        final Optional<Payload> actual = sut.collectData(null);
        assertEquals(73, actual.get().getTemp1());
        assertEquals(21, actual.get().getTemp2());
        assertEquals(Duration.ofSeconds(87), actual.get().getDuration());
    }
}