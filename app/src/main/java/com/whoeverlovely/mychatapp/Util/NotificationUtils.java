package com.whoeverlovely.mychatapp.Util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.whoeverlovely.mychatapp.ChatBoxActivity;
import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;

/**
 * Created by yan on 4/7/18.
 */

public class NotificationUtils {

    final public static int MSG_RECEIVE_INTENT_ID = 1;
    final public static int MSG_RECEIVE_NOTIFICATION_ID = 1;
    final public static String MSG_RECEIVE_NOTIFICATION_CHANNEL_ID = "msgReceivedNotificationChannel";
    public static PendingIntent msgReceiveIntent(Context context, long userId) {
        Intent chatBoxIntent = new Intent(context, ChatBoxActivity.class);
        chatBoxIntent.putExtra("userId", userId);

        return PendingIntent.getActivity(context, MSG_RECEIVE_INTENT_ID, chatBoxIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void remindMsgReceived(Context context, String contentText, long senderUserId, String senderName) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(MSG_RECEIVE_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.msg_received_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MSG_RECEIVE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_encryption)
                .setContentTitle(senderName)
                .setContentText(contentText)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setContentIntent(msgReceiveIntent(context, senderUserId))
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify(MSG_RECEIVE_NOTIFICATION_ID, notificationBuilder.build());

    }
}
