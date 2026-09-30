// Claude API を使う AI エンジン。
import Anthropic from '@anthropic-ai/sdk';
import { betaZodOutputFormat } from '@anthropic-ai/sdk/helpers/beta/zod';
import {
  buildPartnerSystem,
  toPartnerMessages,
  FEEDBACK_SYSTEM,
  buildFeedbackPrompt,
  HINT_SYSTEM,
  buildHintPrompt,
  TRANSLATE_SYSTEM,
  buildTranslatePrompt,
  SUMMARY_SYSTEM,
  buildSummaryPrompt,
} from './prompts.js';
import { FeedbackSchema, HintSchema, TranslateSchema, SummarySchema } from './schemas.js';
import { AIError } from './errors.js';

export const DEFAULT_MODEL = 'claude-opus-5-5';

// サーバー側フォールバック: 安全分類器がリクエストを断った場合に
// Anthropic 推奨のモデルで自動的に再実行する (Claude API のみ対応)。
const FALLBACK_BETA = 'server-side-fallback-2026-07-01';

/** effort (思考の深さ) を受け付けないモデル */
const supportsEffort = (model) => !/haiku/.test(model);
/** fallbacks: "default" を受け付けるモデル */
const supportsFallbacks = (model) => /^claude-(opus-5|fable-5-1|sonnet-5-5)/.test(model);

/**
 * @param {object} [opts]
 * @param {Anthropic} [opts.client]
 * @param {string} [opts.model]
 * @param {'low'|'medium'|'high'|'xhigh'|'max'} [opts.fastEffort]    会話・フィードバック・ヒント・翻訳用
 * @param {'low'|'medium'|'high'|'xhigh'|'max'} [opts.summaryEffort] 振り返り用
 */
export function createClaudeEngine({
  client = new Anthropic(),
  model = DEFAULT_MODEL,
  fastEffort = 'low',
  summaryEffort = 'medium',
} = {}) {
  /** モデルに応じて共通パラメータを付ける */
  function common(effort, extraOutputConfig = {}) {
    const params = { model, max_tokens: 16000 };
    const outputConfig = { ...extraOutputConfig };
    if (supportsEffort(model)) outputConfig.effort = effort;
    if (Object.keys(outputConfig).length) params.output_config = outputConfig;
    if (supportsFallbacks(model)) {
      params.betas = [FALLBACK_BETA];
      params.fallbacks = 'default';
    }
    return params;
  }

  /** 構造化出力 (JSON) で1回呼び出す */
  async function structured(schema, system, content, effort) {
    let res;
    try {
      res = await client.beta.messages.parse({
        ...common(effort, { format: betaZodOutputFormat(schema) }),
        system,
        messages: [{ role: 'user', content }],
      });
    } catch (err) {
      throw toAIError(err);
    }
    if (res.stop_reason === 'refusal') throw new AIError('refusal');
    if (!res.parsed_output) throw new AIError('bad_output');
    return res.parsed_output;
  }

  return {
    name: 'claude',
    model,

    /**
     * 会話相手の返答をストリーミング生成する。
     * @param {{scenario: object, level: string, history: {role: string, text: string}[]}} input
     * @param {(delta: string) => void} onDelta
     * @param {AbortSignal} [signal]
     * @returns {Promise<string>} 返答全文
     */
    async streamReply({ scenario, level, history }, onDelta, signal) {
      const stream = client.beta.messages.stream(
        {
          ...common(fastEffort),
          // 会話が続くほど前半が共通になるので、自動キャッシュで入力コストを下げる
          cache_control: { type: 'ephemeral' },
          system: buildPartnerSystem(scenario, level),
          messages: toPartnerMessages(history),
        },
        { signal },
      );
      // フォールバックが途中で起きても、テキストは同じストリームで続きから届く
      stream.on('text', (delta) => onDelta(delta));
      let message;
      try {
        message = await stream.finalMessage();
      } catch (err) {
        throw toAIError(err);
      }
      if (message.stop_reason === 'refusal') throw new AIError('refusal');
      return message.content
        .filter((b) => b.type === 'text')
        .map((b) => b.text)
        .join('')
        .trim();
    },

    feedback({ scenario, level, history }) {
      return structured(FeedbackSchema, FEEDBACK_SYSTEM, buildFeedbackPrompt(scenario, level, history), fastEffort);
    },

    hint({ scenario, level, history, want }) {
      return structured(HintSchema, HINT_SYSTEM, buildHintPrompt(scenario, level, history, want), fastEffort);
    },

    translate({ text }) {
      return structured(TranslateSchema, TRANSLATE_SYSTEM, buildTranslatePrompt(text), fastEffort);
    },

    summary({ scenario, level, history, completedMissions }) {
      return structured(
        SummarySchema,
        SUMMARY_SYSTEM,
        buildSummaryPrompt(scenario, level, history, completedMissions),
        summaryEffort,
      );
    },
  };
}

/** SDK の例外を、画面に出せる種類に分類する (具体的なクラスから順に判定) */
function toAIError(err) {
  if (err instanceof AIError) return err;
  if (err instanceof Anthropic.APIUserAbortError) return new AIError('aborted', err);
  if (err instanceof Anthropic.AuthenticationError) return new AIError('auth', err);
  if (err instanceof Anthropic.PermissionDeniedError) return new AIError('auth', err);
  if (err instanceof Anthropic.RateLimitError) return new AIError('busy', err);
  if (err instanceof Anthropic.BadRequestError) return new AIError('bad_request', err);
  if (err instanceof Anthropic.APIConnectionError) return new AIError('network', err);
  if (err instanceof Anthropic.APIError) {
    return new AIError(err.status && err.status >= 500 ? 'busy' : 'unknown', err);
  }
  return new AIError('unknown', err);
}
