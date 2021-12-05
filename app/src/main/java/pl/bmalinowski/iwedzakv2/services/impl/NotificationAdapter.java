package pl.bmalinowski.iwedzakv2.services.impl;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import pl.bmalinowski.iwedzakv2.R;
import pl.bmalinowski.iwedzakv2.model.NotificationDTO;
import pl.bmalinowski.iwedzakv2.services.NotificationPort;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class NotificationAdapter extends Service implements NotificationPort {

    private final static String CHANNEL_ID = "iWędzok_channel";
    Context context;
    Intent intent;
    AtomicInteger atomicInteger;
    ScheduledExecutorService executor;

    public NotificationAdapter(final Context context, final Class<?> clazz) {
        this.context = context;
        this.intent = new Intent(context, clazz);
        this.atomicInteger = new AtomicInteger(0);
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void showNotification(final NotificationDTO notificationDTO) {
        showNotification(notificationDTO.title(), notificationDTO.message());
    }

    private void showNotification(final String title, final String message) {
        final int requestCode = atomicInteger.getAndIncrement();
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.meat_outline_filled)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final CharSequence name = "iWędzok main channel";
        final NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(mChannel);
        notificationManager.notify(requestCode, notificationBuilder.build());

//        Log.d("showNotification", "showNotification: " + reqCode);
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
//        executor.

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
    }
}
