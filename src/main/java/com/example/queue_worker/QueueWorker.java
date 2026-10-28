package com.example.queue_worker;

import java.io.IOException;

public final class QueueWorker {
    private final InfraiClient infrai;
    private final String queue;
    private final int maxWorkers;
    private final long intervalMillis;

    public QueueWorker(InfraiClient infrai, String queue, int maxWorkers, long intervalMillis) {
        this.infrai = infrai; this.queue = queue; this.maxWorkers = maxWorkers; this.intervalMillis = intervalMillis;
    }

    public void consumeOnce(int activeWorkers) throws IOException, InterruptedException {
        String envelope = infrai.queueConsume(queue, 1, 30);
        System.out.println("consume envelope: " + envelope);
        Thread.sleep(intervalMillis);
    }

    public static void main(String[] args) throws Exception {
        String queue = System.getenv().getOrDefault("GAME_QUEUE", "game-asset-moderation");
        String result = new InfraiClient().queueCreate(queue);
        System.out.println("queue ready: " + result);
        new QueueWorker(new InfraiClient(), queue, 4, 250).consumeOnce(0);
    }
}
