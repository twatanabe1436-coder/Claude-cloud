// HTTP API と静的ファイル配信。AI エンジンは差し替え可能 (claude / mock)。
import crypto from 'node:crypto';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import { publicCatalog, resolveScenario } from './scenarios.js';
import { ReplyBody, FeedbackBody, HintBody, TranslateBody, SummaryBody } from './ai/schemas.js';
import { AIError } from './ai/errors.js';

const here = path.dirname(fileURLToPath(import.meta.url));
const DEFAULT_STATIC = path.join(here, '..', 'public');

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function parse(schema, body) {
  const r = schema.safeParse(body);
  if (!r.success) {
    const issue = r.error.issues[0];
    const where = issue.path.join('.');
    throw new HttpError(400, `入力が不正です: ${where ? `${where}: ` : ''}${issue.message}`);
  }
  return r.data;
}

function scenarioOf(id) {
  const s = resolveScenario(id);
  if (!s) throw new HttpError(404, 'シナリオが見つかりません');
  return s;
}

function sameSecret(a, b) {
  const ha = crypto.createHash('sha256').update(String(a)).digest();
  const hb = crypto.createHash('sha256').update(String(b)).digest();
  return crypto.timingSafeEqual(ha, hb);
}

/**
 * @param {object} opts
 * @param {ReturnType<import('./ai/claude.js').createClaudeEngine>} opts.engine
 * @param {string} [opts.passcode]  設定するとAPI利用時に合言葉が必要になる (公開サーバー向け)
 * @param {string} [opts.staticDir]
 * @param {(err: unknown) => void} [opts.log]
 */
export function createApp({ engine, passcode = '', staticDir = DEFAULT_STATIC, log = console.error }) {
  const app = express();
  app.disable('x-powered-by');
  app.use(express.json({ limit: '256kb' }));

  app.get('/api/config', (_req, res) => {
    res.json({ mode: engine.name, model: engine.model, needsPasscode: Boolean(passcode) });
  });

  app.get('/api/catalog', (_req, res) => {
    res.json(publicCatalog());
  });

  // ここから下の API は AI を呼ぶので、合言葉が設定されていればチェックする
  app.use('/api', (req, _res, next) => {
    if (!passcode) return next();
    const given = req.get('x-hanaso-passcode') ?? '';
    if (given && sameSecret(given, passcode)) return next();
    next(new HttpError(401, '合言葉が違います'));
  });

  app.post('/api/check-passcode', (_req, res) => res.json({ ok: true }));

  // 会話相手の返答: NDJSON で1行ずつストリーミングする
  //   {"type":"delta","text":"..."} ... {"type":"done","text":"全文"}
  //   失敗時は {"type":"error","message":"..."}
  app.post('/api/reply', async (req, res, next) => {
    let input;
    try {
      const body = parse(ReplyBody, req.body);
      input = { scenario: scenarioOf(body.scenarioId), level: body.level, history: body.history };
    } catch (err) {
      return next(err);
    }

    res.status(200);
    res.setHeader('Content-Type', 'application/x-ndjson; charset=utf-8');
    res.setHeader('Cache-Control', 'no-cache, no-transform');
    res.setHeader('X-Accel-Buffering', 'no');
    res.flushHeaders();

    const abort = new AbortController();
    res.on('close', () => {
      if (!res.writableFinished) abort.abort();
    });
    const send = (obj) => {
      if (!res.writableEnded) res.write(JSON.stringify(obj) + '\n');
    };

    try {
      const text = await engine.streamReply(input, (delta) => send({ type: 'delta', text: delta }), abort.signal);
      send({ type: 'done', text });
    } catch (err) {
      if (abort.signal.aborted) return;
      const aiErr = err instanceof AIError ? err : new AIError('unknown', err);
      if (aiErr.kind !== 'refusal') log(aiErr.cause ?? aiErr);
      send({ type: 'error', kind: aiErr.kind, message: aiErr.message });
    } finally {
      res.end();
    }
  });

  /** JSON を1回で返す API の共通処理 */
  const jsonRoute = (schema, run) => async (req, res, next) => {
    try {
      const body = parse(schema, req.body);
      res.json(await run(body));
    } catch (err) {
      next(err);
    }
  };

  app.post(
    '/api/feedback',
    jsonRoute(FeedbackBody, (b) =>
      engine.feedback({ scenario: scenarioOf(b.scenarioId), level: b.level, history: b.history }),
    ),
  );

  app.post(
    '/api/hint',
    jsonRoute(HintBody, (b) =>
      engine.hint({ scenario: scenarioOf(b.scenarioId), level: b.level, history: b.history, want: b.want || undefined }),
    ),
  );

  app.post('/api/translate', jsonRoute(TranslateBody, (b) => engine.translate({ text: b.text })));

  app.post(
    '/api/summary',
    jsonRoute(SummaryBody, async (b) => {
      const scenario = scenarioOf(b.scenarioId);
      const valid = new Set(scenario.missions.map((m) => m.id));
      const completedMissions = b.completedMissions.filter((id) => valid.has(id));
      const s = await engine.summary({ scenario, level: b.level, history: b.history, completedMissions });
      return { ...s, score: Math.max(0, Math.min(100, Math.round(s.score))) };
    }),
  );

  app.use('/api', (_req, _res, next) => next(new HttpError(404, 'API が見つかりません')));

  app.use(express.static(staticDir, { extensions: ['html'] }));

  // eslint-disable-next-line no-unused-vars
  app.use((err, _req, res, _next) => {
    if (err instanceof HttpError) return res.status(err.status).json({ error: err.message });
    if (err instanceof AIError) {
      if (err.kind !== 'refusal') log(err.cause ?? err);
      return res.status(err.status).json({ error: err.message, kind: err.kind });
    }
    if (err?.type === 'entity.parse.failed') return res.status(400).json({ error: 'JSON が不正です' });
    if (err?.type === 'entity.too.large') return res.status(413).json({ error: '会話が長すぎます' });
    log(err);
    res.status(500).json({ error: 'サーバーエラーが発生しました' });
  });

  return app;
}
