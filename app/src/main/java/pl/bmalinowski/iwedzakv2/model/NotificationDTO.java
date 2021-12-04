package pl.bmalinowski.iwedzakv2.model;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class NotificationDTO {
    String title;
    String message;
}
