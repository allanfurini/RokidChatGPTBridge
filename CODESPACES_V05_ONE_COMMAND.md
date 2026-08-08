Depois de enviar `RokidChatGPTBridge_v0_5.zip` para a raiz do Codespaces, cole:

```bash
ZIP="RokidChatGPTBridge_v0_5.zip"; TMP="$(mktemp -d)"; unzip -oq "$ZIP" -d "$TMP" && find . -mindepth 1 -maxdepth 1 ! -name .git ! -name "$ZIP" -exec rm -rf {} + && cp -a "$TMP"/. . && rm -rf "$TMP" "$ZIP" && git add -A && (git commit -m "Rokid ChatGPT Bridge v0.5 diagnóstico" || true) && git push && SHA="$(git rev-parse HEAD)" && echo "Aguardando build..." && RUN_ID="" && for i in $(seq 1 30); do RUN_ID="$(gh run list --workflow build-apk.yml --limit 10 --json databaseId,headSha --jq ".[] | select(.headSha==\"$SHA\") | .databaseId" | head -n1)"; [ -n "$RUN_ID" ] && break; sleep 5; done && [ -n "$RUN_ID" ] && gh run watch "$RUN_ID" --exit-status && rm -rf APK && mkdir APK && gh run download "$RUN_ID" -n RokidChatGPTBridge-v0.5-debug -D APK && echo "APK pronto:" && find APK -type f -name "*.apk" -print
```
