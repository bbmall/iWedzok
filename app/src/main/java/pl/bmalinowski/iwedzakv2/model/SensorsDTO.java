package pl.bmalinowski.iwedzakv2.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

import lombok.Value;

@Value
//TODO Nazwa jest z dupy :)
public class SensorsDTO {
    Integer temp1;
    Integer temp2;
    Duration duration;
    Instant currentTime = Instant.now();

    public LocalTime durationAsLocalTime() {
        return LocalTime.of((int) duration.toHours(),
                (int) duration.minusHours(duration.toHours()).toMinutes(),
                (int) duration.minusHours(duration.toHours()).minusMinutes(duration.minusHours(duration.toHours()).toMinutes()).getSeconds());
    }
}
