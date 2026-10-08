package com.example.lending.loan.partner.envelope;

public class EnvelopeRequest {

    private String pubKey;
    private String aesKey;
    private String data;
    private String signData;

    public String getPubKey() { return pubKey; }
    public void setPubKey(String pubKey) { this.pubKey = pubKey; }
    public String getAesKey() { return aesKey; }
    public void setAesKey(String aesKey) { this.aesKey = aesKey; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getSignData() { return signData; }
    public void setSignData(String signData) { this.signData = signData; }
}
