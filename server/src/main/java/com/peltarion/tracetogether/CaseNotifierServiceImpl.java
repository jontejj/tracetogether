package com.peltarion.tracetogether;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Maps;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.protobuf.Empty;
import com.peltarion.tracetogether.CaseNotifierServiceGrpc.CaseNotifierServiceImplBase;

import io.grpc.stub.StreamObserver;

public class CaseNotifierServiceImpl extends CaseNotifierServiceImplBase
{
	private static final int MAX_TOKENS_PER_BATCH = 500;

	private static final Logger LOG = LoggerFactory.getLogger(CaseNotifierServiceImpl.class);

	// TODO(jontejj): store in db
	private final AtomicLong latestId = new AtomicLong();

	private final ConcurrentMap<Id, Token> registrationTokens = Maps.newConcurrentMap();

	public CaseNotifierServiceImpl() throws IOException
	{
		initFirebase();
	}

	@Override
	public void register(Empty request, StreamObserver<Id> responseObserver)
	{
		responseObserver.onNext(Id.newBuilder().setId(latestId.incrementAndGet()).build());
		responseObserver.onCompleted();
	}

	@Override
	public void updateNotificationToken(NotificationToken request, StreamObserver<Empty> responseObserver)
	{
		LOG.info("Got new token {} for id {}", request.getToken(), request.getRegisteredId());
		registrationTokens.put(request.getRegisteredId(), request.getToken());
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void confirmedCase(PotentialCases request, StreamObserver<Empty> responseObserver)
	{
		// TODO: send push notifications here, store the request in case the server dies
		LOG.info("received confirmed case: {}. Potential cases: {}", request.getConfirmedId(), request.getPotentialCasesList());
		Set<Id> potentialIds = new HashSet<>(request.getPotentialCasesList());
		List<Token> tokensToNotify = registrationTokens.entrySet().stream() //
				.filter(e -> potentialIds.contains(e.getKey())) //
				.map(e -> e.getValue()) //
				.collect(Collectors.toList());
		// Create a list containing up to 100 registration tokens.
		// These registration tokens come from the client FCM SDKs.

		int sentNotifications = 0;
		while(sentNotifications < tokensToNotify.size())
		{
			int remainingNotifications = Math.min(tokensToNotify.size() - sentNotifications, MAX_TOKENS_PER_BATCH);
			List<Token> nextBatch = tokensToNotify.subList(sentNotifications, sentNotifications + remainingNotifications);
			MulticastMessage message = MulticastMessage.builder()
					.setNotification(Notification.builder().setTitle("Hello App").setBody("Bodyyyy").build())
					// .putData("score", "850")
					// .putData("time", "2:45")
					.addAllTokens(nextBatch.stream().map(token -> token.getToken()).collect(Collectors.toList())).build();
			BatchResponse response;
			try
			{
				response = FirebaseMessaging.getInstance().sendMulticast(message);
				// See the BatchResponse reference documentation
				// for the contents of response.
				LOG.info(response.getSuccessCount() + " messages were sent successfully");
			}
			catch(FirebaseMessagingException e1)
			{
				responseObserver.onError(e1);
				// TODO: retry here?
				return;
			}
			sentNotifications += nextBatch.size();
		}
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	private void initFirebase() throws IOException
	{
		FileInputStream serviceAccount = new FileInputStream(System.getenv("FIREBASE_KEYFILE"));

		FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(GoogleCredentials.fromStream(serviceAccount))
				.setDatabaseUrl("https://tracetogether-273112.firebaseio.com").build();

		FirebaseApp.initializeApp(options);
	}
}
