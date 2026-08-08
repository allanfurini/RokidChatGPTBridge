Depois de enviar `RokidChatGPTBridge_v0_6_HiRokid.zip` para a raiz do Codespaces:

```bash
ZIP="RokidChatGPTBridge_v0_6_HiRokid.zip"; TMP="$(mktemp -d)"; unzip -oq "$ZIP" -d "$TMP" && find . -mindepth 1 -maxdepth 1 ! -name .git ! -name "$ZIP" -exec rm -rf {} + && cp -a "$TMP"/. . && rm -rf "$TMP" "$ZIP" && git add -A && (git commit -m "v0.6 Hi Rokid CXR-L direto" || true) && git push && SHA="$(git rev-parse HEAD)" && RUN_ID="" && echo "Aguardando build..." && for i in $(seq 1 40); do RUN_ID="$(gh run list --workflow build-apk.yml --limit 10 --json databaseId,headSha --jq ".[] | select(.headSha==\"$SHA\") | .databaseId" | head -n1)"; [ -n "$RUN_ID" ] && break; sleep 5; done && [ -n "$RUN_ID" ] && gh run watch "$RUN_ID" --exit-status && rm -rf APK && mkdir APK && gh run download "$RUN_ID" -n RokidChatGPTBridge-v0.6-debug -D APK && echo "APK pronto:" && find APK -type f -name "*.apk" -print
```
