package pl.bmalinowski.iwedzakv2.services;

import java.util.Optional;

import pl.bmalinowski.iwedzakv2.model.Payload;
import pl.bmalinowski.iwedzakv2.model.URL;

public interface DataFetcherPort {
    Optional<Payload> collectData(URL URL);
}
