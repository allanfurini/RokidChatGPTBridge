# Um comando no Codespaces

Depois de enviar `RokidChatGPTBridge_v0_3.zip` para a raiz do repositório, rode:

```bash
ZIP="RokidChatGPTBridge_v0_3.zip"; TMP="$(mktemp -d)"; unzip -oq "$ZIP" -d "$TMP" && find . -mindepth 1 -maxdepth 1 ! -name .git ! -name "$ZIP" -exec rm -rf {} + && cp -a "$TMP"/. . && rm -rf "$TMP" "$ZIP" && git add -A && (git commit -m "Rokid ChatGPT Bridge v0.3" || true) && git push && gh workflow run build-apk.yml && sleep 5 && RUN_ID="$(gh run list --workflow build-apk.yml --limit 1 --json databaseId --jq '.[0].databaseId')" && gh run watch "$RUN_ID" --exit-status && mkdir -p APK && gh run download "$RUN_ID" -n RokidChatGPTBridge-v0.3-debug -D APK && echo && echo "APK pronto em: APK/app-debug.apk"
```
