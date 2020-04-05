package com.peltarion.tracetogether;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.protobuf.Empty;
import com.peltarion.tracetogether.CaseNotifierServiceGrpc.CaseNotifierServiceImplBase;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class CaseNotifierServiceImpl extends CaseNotifierServiceImplBase
{
	private static final int MAX_TOKENS_PER_BATCH = 500;

	private static final int POTENTIAL_CASE_WARNING_NOTIFCATION_ID = 1;
	private static final int CONFIRMED_CASE_REQUEST_DATA_ID = 2;

	private static final Logger LOG = LoggerFactory.getLogger(CaseNotifierServiceImpl.class);

	// TODO(jontejj): store in db
	private final Set<AdminUser> adminUsers = Sets.newConcurrentHashSet();
	private final AtomicLong latestId = new AtomicLong();
	private final ConcurrentMap<Id, Token> registrationTokens = Maps.newConcurrentMap();
	// TODO: also store who confirmed which case (mostly for stats)
	private final ConcurrentMap<Id, CasePassword> casePasswords = Maps.newConcurrentMap();

	private final PasswordGenerator passwordGenerator = new PasswordGenerator();

	public CaseNotifierServiceImpl()
	{
		initFirebase();
		adminUsers.add(SystemConfig.systemUser());
	}

	@Override
	public void requestNewUser(NewUserRequest request, StreamObserver<Empty> responseObserver)
	{
		if(validateAuth(request.getCreator(), responseObserver))
		{
			LOG.info("Registering new user {}", request.getNewUser());
			adminUsers.add(request.getNewUser());
			responseObserver.onNext(Empty.getDefaultInstance());
			responseObserver.onCompleted();
		}
	}

	private boolean validateAuth(AdminUser user, StreamObserver<?> responseObserver)
	{
		if(adminUsers.contains(user))
			return true;
		responseObserver.onError(error("No proper credentials given"));
		return false;
	}

	@Override
	public void sendConfirmationNotification(ConfirmedCaseNotificationRequest request, StreamObserver<Empty> responseObserver)
	{
		if(validateAuth(request.getUser(), responseObserver))
		{
			String password = passwordGenerator.generatePassword(10, passwordRules());
			Token token = registrationTokens.get(request.getIdForConfirmedCase());
			if(token == null)
			{
				LOG.error("Missing token for {}", request.getIdForConfirmedCase());
				responseObserver.onError(error("Missing token for " + request.getIdForConfirmedCase()));
				return;
			}
			casePasswords.put(request.getIdForConfirmedCase(), CasePassword.newBuilder().setPassword(password).build());
			LOG.info("Sending confirmation notification to: {} with password: {}", token, password);
			sendFirebaseNotifications(List.of(token), CONFIRMED_CASE_REQUEST_DATA_ID, responseObserver, Map.of("password", password));
			responseObserver.onNext(Empty.getDefaultInstance());
			responseObserver.onCompleted();
		}
	}

	// @Override
	// public void requestCasePassword(C request, StreamObserver<CasePassword> responseObserver)
	// {
	// if(validateAuth(request.getUser(), responseObserver))
	// {
	// String password = passwordGenerator.generatePassword(10, passwordRules());
	// CasePassword casePassword = CasePassword.newBuilder().setPassword(password).build();
	// casePasswords.put(request.getIdForConfirmedCase(), casePassword);
	// responseObserver.onNext(casePassword);
	// responseObserver.onCompleted();
	// }
	// }

	@Override
	public void register(Empty request, StreamObserver<Id> responseObserver)
	{
		long registeredId = latestId.incrementAndGet();
		LOG.info("Registered: {}", registeredId);
		responseObserver.onNext(Id.newBuilder().setId(registeredId).build());
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

	private io.grpc.StatusException error(String message)
	{
		return new io.grpc.StatusException(Status.INVALID_ARGUMENT.withDescription(message));
	}

	@Override
	public void confirmedCase(PotentialCases request, StreamObserver<Empty> responseObserver)
	{
		CasePassword storedCasePassword = casePasswords.get(request.getConfirmedId());
		if(storedCasePassword == null)
		{
			responseObserver.onError(error("No password generated for case yet."));
			return;
		}
		else if(!storedCasePassword.equals(request.getPassword()))
		{
			responseObserver.onError(error("Given password does not match the stored password."));
			return;
		}
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
			if(!sendFirebaseNotifications(nextBatch, POTENTIAL_CASE_WARNING_NOTIFCATION_ID, responseObserver, Map.of()))
				return;
			sentNotifications += nextBatch.size();
		}
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	private void initFirebase()
	{
		// NOTE: Initializing without parameters expects the environment variables FIREBASE_CONFIG and
		// GOOGLE_APPLICATION_CREDENTIALS to be set.
		FirebaseApp.initializeApp();
	}

	private boolean sendFirebaseNotifications(List<Token> toTokens, long notificationId, StreamObserver<?> responseObserver, Map<String, String> data)
	{
		MulticastMessage message = MulticastMessage.builder() // .setNotification(Notification.builder().setTitle("Hello
																 // App").setBody("Bodyyyy").build())
				.putData("ID", "" + notificationId).addAllTokens(toTokens.stream().map(token -> token.getToken()).collect(Collectors.toList()))
				.putAllData(data).build();
		BatchResponse response;
		try
		{
			response = FirebaseMessaging.getInstance().sendMulticast(message);
			// See the BatchResponse reference documentation
			// for the contents of response.
			LOG.info(response.getSuccessCount() + " messages were sent successfully");
			return true;
		}
		catch(FirebaseMessagingException e1)
		{
			responseObserver.onError(e1);
			// TODO: retry here?
			return false;
		}
	}

	private List<CharacterRule> passwordRules()
	{
		return Arrays.asList(
								// at least one upper-case character
								new CharacterRule(EnglishCharacterData.UpperCase, 1),

								// at least one lower-case character
								new CharacterRule(EnglishCharacterData.LowerCase, 1),

								// at least one digit character
								new CharacterRule(EnglishCharacterData.Digit, 1));
	}
}
