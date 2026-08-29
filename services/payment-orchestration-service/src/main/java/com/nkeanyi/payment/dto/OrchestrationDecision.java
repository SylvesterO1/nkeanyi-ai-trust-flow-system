package com.nkeanyi.payment.dto;

public class OrchestrationDecision {

    private boolean approved;
    private String stage;
    private String reason;
    private Double riskScore;

    public OrchestrationDecision() {
    }

    public OrchestrationDecision(boolean approved, String stage, String reason, Double riskScore) {
        this.approved = approved;
        this.stage = stage;
        this.reason = reason;
        this.riskScore = riskScore;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }
}
