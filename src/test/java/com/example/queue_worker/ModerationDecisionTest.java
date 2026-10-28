package com.example.queue_worker;

public final class ModerationDecisionTest {
    public static void main(String[] args) {
        AssetJob safeSkin = new AssetJob("asset-17", "player-skin", 0.25);
        AssetJob riskySkin = new AssetJob("asset-18", "player-skin", 0.70);
        if (!ModerationDecision.allow(safeSkin, 1, 4)) throw new AssertionError("safe skin should enter moderation");
        if (ModerationDecision.allow(riskySkin, 1, 4)) throw new AssertionError("risk threshold should reject");
        if (ModerationDecision.allow(safeSkin, 4, 4)) throw new AssertionError("worker limit should reject");
        System.out.println("moderation decision test passed");
    }
}
