package pl.bmalinowski.iwedzakv2.services;

import pl.bmalinowski.iwedzakv2.model.NotificationDTO;

public interface NotificationPort {
    void showNotification(final NotificationDTO notificationDTO);
}
