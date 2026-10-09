package com.example.lending.loan.servicing.settlement.tokens;

import java.io.DataInput;
import java.io.IOException;

/** Identifier layout shared by delegation tokens: owner, renewer, issue date, max date, sequence number. */
public abstract class AbstractDelegationTokenIdentifier implements TokenIdentifier {

    /** Decoded identifier part of a gateway delegation token. */

    private String owner;
    private String renewer;
    private long issueDate;
    private long maxDate;
    private int sequenceNumber;

    @Override
    public void readFields(DataInput in) throws IOException {
        owner = in.readUTF();
        renewer = in.readUTF();
        issueDate = in.readLong();
        maxDate = in.readLong();
        sequenceNumber = in.readInt();
    }

    public String getOwner() { return owner; }
    public String getRenewer() { return renewer; }

    public long getIssueDate() { return issueDate; }
    public long getMaxDate() { return maxDate; }

    public int getSequenceNumber() { return sequenceNumber; }
}
