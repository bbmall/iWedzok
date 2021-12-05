package pl.bmalinowski.iwedzakv2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.method.LinkMovementMethod;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pl.bmalinowski.iwedzakv2.command.DebugCommand;
import pl.bmalinowski.iwedzakv2.command.TempRangeChangedCommand;
import pl.bmalinowski.iwedzakv2.model.NotificationDTO;
import pl.bmalinowski.iwedzakv2.model.SensorsDTO;
import pl.bmalinowski.iwedzakv2.model.TemperatureRange;
import pl.bmalinowski.iwedzakv2.model.URL;
import pl.bmalinowski.iwedzakv2.services.DataFetcherPort;
import pl.bmalinowski.iwedzakv2.services.LanScannerPort;
import pl.bmalinowski.iwedzakv2.services.NotificationPort;
import pl.bmalinowski.iwedzakv2.services.impl.ServiceFactory;

/**
 * service: https://stackoverflow.com/a/39675175/6760468
 */
public class NotificationService extends Service {

    private final double MAX_DURATION_MIN = 4.5 * 60;
    private final ScheduledExecutorService executor;
    private final ServiceFactory serviceFactory;
    private Instant tempNotificationSendTime;
    private Instant timeNotificationSendTime;
    private BroadcastReceiver tempRangeBroadcastReceiver;
    private BroadcastReceiver debugBroadcastReceiver;
    private TemperatureRange temperatureRange;
    private final ObjectMapper objectMapper;
    private URL smokingHouseIp;
    private boolean debug;

    public NotificationService() {
        this.serviceFactory = new ServiceFactory(this);
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.temperatureRange = TemperatureRange.defaultRange();
        this.objectMapper = new ObjectMapper();
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        registerReceiver();

//        process();
        supplyData();
        return START_STICKY;
    }

    private void supplyData() {
        final DataFetcherPort dataFetcherPort = serviceFactory.getDataFetcherPort();
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final Runnable job = () -> {
            if (Optional.ofNullable(smokingHouseIp).isPresent()) {
                final Optional<SensorsDTO> sensorsDTO = dataFetcherPort.collectData(smokingHouseIp);

                final Intent in = new Intent(SensorsDTO.class.getName());
                final Bundle extras = new Bundle();
                extras.putString("json", writeValueAsString(sensorsDTO.orElse(null)));
                in.putExtras(extras);
                getBaseContext().sendBroadcast(in);

            } else {
                final Intent in = new Intent(SensorsDTO.class.getName());
                final Bundle extras = new Bundle();
                extras.putString("json", null);
                in.putExtras(extras);
                getBaseContext().sendBroadcast(in);
                findAndSetSmokingHouseIp();
            }
        };
        executorService.scheduleWithFixedDelay(job, 0, 2, TimeUnit.SECONDS);
    }

    private void findAndSetSmokingHouseIp() {
        final SwitchCompat switchCompat = findViewById(R.id.debugSwitch);
        final int port = switchCompat.isChecked() ? 8000 : 80;
        final LanScannerPort lanScannerPort = serviceFactory.getScannerPort(port);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Runnable job = () -> {
            final Optional<URL> ip = lanScannerPort.findExternalIP();
            runOnUiThread(() -> {
                if (ip.isPresent()) {
                    txtIp.setClickable(true);
                    txtIp.setMovementMethod(LinkMovementMethod.getInstance());
                    final String link = String.format("<a href='%s'> %s </a>", ip.get(), ip.get());
                    txtIp.setText(Html.fromHtml(link, Html.FROM_HTML_MODE_COMPACT));

                } else {
                    //Tu chyba nie powinno  to tyle razy wchodzić. jeno raz
                    txtIp.setClickable(false);
                    txtIp.setText("No connection");
                }
                this.setSmokingHouseIp(ip.orElse(null));
            });
        };
        executorService.execute(job);
    }

    private String writeValueAsString(final SensorsDTO sensorsDTO) {
        try {
            return objectMapper.writeValueAsString(sensorsDTO);
        } catch (final JsonProcessingException e) {
            return null;
        }
    }

    private void registerReceiver() {
        tempRangeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                try {
                    final String json = intent.getStringExtra("json");
                    final TempRangeChangedCommand command = objectMapper.readValue(json, TempRangeChangedCommand.class);
                    setTemperatureRange(command.getTemperatureRange());
                } catch (final IOException e) {
                    setTemperatureRange(null);
                }
            }
        };
        debugBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                try {
                    final String json = intent.getStringExtra("json");
                    final DebugCommand command = objectMapper.readValue(json, DebugCommand.class);
                    setDebug(command.isDebug());
                } catch (final IOException e) {
                    setTemperatureRange(null);
                }
            }
        };
        registerReceiver(tempRangeBroadcastReceiver, new IntentFilter(TemperatureRange.class.getName()));
        registerReceiver(debugBroadcastReceiver, new IntentFilter(DebugCommand.class.getName()));
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        if (tempRangeBroadcastReceiver != null) {
            unregisterReceiver(tempRangeBroadcastReceiver);
        }
    }


    private void process() {
        final DataFetcherPort dataFetcherPort = serviceFactory.getDataFetcherPort();
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final Runnable job = () -> {
            synchronized (NotificationService.class) {
                if (getSmokingHouseIp().isPresent() && Objects.nonNull(temperatureRange)) {
                    final Optional<SensorsDTO> sensorsDTO = dataFetcherPort.collectData(getSmokingHouseIp().get());
                    if (sensorsDTO.isPresent()) {
                        final SensorsDTO dto = sensorsDTO.get();
                        if (!temperatureRange.inRange(dto.getTemp1()) || !temperatureRange.inRange(dto.getTemp2())) {
                            showTempOutOfRangeNotification(!temperatureRange.inRange(dto.getTemp1()) ? dto.getTemp1() : dto.getTemp2());
                        }
                        if (dto.getDuration().toMinutes() > MAX_DURATION_MIN) {
                            showDurationNotification(dto);
                        }
                    }
                }

                publish();
            }
        };
        executorService.scheduleWithFixedDelay(job, 0, 2, TimeUnit.SECONDS);

    }

    private void publish() {
        final Intent in = new Intent("com.an.sms.example");
        final Bundle extras = new Bundle();
        extras.putString("com.an.sms.example.otp", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxxXXXXXXXXXXXXXXX");
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

    private void showDurationNotification(final SensorsDTO sensorsDTO) {
        if (timeNotificationSendTime == null ||
                Instant.now().toEpochMilli() - timeNotificationSendTime.toEpochMilli() > Duration.ofMinutes(20).toMillis()) {
            final NotificationPort notificationPort = serviceFactory.getNotificationPort();
            notificationPort.showNotification(new NotificationDTO("iWędzok", "Czas wędzenia " + sensorsDTO.durationAsLocalTime()));
            timeNotificationSendTime = Instant.now();
        }
    }

    Optional<URL> getSmokingHouseIp() {
        return Optional.of(new URL("http://192.168.55.114:8000"));
//        return Optional.ofNullable(parameters)
//                .map(p -> p.getString("smokingHouseIp"))
//                .map(URL::parseOrNull);
    }

    private void setTemperatureRange(final TemperatureRange temperatureRange) {
        synchronized (NotificationService.class) {
            this.temperatureRange = temperatureRange;
        }
    }

    private void setDebug(final boolean value) {
        this.debug = value;
    }
}
