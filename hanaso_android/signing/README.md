# 公開用 APK の署名鍵

Android は、インストール済みのアプリと**同じ鍵で署名された APK** でないと上書き更新できません。
そのため、Releases で配布する APK はいつもこの鍵で署名します。

| ファイル | 内容 |
| --- | --- |
| `hanaso-release.p12` | 署名鍵 (PKCS12、別名 `hanaso`)。秘密鍵はパスワードで暗号化 (AES-256、PBKDF2) されています |
| `release-cert.sha256` | 証明書の SHA-256 フィンガープリント。CI が公開前に APK の署名と照合します |

- パスワードはリポジトリには置かず、GitHub Actions の secret **`HANASO_KEYSTORE_PASSWORD`** にだけ保存します
  (Settings → Secrets and variables → Actions)。
- secret がない環境 (手元のビルド・エミュレータのテスト) では、Android の debug 鍵で署名します。
  その APK は Releases の版とは別アプリ扱いになり、上書きインストールできません。
- CI は、Release を作るときに secret がない、または APK の証明書が `release-cert.sha256` と違うと失敗します。
- この鍵とパスワードをなくすと、以後の版はアンインストールしてから入れ直すことになります。
  パスワードは secret とは別に、安全な場所にも控えておいてください。
