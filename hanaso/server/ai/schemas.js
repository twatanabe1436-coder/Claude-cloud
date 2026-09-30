// Claude の構造化出力 (JSON) のスキーマと、API リクエストボディの検証スキーマ。
import * as z from 'zod';
import { LEVELS } from '../scenarios.js';

// ---- Claude の出力 -----------------------------------------------------------

export const FeedbackSchema = z.object({
  rating: z.enum(['great', 'good', 'fix']),
  corrected: z.string(),
  natural: z.string(),
  explanation_ja: z.string(),
  mistakes: z.array(z.object({ wrong: z.string(), right: z.string(), note_ja: z.string() })),
  completed_missions: z.array(z.string()),
});

export const HintSchema = z.object({
  suggestions: z.array(z.object({ label_ja: z.string(), en: z.string(), ja: z.string() })),
});

export const TranslateSchema = z.object({
  ja: z.string(),
  words: z.array(z.object({ en: z.string(), ja: z.string() })),
});

export const SummarySchema = z.object({
  score: z.number().int(),
  headline_ja: z.string(),
  good_points_ja: z.array(z.string()),
  improve_points: z.array(z.object({ point_ja: z.string(), example_en: z.string() })),
  key_phrases: z.array(z.object({ en: z.string(), ja: z.string() })),
  next_challenge_ja: z.string(),
});

// ---- API リクエスト -----------------------------------------------------------

const HistoryItem = z.object({
  role: z.enum(['ai', 'user']),
  text: z.string().trim().min(1).max(1500),
});

const ConversationBody = z.object({
  scenarioId: z.string().max(64),
  level: z.enum(LEVELS),
  history: z.array(HistoryItem).min(1).max(120),
});

const endsWithUser = (b) => b.history[b.history.length - 1].role === 'user';

export const ReplyBody = ConversationBody.refine(endsWithUser, {
  message: 'history must end with a learner message',
});
export const FeedbackBody = ReplyBody;
export const HintBody = ConversationBody.extend({
  want: z.string().trim().max(300).optional(),
});
export const TranslateBody = z.object({ text: z.string().trim().min(1).max(1500) });
export const SummaryBody = ConversationBody.extend({
  completedMissions: z.array(z.string().max(64)).max(20).default([]),
});
