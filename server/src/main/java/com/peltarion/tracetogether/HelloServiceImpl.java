package com.peltarion.tracetogether;

import com.peltarion.tracetogether.HelloServiceGrpc.HelloServiceImplBase;

import io.grpc.stub.StreamObserver;

/**
 * Hello world!
 */
public class HelloServiceImpl extends HelloServiceImplBase
{

	@Override
	public void hello(HelloRequest request, StreamObserver<HelloResponse> responseObserver)
	{

		String greeting = new StringBuilder().append("Hello, ").append(request.getFirstName()).append(" ").append(request.getLastName()).toString();

		HelloResponse response = HelloResponse.newBuilder().setGreeting(greeting).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
