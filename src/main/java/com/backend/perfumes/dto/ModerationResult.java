package com.backend.perfumes.dto;

import com.backend.perfumes.model.ModerationStatus;
import lombok.Data;

@Data
public class ModerationResult {
    private ModerationStatus status;
    private String reason;
    private boolean needsHumanReview;

    public ModerationResult() {}

    public ModerationResult(ModerationStatus status, String reason) {
        this.status = status;
        this.reason = reason;
        this.needsHumanReview = status == ModerationStatus.PENDING_REVIEW;
    }
}