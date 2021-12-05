package pl.bmalinowski.iwedzakv2.services.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import pl.bmalinowski.iwedzakv2.model.Payload;
import pl.bmalinowski.iwedzakv2.model.URL;
import pl.bmalinowski.iwedzakv2.services.DataFetcherPort;

class DataFetcherMockAdapter implements DataFetcherPort {

    private final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

    @Override
    public Optional<Payload> collectData(final URL URL) {
        return Optional.of(new Payload(threadLocalRandom.nextInt(100), threadLocalRandom.nextInt(100), Duration.ZERO));
    }
}
