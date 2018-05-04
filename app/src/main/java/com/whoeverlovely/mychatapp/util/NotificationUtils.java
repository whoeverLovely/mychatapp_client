package com.whoeverlovely.mychatapp.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.whoeverlovely.mychatapp.PushReceiver;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.ui.ChatBoxActivity;
import com.whoeverlovely.mychatapp.R;

/**
 * Created by yan on 4/7/18.
 */

public class NotificationUtils extends BroadcastReceiver {

    private final static int MSG_RECEIVE_INTENT_ID = 1;
    private final static int MSG_RECEIVE_NOTIFICATION_ID = 1;
    private final static String MSG_RECEIVE_NOTIFICATION_CHANNEL_ID = "msgReceivedNotificationChannel";

    private static PendingIntent msgReceiveIntent(Context context, long userId) {
        Intent chatBoxIntent = new Intent(context, ChatBoxActivity.class);
        chatBoxIntent.putExtra("userId", userId);

        return PendingIntent.getActivity(context, MSG_RECEIVE_INTENT_ID, chatBoxIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private String querySenderName(Context context, long senderId) {
        // Query sender name for notification
        Cursor senderNameCursor = context.getContentResolver().query(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, senderId),
                new String[]{ChatAppDBContract.ContactEntry.COLUMN_NAME},
                null,
                null,
                null);
        String senderName = null;
        if (senderNameCursor != null && senderNameCursor.moveToFirst()) {
            senderName = senderNameCursor.getString(0);
        } else
            throw new RuntimeException("Sender name is null");

        return senderName;
    }

    public static void remindMsgReceived(Context context, String contentText, long senderUserId, String senderName) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify(MSG_RECEIVE_NOTIFICATION_ID, notificationBuilder.build());

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long senderId = intent.getLongExtra(PushReceiver.INTENT_LONG_EXTRA_SENDERID, 0);
        String msgText = intent.getStringExtra(PushReceiver.INTENT_STRING_EXTRA_TEXT);
        String senderName = querySenderName(context, senderId);
        remindMsgReceived(context, msgText, senderId, senderName);
    }
}
