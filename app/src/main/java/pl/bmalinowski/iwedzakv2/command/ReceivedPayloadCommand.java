package pl.bmalinowski.iwedzakv2.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;
import pl.bmalinowski.iwedzakv2.model.Payload;

@Value
public class ReceivedPayloadCommand extends Command {
    Payload payload;
    
    @JsonCreator
    public ReceivedPayloadCommand(@JsonProperty("payload") Payload payload) {
        this.payload = payload;
    }
}
