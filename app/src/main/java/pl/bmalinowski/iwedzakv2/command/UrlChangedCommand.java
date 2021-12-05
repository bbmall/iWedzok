package pl.bmalinowski.iwedzakv2.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;
import pl.bmalinowski.iwedzakv2.model.URL;

@Value
public class UrlChangedCommand extends Command {
    URL url;

    @JsonCreator
    public UrlChangedCommand(@JsonProperty("url") final URL url) {
        this.url = url;
    }
}
