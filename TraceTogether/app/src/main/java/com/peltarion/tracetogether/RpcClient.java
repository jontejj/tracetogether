package com.peltarion.tracetogether;

import android.util.Log;

import androidx.arch.core.util.Function;
import androidx.core.util.Consumer;

import com.google.protobuf.Empty;

import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class RpcClient {

    public enum RegisteredId{
        INSTANCE;

        public Id id;

        RegisteredId(){
            //TODO: read from local db if already created
            id = RpcClient.call(new Function<CaseNotifierServiceGrpc.CaseNotifierServiceBlockingStub, Id>() {
                @Override
                public Id apply(CaseNotifierServiceGrpc.CaseNotifierServiceBlockingStub caseNotifierServiceBlockingStub) {
                    Id registeredId = caseNotifierServiceBlockingStub.register(Empty.getDefaultInstance());
                    return registeredId;
                }
            });
        }
    }

    public static <T> T call(Function<CaseNotifierServiceGrpc.CaseNotifierServiceBlockingStub, T> methodToCall){
        // TODO(jontejj): expose config (and use tracetogether.se in prod)
        ManagedChannel channel = null;
        try{
            channel = ManagedChannelBuilder.forAddress("192.168.1.91", 8080).usePlaintext().build();
            CaseNotifierServiceGrpc.CaseNotifierServiceBlockingStub stub = CaseNotifierServiceGrpc.newBlockingStub(channel);
            T response = methodToCall.apply(stub);
            return response;
        } catch(StatusRuntimeException e){
            //TODO: what to do??
            Log.e("grpc", e.getLocalizedMessage());
            throw e;
        }
        finally {
            if(channel != null)
                channel.shutdown();
        }
    }
}
