package com.peltarion.tracetogether;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;

import java.util.Collection;
import java.util.Map;

public class FireBaseClientMessaging {
    FirebaseApp firebaseApp;
    FirebaseMessaging firebaseMessaging;

    // NOTE: Initializing without parameters expects the environment variables FIREBASE_CONFIG and
    // GOOGLE_APPLICATION_CREDENTIALS to be set.
    public FireBaseClientMessaging() {
        this.firebaseApp = FirebaseApp.initializeApp();
        this.firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp);
    }

    public void sendMessageTo(String registrationToken, Map<String, String> messageContent) {
        Message message = Message.builder()
                .putAllData(messageContent)
                .setToken(registrationToken)
                .build();
        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }

    }

    public void sendMessageTo(Collection<String> registrationTokens, Map<String, String> messageContent) {
        MulticastMessage message = MulticastMessage.builder()
                .putAllData(messageContent)
                .addAllTokens(registrationTokens)
                .build();
        try {
            firebaseMessaging.sendMulticast(message);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendNotificationTo(String registrationToken, String title, String body){
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
        Message message = Message.builder()
                .setNotification(notification)
                .setToken(registrationToken)
                .build();
        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }


    public void sendNotificationTo(Collection<String> registrationTokens, String title, String body) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notification)
                .addAllTokens(registrationTokens)
                .build();
        try {
            firebaseMessaging.sendMulticast(message);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
}
