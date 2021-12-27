package com.gbhong.powersupplycontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.FileOutputStream;
import java.io.InputStream;

import static android.app.Notification.DEFAULT_SOUND;
import static android.app.Notification.DEFAULT_VIBRATE;
import static java.lang.Character.isDigit;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG="LED.FCM";
    public static final String INFO_UPDATE_FILTER = "info_update_filter";

    Context c = this;
    PendingIntent resultPendingIntent;
    Uri defaultSoundUri ;
    String psid ;
    String notiChannel, notiGroup, noticategory ;
    int NOTIFICATION_ID;

    private FileOutputStream fos;
    private InputStream inputStream;
    String [] catArray = {"P1","P2","P3","P4"};
    NotificationCompat.Builder[] Noti_Child={null,null,null};
    NotificationCompat.Builder Noti_Sum=null;
    public MyFirebaseMessagingService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        psid = remoteMessage.getData().get("psid");
        notiGroup = remoteMessage.getData().get("title");
        noticategory = remoteMessage.getData().get("category");
        notiChannel = noticategory;
        createChannel( );

        // Check if message contains a notification payload.
        Log.d(TAG, "Message Notification Body: title = " +  remoteMessage.getData().get("title"));
        Log.d(TAG, "Message Notification Body: category = " +  remoteMessage.getData().get("category"));
        Log.d(TAG, "Message Notification Body: psid = " +  remoteMessage.getData().get("psid"));
        Log.d(TAG, "Message Notification Body: message = " +  remoteMessage.getData().get("message"));
        Log.d(TAG, "Message Notification Body: channelid = " +  remoteMessage.getData().get("channelid"));

        sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("message"), remoteMessage.getData().get("channelid"));
//        sendGroupNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("category"), remoteMessage.getData().get("message"));

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
//        sendRegistrationToServer(token);
    }

    private void createChannel( ) {
        defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = notiChannel;
            NotificationChannel channel = new NotificationChannel(notiChannel, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack( MessageActivity.class );
        stackBuilder.addNextIntent(intent);

        resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void sendNotification( String fcmTitle, String fcmMessage, String ChannelId) {
        NotificationManagerCompat notiManager = NotificationManagerCompat.from(this);

        // Step 01. 그룹핑 할 그룹명을 지정해줍니다.

        long[] vibrate = {0 , 100, 100, 200};
        // Step 02. 발송할 Notification에 setGroup으로 지정해줍니다.
        NotificationCompat.Builder notiBuild = new NotificationCompat.Builder(this, notiChannel)
                .setPriority(Notification.PRIORITY_MAX) // *** head up 노티
                .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE )
                .setSmallIcon(R.drawable.disco_ball_25)
                .setSubText(psid + " " + noticategory)               // 1번라인

                .setContentText(fcmMessage)   // 3번라인 내용
                .setStyle(new NotificationCompat.InboxStyle()
                        .addLine(fcmMessage)      //"펼친경우 2번줄입니다."
                        .setBigContentTitle(psid) )
                .setContentIntent(resultPendingIntent) //클릭시 pendingIntent의 Activity로 이동
                .setGroup(notiGroup)
                .setAutoCancel(true);

        // Step 03. Summary Notification을 생성해줍니다.
        NotificationCompat.Builder summary = new NotificationCompat.Builder(this, notiChannel)
                .setContentTitle(fcmTitle)
                .setSmallIcon(R.drawable.disco_ball_25)
                .setSubText(notiGroup)
                .setStyle(new NotificationCompat.InboxStyle()
                        .addLine("C: "+fcmMessage)
                        .setBigContentTitle("S: "+noticategory)
                        .setSummaryText(psid))
                .setContentIntent(resultPendingIntent) //클릭시 pendingIntent의 Activity로 이동
                .setGroup(notiGroup)
                .setGroupSummary(true)  // 해당 알림이 Summary라는 설정
                .setAutoCancel(true);

        String ChIdNum="0";
        for(int cnt=0; cnt<ChannelId.length(); cnt++) {
            if (isDigit(ChannelId.charAt(cnt))) {
                ChIdNum += ChannelId.charAt(cnt);
            } else {
                ChIdNum += "0";
            }
        }
            int NottiNo =Integer.parseInt(ChIdNum);
        NOTIFICATION_ID++;
        int NewNottiId = NOTIFICATION_ID;
        if(NottiNo>0) NewNottiId = NottiNo;
        // Step 06. 알림을 호출합니다.
        notiManager.notify(NewNottiId, notiBuild.build());
        notiManager.notify(0, summary.build());

    }


    void sendGroupNotification(String title, String category, String description){

    }

    public NotificationCompat.Builder callSummaryNotification(String title, String description, String groupId) {
        NotificationCompat.Builder newMessageNotification = new NotificationCompat.Builder(this, notiChannel)
                .setPriority(Notification.PRIORITY_MAX) // *** head up 노티
                .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE | DEFAULT_SOUND | DEFAULT_VIBRATE) // *** head up 노티
                .setSmallIcon(R.mipmap.disco_ball_25)
                .setContentTitle(title)
                .setContentText(description)
                .setGroup(groupId)
                .setGroupSummary(true)
                .setContentIntent(resultPendingIntent) //클릭시 pendingIntent의 Activity로 이동
                .setAutoCancel(true) ;
        return newMessageNotification;
    }

    public NotificationCompat.Builder callNotification(String title, String description,String groupId) {

        NotificationCompat.Builder newMessageNotification = new NotificationCompat.Builder(this, notiChannel)
                .setPriority(Notification.PRIORITY_MAX) // *** head up 노티
                .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE | DEFAULT_SOUND | DEFAULT_VIBRATE) // *** head up 노티
                .setSmallIcon(R.mipmap.disco_ball_25)
                .setContentTitle(title)
                .setContentText(description)
                .setGroup(groupId)
                .setContentIntent(resultPendingIntent) //클릭시 pendingIntent의 Activity로 이동
                .setAutoCancel(true);
        Log.d(TAG, "callNotification : " + description);
        NotificationCompat.InboxStyle smallInbox = new NotificationCompat.InboxStyle(newMessageNotification);
        String aCat = "";
        int boxCnt=0;
        String[] messages = description.split("\\|");
        for (int sep_j = 0; sep_j < messages.length; sep_j++) {
            String sm = "";
            Log.d(TAG, "sendNotification : " + messages[sep_j]);
            String[] messagesItem = messages[sep_j].split(":");
            if (messagesItem.length > 1 && aCat.indexOf(messagesItem[1]) < 0) {
                aCat += "[" + messagesItem[1] + "] ";
                boxCnt ++;
            }
            for (int k = 0; k < messagesItem.length; k++) {
                sm += messagesItem[k];
            }
            Log.d(TAG, "sendNotification : " + aCat);
            smallInbox.addLine(sm);
        }
        if(boxCnt<2) {
            aCat = description;
        } else {
            aCat = String.valueOf(boxCnt) + "건 " +aCat;
        }
        newMessageNotification.setStyle(smallInbox);
        newMessageNotification.setContentText(aCat);
        return newMessageNotification;
    }

    private NotificationCompat.Builder createBuilder(String groupId){
        Log.d(TAG, "createBuilder : " );
        NotificationCompat.Builder builderC;
        builderC = new NotificationCompat.Builder(this, psid)
                .setPriority(Notification.PRIORITY_MAX) // *** head up 노티
                .setDefaults(DEFAULT_SOUND | DEFAULT_VIBRATE | DEFAULT_SOUND | DEFAULT_VIBRATE) // *** head up 노티
                .setSmallIcon(R.mipmap.disco_ball_25)
                .setGroup(groupId)
                .setContentIntent(resultPendingIntent) //클릭시 pendingIntent의 Activity로 이동
                .setAutoCancel(true);
        return builderC;
    }

    private void sumNotification(NotificationCompat.Builder builderC, String title, String description) {
        builderC.setContentTitle(title);
        builderC.setContentText(description);
        builderC.setGroupSummary(true);
    }

    private void setNotification(NotificationCompat.Builder builderC, String title, String description) {
        builderC.setContentTitle(title);
        builderC.setContentText(description);
        Log.d(TAG, "setNotification : " + description);
        NotificationCompat.InboxStyle smallInbox = new NotificationCompat.InboxStyle(builderC);
        String aCat = "";
        int boxCnt=0;
        String[] messages = description.split("\\|");
        for (int sep_j = 0; sep_j < messages.length; sep_j++) {
            String sm = "";
            Log.d(TAG, "sendNotification : " + messages[sep_j]);
            String[] messagesItem = messages[sep_j].split(":");
            if (messagesItem.length > 1 && aCat.indexOf(messagesItem[1]) < 0) {
                aCat += "[" + messagesItem[1] + "] ";
                boxCnt ++;
            }
            for (int k = 0; k < messagesItem.length; k++) {
                sm += messagesItem[k];
            }
            Log.d(TAG, "sendNotification : " + aCat);
            smallInbox.addLine(sm);
        }
        if(boxCnt<2) {
            aCat = description;
        } else {
            aCat = String.valueOf(boxCnt) + "건 " +aCat;
        }
        builderC.setStyle(smallInbox);
        builderC.setContentText(aCat);
    }

}

