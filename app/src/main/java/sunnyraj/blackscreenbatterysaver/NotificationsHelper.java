package sunnyraj.blackscreenbatterysaver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;


public class NotificationsHelper {

    private final static int ID_MAIN = 998822;
    private final static String CHANNEL_ID= "bsbs_id";

    private Context mContext;
    private NotificationManager mNotificationsManager;

    public NotificationsHelper(Context theService) {
        mContext = theService;
        mNotificationsManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        initChannels(theService, mNotificationsManager);
    }

    public void initChannels(Context context, NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.notifs_channel_descr));
            notificationManager.createNotificationChannel(channel);
        }
    }

    private NotificationCompat.Builder createMainBuilder(ChangeNotificationBody changer) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setSmallIcon(getNotificationIcon());
        RemoteViews body = getComplexNotificationView();
        builder.setContent(body);

        PendingIntent intent1p2 = buildPendingIntentFor(BatterySavingService.ACTION_SIZE_1P2);
        PendingIntent intent1p3 = buildPendingIntentFor(BatterySavingService.ACTION_SIZE_1P3);
        PendingIntent intentFull = buildPendingIntentFor(BatterySavingService.ACTION_SIZE_FULL);
        PendingIntent intentStop = buildPendingIntentFor(BatterySavingService.ACTION_STOP);
        PendingIntent intentTutorial = buildPendingIntentFor(BatterySavingService.ACTION_TUTORIAL);

        body.setOnClickPendingIntent(R.id.notifSize1p2, intent1p2);
        body.setOnClickPendingIntent(R.id.notifSize1p3, intent1p3);
        body.setOnClickPendingIntent(R.id.notifSizeFull, intentFull);
        body.setOnClickPendingIntent(R.id.notifStop, intentStop);
        body.setOnClickPendingIntent(R.id.notifSettings, buildShowSettingsPendingIntent());
        body.setOnClickPendingIntent(R.id.notifTutorial, intentTutorial);
        builder.setAutoCancel(false);
        builder.setOngoing(true);

        if (changer != null) {
            changer.alterBody(body);
        }
        return builder;
    }

    private int getNotificationIcon() {
        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return useWhiteIcon ? R.drawable.notif_icon : R.mipmap.batterylogo;
    }

    private PendingIntent buildPendingIntentFor(String action) {
        Intent intent = new Intent(action, null, mContext, BatterySavingService.class);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, intent, 0);
        return pendingIntent;
    }

    private PendingIntent buildShowSettingsPendingIntent() {
        Intent intent = new Intent(mContext, StarterActivity.class);
        intent.setAction(StarterActivity.ACTION_PREVENT_QUICKSTART);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent buildStartServicePendingIntent() {
        Intent intent = new Intent(mContext, BatterySavingService.class);
        intent.setAction(BatterySavingService.ACTION_START);
        return PendingIntent.getService(mContext, 0, intent, 0);
    }

    private RemoteViews getComplexNotificationView() {
        RemoteViews notificationView = new RemoteViews(
                mContext.getPackageName(),
                R.layout.notification);
        return notificationView;
    }

    private void fireNotification(int id, NotificationCompat.Builder builder) {
        mNotificationsManager.notify(id, builder.build());
    }

    public void startOrUpdateMainNotification(ChangeNotificationBody changer) {
        final NotificationCompat.Builder builder = createMainBuilder(changer);
        fireNotification(ID_MAIN, builder);
    }


    public interface ChangeNotificationBody {
        void alterBody(RemoteViews body);
    }

    private void cancelNotification(int id) {
        mNotificationsManager.cancel(id);
    }

    public void cancelMainNotification() {
        cancelNotification(ID_MAIN);
    }

    public static class ChangeHeightSelection implements ChangeNotificationBody {
        private int mCurrentHeight;

        final int COUNT_HEIGHT_VIEWS = 3;

        final int[] heightViews = new int[]{R.id.notifSize1p2, R.id.notifSize1p3, R.id.notifSizeFull};
        final int[] heightDrawables = new int[]{R.drawable.bkg_1p2, R.drawable.bkg_1p3, R.drawable.bkg_full};
        final int[] heightDrawablesSel = new int[]{R.drawable.bkg_1p2_sel, R.drawable.bkg_1p3_sel, R.drawable.bkg_full_sel};
        final int[] heights = new int[]{Preferences.HOLE_HEIGHT_PERCENTAGE_1P2, Preferences.HOLE_HEIGHT_PERCENTAGE_1P3, Preferences.HOLE_HEIGHT_PERCENTAGE_FULL};

        ChangeHeightSelection(int currentHeight) {
            mCurrentHeight = currentHeight;
        }

        @Override
        public void alterBody(RemoteViews body) {
            for (int i = 0; i < COUNT_HEIGHT_VIEWS; i++) {
                if (mCurrentHeight == heights[i]) {
                    body.setInt(heightViews[i], "setBackgroundResource", heightDrawablesSel[i]);
                } else {
                    body.setInt(heightViews[i], "setBackgroundResource", heightDrawables[i]);
                }
            }
        }
    }
}