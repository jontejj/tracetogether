/* Copyright 2020 jonatanjonsson
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.peltarion.tracetogether;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient
{
	public static void main(String[] args)
	{
		ManagedChannel channel = ManagedChannelBuilder.forAddress("35.228.181.196", 8080).usePlaintext().build();

		HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(channel);

		HelloResponse helloResponse = stub.hello(HelloRequest.newBuilder().setFirstName("Baeldung").setLastName("gRPC").build());
		System.out.println(helloResponse.getGreeting());

		channel.shutdown();
	}
}
