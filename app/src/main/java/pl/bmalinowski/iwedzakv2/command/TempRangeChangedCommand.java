package pl.bmalinowski.iwedzakv2.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;
import pl.bmalinowski.iwedzakv2.model.TemperatureRange;

@Value
public class TempRangeChangedCommand extends Command {
    TemperatureRange temperatureRange;

    @JsonCreator
    public TempRangeChangedCommand(@JsonProperty("temperatureRange") final TemperatureRange temperatureRange) {
        this.temperatureRange = temperatureRange;
    }
}
