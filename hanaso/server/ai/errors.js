// AI 呼び出しの失敗を、画面に出すメッセージと HTTP ステータスに対応づける。

const KINDS = {
  auth: { status: 502, messageJa: 'AI の認証に失敗しました。サーバーの ANTHROPIC_API_KEY を確認してください。' },
  busy: { status: 503, messageJa: 'AI が混み合っています。少し待ってからもう一度お試しください。' },
  network: { status: 502, messageJa: 'AI に接続できませんでした。ネットワークを確認してください。' },
  refusal: { status: 422, messageJa: 'この内容には AI が応答できませんでした。別の言い方で話してみてください。' },
  bad_output: { status: 502, messageJa: 'AI の応答を読み取れませんでした。もう一度お試しください。' },
  bad_request: { status: 502, messageJa: 'AI へのリクエストが不正でした。サーバーのログを確認してください。' },
  aborted: { status: 499, messageJa: '中断しました。' },
  unknown: { status: 500, messageJa: '予期しないエラーが発生しました。もう一度お試しください。' },
};

export class AIError extends Error {
  /**
   * @param {keyof typeof KINDS} kind
   * @param {unknown} [cause]
   */
  constructor(kind, cause) {
    super(KINDS[kind]?.messageJa ?? KINDS.unknown.messageJa, { cause });
    this.name = 'AIError';
    this.kind = kind in KINDS ? kind : 'unknown';
    this.status = KINDS[this.kind].status;
  }
}
