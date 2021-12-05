package pl.bmalinowski.iwedzakv2.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

@Value
public class DebugChangedCommand extends Command {
    boolean debug;

    @JsonCreator
    public DebugChangedCommand(@JsonProperty("debug") final boolean debug) {
        this.debug = debug;
    }
}
