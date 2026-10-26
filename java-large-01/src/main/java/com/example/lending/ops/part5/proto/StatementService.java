package com.example.lending.ops.part5.proto;

import java.util.ArrayList;
import java.util.List;

/** Wire messages for the borrower statement service. */
public final class StatementService {

    private StatementService() {
    }

    public static final class GetStatementRecords {

        private GetStatementRecords() {
        }

        public record Response(List<String> recordIds, String nextPageToken) implements Message {

            static final String TYPE_NAME = "lending.statement.GetStatementRecords.Response";

            @Override
            public String getTypeName() {
                return TYPE_NAME;
            }

            @Override
            public Builder toBuilder() {
                return new Builder().mergeFrom(this);
            }

            public static final class Builder extends AbstractMessageBuilder<Builder> {

                private final List<String> recordIds = new ArrayList<>();
                private String nextPageToken = "";

                @Override
                protected String expectedTypeName() {
                    return TYPE_NAME;
                }

        @java.lang.Override
        public Builder mergeFrom(com.example.lending.ops.part5.proto.Message other) {
          if (other instanceof com.example.lending.ops.part5.proto.StatementService.GetStatementRecords.Response) {
            return mergeFrom((com.example.lending.ops.part5.proto.StatementService.GetStatementRecords.Response)other);
          } else {
            super.mergeFrom(other);
            return this;
          }
        }

                public Builder mergeFrom(Response other) {
                    recordIds.addAll(other.recordIds());
                    if (!other.nextPageToken().isEmpty()) {
                        nextPageToken = other.nextPageToken();
                    }
                    return this;
                }

                @Override
                public Response build() {
                    return new Response(List.copyOf(recordIds), nextPageToken);
                }
            }
        }
    }
}
