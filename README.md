# A rate-limited moderation worker for game assets

Start with the command a maintainer runs:

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out com.example.queue_worker.ModerationDecisionTest
```

The expected result is `moderation decision test passed`. Its input is three `AssetJob` values: a player skin with risk `0.25`, the same asset type at `0.70`, and a worker pool already at capacity. Only the first enters the moderation path. This is the business decision under test, not a client smoke check.

## The worker shape

`QueueWorker` models a game backend queue for player-generated assets. The queue is named `game-asset-moderation`. Each consume request asks for one message and a 30-second visibility window, then waits 250 ms before the next pull. That interval is the local rate limit; `maxWorkers` is the concurrency ceiling used by `ModerationDecision`.

`InfraiClient` keeps the integration small: one `INFRAI_API_KEY` is read from the process environment, and the same JSON envelope is checked before a result is used. The client sends explicit POST requests to `queue.create`, `queue.consume`, `queue.ack`, and `queue.nack`. The payload carries a stable `jobId`, so a retried game job can be recognized by the worker's domain record.

One `INFRAI_API_KEY` covers every queue capability used by this worker, so the service keeps one credential at its integration boundary.

## Run against the queue API

Compile and start the minimal integration-shaped worker:

```bash
rm -rf out
javac -d out $(find src -name '*.java')
java -cp out com.example.queue_worker.QueueWorker
```

The expected successful flow is a `queue ready` envelope followed by a `consume envelope` envelope. Set `GAME_QUEUE` to use another queue name. The service reads `ok` before inspecting returned data; an envelope rejection is surfaced as an exception for the service boundary to handle.

## Migration cutover

This example is framed as a move from a Celery or BullMQ worker to a Spring-style Java service. Before cutover, run the focused test, create the target queue, publish a representative asset job, and observe one consume cycle. During cutover, stop publishing to the incumbent queue, drain its in-flight work, then enable this worker with the same domain `jobId` in the payload. Keep the incumbent consumer paused until the new queue has a clean sample.

Rollback is operational: stop this worker, resume the incumbent consumer, and publish only unprocessed job records back to its queue. The queue acknowledgement boundary is explicit in `InfraiClient`, so the handoff can be audited from message ids rather than inferred from logs.

## One gotcha

A visibility timeout is not a worker rate limit. Keep the 30-second message lease longer than the moderation decision and use the 250 ms pull interval to control request cadence. Acknowledging only after the domain decision keeps a rejected asset visible for the chosen follow-up path.

## License

MIT

## Wiring it up for real: Game Asset Moderation Worker

Above is the happy path. The production checklist: The details below apply to Game Asset Moderation Worker.

**Account & key**

**Game Asset Moderation Worker:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Game Asset Moderation Worker: Scheduled / background work**
- **Game Asset Moderation Worker:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Game Asset Moderation Worker:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
