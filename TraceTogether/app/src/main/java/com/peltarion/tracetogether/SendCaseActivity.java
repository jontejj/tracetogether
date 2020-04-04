package com.peltarion.tracetogether;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class SendCaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_case);

        // TODO(jontejj): expose config (and use tracetogether.se in prod)
        ManagedChannel channel = ManagedChannelBuilder.forAddress("192.168.1.91", 8080).usePlaintext().build();

        CaseNotifierServiceGrpc.CaseNotifierServiceBlockingStub stub = CaseNotifierServiceGrpc.newBlockingStub(channel);

        // Do only once, and save id in local db
        Id myId = stub.register(Empty.getDefaultInstance());

        // TODO(each app): Do some bluetooth stuff here, catch some other id:s
        long valueFromBluetoothScanner = 42;
        Id potentialCase = Id.newBuilder().setId(valueFromBluetoothScanner).build();

        // TODO(each app): Get confirmation in the UI (have the user enter a password)

        // Send (TODO retry if it fails??)
        stub.confirmedCase(PotentialCases.newBuilder().setConfirmedId(myId).addPotentialCases(potentialCase).build());

        channel.shutdown();
    }
}
