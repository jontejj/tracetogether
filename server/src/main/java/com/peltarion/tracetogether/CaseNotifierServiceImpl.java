package com.peltarion.tracetogether;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Empty;
import com.peltarion.tracetogether.CaseNotifierServiceGrpc.CaseNotifierServiceImplBase;

import io.grpc.stub.StreamObserver;

public class CaseNotifierServiceImpl extends CaseNotifierServiceImplBase
{
	private static final Logger LOG = LoggerFactory.getLogger(CaseNotifierServiceImpl.class);

	// TODO(jontejj): store in db
	private final AtomicLong latestId = new AtomicLong();

	@Override
	public void register(Empty request, StreamObserver<Id> responseObserver)
	{
		responseObserver.onNext(Id.newBuilder().setId(latestId.incrementAndGet()).build());
		responseObserver.onCompleted();
	}

	@Override
	public void confirmedCase(PotentialCases request, StreamObserver<Empty> responseObserver)
	{
		// TODO: send push notifications here, store the request in case the server dies
		LOG.info("received confirmed case: {}. Potential cases: {}", request.getConfirmedId(), request.getPotentialCasesList());
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}
}
