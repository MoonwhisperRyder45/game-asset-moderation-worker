package com.example.queue_worker;

public final class ModerationDecision {
    private ModerationDecision() { }

    public static boolean allow(AssetJob job, int activeWorkers, int maxWorkers) {
        return job.assetType().equals("player-skin") && activeWorkers < maxWorkers && job.riskScore() < 0.70;
    }
}
