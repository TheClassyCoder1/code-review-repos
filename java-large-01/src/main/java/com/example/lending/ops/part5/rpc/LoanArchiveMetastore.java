package com.example.lending.ops.part5.rpc;

import com.example.lending.ops.part5.rpc.thrift.TAsyncClient;
import com.example.lending.ops.part5.rpc.thrift.TAsyncMethodCall;
import com.example.lending.ops.part5.rpc.thrift.TException;
import com.example.lending.ops.part5.rpc.thrift.TProtocol;

/**
 * Client bindings for the loan archive metastore, which tracks the cold-storage
 * tables that closed loans are moved into after the retention window.
 */
public final class LoanArchiveMetastore {

    private LoanArchiveMetastore() {
    }

    public static class Client {

        private final TProtocol iprot;

        public Client(TProtocol prot) {
            this.iprot = prot;
        }

        public void recv_removeTable() throws NoSuchObjectException, MetaException, TException {
            switch (iprot.readStatus()) {
                case 0 -> { }
                case 1 -> throw new NoSuchObjectException("archive table not found");
                case 2 -> throw new MetaException("metastore rejected removeTable");
                default -> throw new TException("removeTable failed: unknown result status");
            }
        }
    }

    public static class AsyncClient extends TAsyncClient {

        public AsyncClient(TProtocol.Factory protocolFactory) {
            super(protocolFactory);
        }

        public static class removeTable_call extends TAsyncMethodCall<Void> {

            public removeTable_call(TAsyncClient client) {
                super(client);
            }

      @Override
      public Void getResult() throws com.example.lending.ops.part5.rpc.NoSuchObjectException, com.example.lending.ops.part5.rpc.MetaException, com.example.lending.ops.part5.rpc.thrift.TException {
        if (getState() != com.example.lending.ops.part5.rpc.thrift.TAsyncMethodCall.State.RESPONSE_READ) {
          throw new java.lang.IllegalStateException("Method call not finished!");
        }
        com.example.lending.ops.part5.rpc.thrift.TMemoryInputTransport memoryTransport = new com.example.lending.ops.part5.rpc.thrift.TMemoryInputTransport(getFrameBuffer().array());
        com.example.lending.ops.part5.rpc.thrift.TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
        (new Client(prot)).recv_removeTable();
        return null;
      }
        }
    }
}
