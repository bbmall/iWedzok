package pl.bmalinowski.iwedzakv2.command;

import lombok.Value;
import pl.bmalinowski.iwedzakv2.model.TemperatureRange;

@Value
public class TempRangeChangedCommand {
    TemperatureRange temperatureRange;
}
