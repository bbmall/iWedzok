package pl.bmalinowski.iwedzakv2.services;

import java.util.Optional;

import pl.bmalinowski.iwedzakv2.model.SensorsDTO;
import pl.bmalinowski.iwedzakv2.model.URL;

public interface DataFetcherPort {
    Optional<SensorsDTO> collectData(URL URL);
}
