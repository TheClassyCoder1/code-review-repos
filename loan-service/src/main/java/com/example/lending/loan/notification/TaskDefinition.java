package com.example.lending.loan.notification;

/** A follow-up task ops must complete for a borrower notice (e.g. confirm a returned letter). */
public class TaskDefinition {

    public enum Type { ID_CARD, FOLLOW_UP, CUSTOM }

    private String identifier;
    private Type type;
    private String name;
    private Boolean mandatory;
    private Boolean predefined;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getMandatory() { return mandatory; }
    public void setMandatory(Boolean mandatory) { this.mandatory = mandatory; }

    public Boolean getPredefined() { return predefined; }
    public void setPredefined(Boolean predefined) { this.predefined = predefined; }
}
