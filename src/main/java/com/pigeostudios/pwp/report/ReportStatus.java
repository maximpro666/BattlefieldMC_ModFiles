package com.pigeostudios.pwp.report;

public enum ReportStatus {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    DISMISSED("Dismissed"),
    ACTION_TAKEN("Action Taken");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
    public boolean isResolved() {
        return this == DISMISSED || this == ACTION_TAKEN;
    }
}
