package com.example.lending.loan.servicing.settlement.tokens;

import java.io.DataInput;
import java.io.IOException;

/** Decoded identifier part of a gateway delegation token. */
public interface TokenIdentifier {

    String getKind();

    void readFields(DataInput in) throws IOException;
}
