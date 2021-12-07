package pl.bmalinowski.iwedzakv2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pl.bmalinowski.iwedzakv2.command.Command;
import pl.bmalinowski.iwedzakv2.command.DebugChangedCommand;
import pl.bmalinowski.iwedzakv2.command.ReceivedPayloadCommand;
import pl.bmalinowski.iwedzakv2.command.StopForegroundServiceCommand;
import pl.bmalinowski.iwedzakv2.command.TempRangeChangedCommand;
import pl.bmalinowski.iwedzakv2.command.UrlChangedCommand;
import pl.bmalinowski.iwedzakv2.model.NotificationDTO;
import pl.bmalinowski.iwedzakv2.model.Payload;
import pl.bmalinowski.iwedzakv2.model.TemperatureRange;
import pl.bmalinowski.iwedzakv2.model.URL;
import pl.bmalinowski.iwedzakv2.services.DataFetcherPort;
import pl.bmalinowski.iwedzakv2.services.LanScannerPort;
import pl.bmalinowski.iwedzakv2.services.NotificationPort;
import pl.bmalinowski.iwedzakv2.services.impl.ServiceFactory;

/**
 * service: https://stackoverflow.com/a/39675175/6760468
 */
public class ForegroundActivity extends Service {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final double MAX_DURATION_MIN = 4.5 * 60;

    private final ServiceFactory serviceFactory;
    private final List<BroadcastReceiver> receivers;
    private final ScheduledExecutorService executor;

    private Instant tempNotificationSendTime;
    private Instant timeNotificationSendTime;
    private TemperatureRange temperatureRange;
    private URL smokingHouseIp;
    private boolean debug;

    public ForegroundActivity() {
        this.serviceFactory = new ServiceFactory(this);
        this.temperatureRange = TemperatureRange.defaultRange();
        this.receivers = new ArrayList<>();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent.getAction().equals(StopForegroundServiceCommand.class.getName())) {
            stopForeground(true);
            stopSelfResult(startId);
            stopSelf();
        } else {
            handleDebugChanged(intent.getExtras().getBoolean("debug", false));
            registerReceiver();
            fetchData();
        }
        return START_STICKY;
    }

    private void fetchData() {
        final DataFetcherPort dataFetcherPort = serviceFactory.getDataFetcherPort();
        final Runnable job = () -> {
            final URL ip = smokingHouseIp;
            if (Objects.nonNull(ip)) {
                final Optional<Payload> payload = dataFetcherPort.collectData(ip);

                if (payload.isPresent()) {
                    verifyData(payload.get());
                } else {
                    findAndSetSmokingHouseIp();
                }
                publish(new ReceivedPayloadCommand(payload.orElse(null)));

            } else {
                publish(new ReceivedPayloadCommand(null));
                findAndSetSmokingHouseIp();
            }
        };
        executor.scheduleWithFixedDelay(job, 0, 5, TimeUnit.SECONDS);
    }

    private void findAndSetSmokingHouseIp() {
        final int port = debug ? 8000 : 80;
        final LanScannerPort lanScannerPort = serviceFactory.getScannerPort(port);

        final Optional<URL> ip = lanScannerPort.findExternalIP();
        setSmokingHouseIp(ip.orElse(null));
        publish(new UrlChangedCommand(ip.orElse(null)));
    }

    private String writeValueAsString(final Object o) {
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (final JsonProcessingException e) {
            return null;
        }
    }

    private void registerReceiver() {
        final BroadcastReceiver tempRangeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String json = intent.getStringExtra("json");
                try {
                    final TempRangeChangedCommand command = OBJECT_MAPPER.readValue(json, TempRangeChangedCommand.class);
                    handleTempRangeChanged(command.getTemperatureRange());
                } catch (final IOException e) {
                    handleTempRangeChanged(null);
                }
            }
        };
        final BroadcastReceiver debugBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String json = intent.getStringExtra("json");
                try {
                    final DebugChangedCommand command = OBJECT_MAPPER.readValue(json, DebugChangedCommand.class);
                    handleDebugChanged(command.isDebug());
                } catch (final IOException e) {
                    handleDebugChanged(false);
                    handleTempRangeChanged(null);
                }
            }
        };
        registerReceiver(tempRangeBroadcastReceiver, new IntentFilter(TempRangeChangedCommand.class.getName()));
        registerReceiver(debugBroadcastReceiver, new IntentFilter(DebugChangedCommand.class.getName()));
    }

    @Override
    public Intent registerReceiver(final BroadcastReceiver receiver, final IntentFilter filter) {
        receivers.add(receiver);
        return super.registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        receivers.stream()
                .filter(Objects::nonNull)
                .forEach(this::unregisterReceiver);
    }


    private void verifyData(final Payload payload) {
        synchronized (ForegroundActivity.class) {
            if (Objects.nonNull(temperatureRange) && Objects.nonNull(payload)) {
                if (!temperatureRange.inRange(payload.getTemp1()) || !temperatureRange.inRange(payload.getTemp2())) {
                    showTempOutOfRangeNotification(!temperatureRange.inRange(payload.getTemp1()) ? payload.getTemp1() : payload.getTemp2());
                }
                if (payload.getDuration().toMinutes() > MAX_DURATION_MIN) {
                    showDurationNotification(payload);
                }
            }
        }
    }

    private void publish(final Command command) {
        final Intent in = new Intent(command.getClass().getName());
        final Bundle extras = new Bundle();
        extras.putString("json", writeValueAsString(command));
        in.putExtras(extras);
        getBaseContext().sendBroadcast(in);
    }

    private void showTempOutOfRangeNotification(final Integer temp) {
        if (tempNotificationSendTime == null ||
                Instant.now().toEpochMilli() - tempNotificationSendTime.toEpochMilli() > Duration.ofMinutes(1).toMillis()) {
            final NotificationPort notificationPort = serviceFactory.getNotificationPort();
            notificationPort.showNotification(new NotificationDTO("iWędzok", "Temperatura wędzoka " + temp + " °C!"));
            tempNotificationSendTime = Instant.now();
        }
    }

    private void showDurationNotification(final Payload payload) {
        if (timeNotificationSendTime == null ||
                Instant.now().toEpochMilli() - timeNotificationSendTime.toEpochMilli() > Duration.ofMinutes(20).toMillis()) {
            final NotificationPort notificationPort = serviceFactory.getNotificationPort();
            notificationPort.showNotification(new NotificationDTO("iWędzok", "Czas wędzenia " + payload.durationAsLocalTime()));
            timeNotificationSendTime = Instant.now();
        }
    }

    private void setSmokingHouseIp(final URL url) {
        this.smokingHouseIp = url;
    }

    private void handleTempRangeChanged(final TemperatureRange temperatureRange) {
        synchronized (ForegroundActivity.class) {
            this.temperatureRange = temperatureRange;
        }
    }

    private void handleDebugChanged(final boolean value) {
        this.debug = value;
        setSmokingHouseIp(null);
    }
}
