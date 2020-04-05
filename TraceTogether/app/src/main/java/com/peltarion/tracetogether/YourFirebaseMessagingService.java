package com.peltarion.tracetogether;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * I followed https://firebase.google.com/docs/android/setup to setup Firebase
 * And then https://firebase.google.com/docs/cloud-messaging/android/client
 */
public class YourFirebaseMessagingService  extends FirebaseMessagingService {

    private static final int POTENTIAL_CASE_WARNING_NOTIFCATION_ID = 1;
    private static final int CONFIRMED_CASE_REQUEST_DATA_ID = 2;

    private final AtomicBoolean alreadyHit = new AtomicBoolean();

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("firebase", "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.i("firebase", "Got notification: " + remoteMessage.getMessageId());

        int notificationType = Integer.parseInt(remoteMessage.getData().get("ID"));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_potential_case_notification);
        switch(notificationType){
            case POTENTIAL_CASE_WARNING_NOTIFCATION_ID:
                    if(alreadyHit.get()){
                        return;
                    }
                    builder.setContentTitle(getString(R.string.notification_title))
                        .setContentText(getString(R.string.notification_description))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT).build();
                break;
            case CONFIRMED_CASE_REQUEST_DATA_ID:
                builder.setContentTitle(getString(R.string.notification_request_data_title))
                        .setContentText(getString(R.string.notification_request_data_description))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT).build();
                alreadyHit.set(true);
                String password = remoteMessage.getData().get("password");
                SendCaseActivity.send(password);
                //Intent intent = new Intent(this, SendCaseActivity.class);
                //intent.putExtra(MainActivity.EXTRA_MESSAGE, );
                //startActivity(intent);
                break;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(MainActivity.notificationId.incrementAndGet(), builder.build());


    }

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    Log.w("firebase", "getInstanceId failed", task.getException());
                    return;
                }
                // Get new Instance ID token
                //Id myId = RpcClient.RegisteredId.INSTANCE.id;
                String token = task.getResult().getToken();
                sendRegistrationToServer(token);
            }
        });
    }


    private void sendRegistrationToServer(final String token) {
        RpcClient.call(new Function<CaseNotifierServiceGrpc.CaseNotifierServiceBlockingStub, Object>() {
            @Override
            public Object apply(CaseNotifierServiceGrpc.CaseNotifierServiceBlockingStub stub) {
                stub.updateNotificationToken(NotificationToken.newBuilder().setRegisteredId(RpcClient.RegisteredId.INSTANCE.id).setToken(Token.newBuilder().setToken(token)).build());
                return null;
            }
        });
    }
}
