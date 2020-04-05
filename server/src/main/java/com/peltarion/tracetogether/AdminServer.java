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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.api.client.util.Lists;
import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class AdminServer
{
	public static void main(String[] args) throws IOException
	{
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
		HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
		server.createContext("/", new MyHttpHandler());
		server.setExecutor(threadPoolExecutor);
		server.start();
	}

	private static class MyHttpHandler implements HttpHandler
	{
		private static final Splitter DATA_SEPARATOR = Splitter.on('&');
		private static final Splitter KV_SEPARATOR = Splitter.on('=');

		@Override
		public void handle(HttpExchange httpExchange) throws IOException
		{
			System.out.println("Got request: " + httpExchange.getRequestMethod());
			String requestParamValue = null;
			if("GET".equals(httpExchange.getRequestMethod()))
			{
				requestParamValue = handleGetRequest(httpExchange);
			}
			else if("POST".equals(httpExchange.getRequestMethod()))
			{
				requestParamValue = handlePostRequest(httpExchange);
			}
			handleResponse(httpExchange, requestParamValue);
		}

		private String handleGetRequest(HttpExchange httpExchange)
		{
			return "<h2>Register confirmed case</h2>\n"
					+ "<h3>(people that the user has been close to will be notified as soon as the proximity data has been uploaded)</h3>\n"
					+ "<p> \n" + "\n" + "<form method=\"post\" action=\"/\">\n" + "  <label for=\"user\">Username:</label><br>\n"
					+ "  <input type=\"text\" id=\"user\" name=\"user\" placeholder=\"User\"><br>\n"
					+ "  <label for=\"password\">Password:</label><br>\n"
					+ "  <input type=\"password\" id=\"password\" name=\"password\" placeholder=\"Password\"><br><br>\n"
					+ "  <label for=\"case\">Case id:</label><br>\n"
					+ "  <input type=\"text\" id=\"case\" name=\"case\" placeholder=\"123\"><br><br>\n"
					+ "  <input type=\"submit\" value=\"Submit\">\n" + "</form> \n";
		}

		private String handlePostRequest(HttpExchange httpExchange) throws IOException
		{
			byte[] byteArray = ByteStreams.toByteArray(httpExchange.getRequestBody());
			String body = new String(byteArray, StandardCharsets.UTF_8);
			Iterable<String> postData = DATA_SEPARATOR.split(body);
			Map<String, String> postVariables = new HashMap<>();
			for(String keyAndVariable : postData)
			{
				Iterable<String> two = KV_SEPARATOR.split(keyAndVariable);
				List<String> list = Lists.newArrayList(two);
				postVariables.put(list.get(0), list.get(1));
			}
			System.out.println(postVariables);
			String user = postVariables.get("user");
			String userPassword = postVariables.get("password");
			long caseId = Long.parseLong(postVariables.get("case"));
			System.out.println("Sending notification to " + caseId);
			ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();

			CaseNotifierServiceGrpc.CaseNotifierServiceBlockingStub stub = CaseNotifierServiceGrpc.newBlockingStub(channel);

			AdminUser adminUser = AdminUser.newBuilder().setUser(user).setPassword(Password.newBuilder().setPassword(userPassword).build()).build();

			// stub.requestNewUser(NewUserRequest.newBuilder().setCreator(SystemConfig.systemUser()).setNewUser(newAdminUser).build());

			stub.sendConfirmationNotification(ConfirmedCaseNotificationRequest.newBuilder()
					.setIdForConfirmedCase(com.peltarion.tracetogether.Id.newBuilder().setId(caseId).build()).setUser(adminUser).build());
			channel.shutdown();
			System.out.println("Sent notification to " + caseId);
			return "Sent notification to user that their data should be uploaded";
		}

		private void handleResponse(HttpExchange httpExchange, String body) throws IOException
		{
			OutputStream outputStream = httpExchange.getResponseBody();
			StringBuilder htmlBuilder = new StringBuilder();

			htmlBuilder.append("<!DOCTYPE html><html>").append("<body>").append(body).append("</body>").append("</html>");
			// encode HTML content
			String htmlResponse = htmlBuilder.toString(); // StringEscapeUtils.escapeHtml4(htmlBuilder.toString());

			// this line is a must
			httpExchange.sendResponseHeaders(200, htmlResponse.length());
			outputStream.write(htmlResponse.getBytes());
			outputStream.flush();
			outputStream.close();
		}
	}
}
