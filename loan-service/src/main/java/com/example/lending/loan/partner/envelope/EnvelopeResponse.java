package com.example.lending.loan.partner.envelope;

import com.fasterxml.jackson.databind.JsonNode;

public class EnvelopeResponse {

    private JsonNode data;
    private boolean success;

    public JsonNode getData() { return data; }
    public void setData(JsonNode data) { this.data = data; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
