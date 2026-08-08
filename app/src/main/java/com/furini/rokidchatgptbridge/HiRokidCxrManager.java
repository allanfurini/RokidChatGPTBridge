package com.furini.rokidchatgptbridge;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import com.rokid.cxr.link.CXRLink;
import com.rokid.cxr.link.callbacks.IAudioStreamCbk;
import com.rokid.cxr.link.callbacks.ICXRLinkCbk;
import com.rokid.cxr.link.callbacks.ICustomViewCbk;
import com.rokid.cxr.link.utils.CxrDefs;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class HiRokidCxrManager {
    public interface AudioListener {
        void onAudio(byte[] data);
        void onAudioState(boolean started);
        void onAudioError(int code, String info);
    }

    private static final String GLOBAL_PKG = "com.rokid.sprite.global.aiapp";
    private static final String MEDIA_SERVICE_ACTION =
            "com.rokid.sprite.aiapp.externalapp.MEDIA_STREAM_SERVICE";

    private final Context context;
    private CXRLink cxrLink;
    private AudioListener audioListener;
    private volatile boolean sceneReady = false;

    public HiRokidCxrManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setAudioListener(AudioListener listener) {
        this.audioListener = listener;
    }

    public boolean isSceneReady() {
        return sceneReady;
    }

    public void connect(String token) {
        try {
            DiagnosticStore.INSTANCE.add(context, "Criando sessão CXR-L CUSTOMVIEW...");
            cxrLink = new CXRLink(context);
            cxrLink.configCXRSession(
                    new CxrDefs.CXRSession(CxrDefs.CXRSessionType.CUSTOMVIEW)
            );

            cxrLink.setCXRCustomViewCbk(new ICustomViewCbk() {
                @Override public void onCustomViewOpened() {
                    sceneReady = true;
                    BridgeState.INSTANCE.setSceneReady(true);
                    DiagnosticStore.INSTANCE.add(context, "HUD/CustomView aberto nos óculos.");
                }

                @Override public void onCustomViewUpdated() { }

                @Override public void onCustomViewClosed() {
                    sceneReady = false;
                    BridgeState.INSTANCE.setSceneReady(false);
                }

                @Override public void onCustomViewIconsSent() { }

                @Override public void onCustomViewError(int code, String msg) {
                    sceneReady = false;
                    BridgeState.INSTANCE.setSceneReady(false);
                    DiagnosticStore.INSTANCE.add(
                            context, "Erro CustomView " + code + ": " + msg
                    );
                }
            });

            cxrLink.setCXRLinkCbk(new ICXRLinkCbk() {
                @Override public void onCXRLConnected(boolean connected) {
                    BridgeState.INSTANCE.setCxrConnected(connected);
                    DiagnosticStore.INSTANCE.add(
                            context, "CXR-L conectado: " + connected
                    );
                    if (connected) {
                        showText("ChatGPT Bridge conectado", "Pronto para testar HUD e microfone.");
                    } else {
                        sceneReady = false;
                        BridgeState.INSTANCE.setSceneReady(false);
                    }
                }

                @Override public void onGlassBtConnected(boolean connected) {
                    BridgeState.INSTANCE.setGlassBtConnected(connected);
                    DiagnosticStore.INSTANCE.add(
                            context, "Bluetooth dos óculos: " + connected
                    );
                }

                @Override public void onGlassAiAssistStart() {
                    DiagnosticStore.INSTANCE.add(context, "AI Assist START detectado.");
                }

                @Override public void onGlassAiAssistStop() {
                    DiagnosticStore.INSTANCE.add(context, "AI Assist STOP detectado.");
                }
            });

            cxrLink.setCXRAudioCbk(new IAudioStreamCbk() {
                @Override public void onAudioReceived(byte[] data, int offset, int length) {
                    if (data == null || length <= 0 || offset < 0 || offset + length > data.length) return;
                    byte[] chunk = new byte[length];
                    System.arraycopy(data, offset, chunk, 0, length);
                    AudioListener l = audioListener;
                    if (l != null) l.onAudio(chunk);
                }

                @Override public void onAudioStreamStateChanged(boolean started) {
                    AudioListener l = audioListener;
                    if (l != null) l.onAudioState(started);
                }

                @Override public void onAudioError(int code, String info) {
                    AudioListener l = audioListener;
                    if (l != null) l.onAudioError(code, info);
                }
            });

            boolean ok = cxrLink.connect(token);
            DiagnosticStore.INSTANCE.add(context, "cxrLink.connect retornou: " + ok);

            if (!ok) {
                ServiceConnection internal = findServiceConnection(cxrLink);
                if (internal == null) {
                    DiagnosticStore.INSTANCE.add(context, "ServiceConnection interno não encontrado.");
                    return;
                }

                Intent service = new Intent(MEDIA_SERVICE_ACTION);
                service.setPackage(GLOBAL_PKG);
                service.putExtra("auth_token", token);
                service.putExtra("auth_package", context.getPackageName());

                boolean bindOk = context.bindService(
                        service,
                        internal,
                        Context.BIND_AUTO_CREATE
                );
                DiagnosticStore.INSTANCE.add(context, "bindService manual: " + bindOk);
            }
        } catch (Throwable t) {
            DiagnosticStore.INSTANCE.add(
                    context,
                    "Falha CXR-L: " + t.getClass().getSimpleName() + ": " + t.getMessage()
            );
        }
    }

    public boolean showText(String title, String text) {
        if (cxrLink == null || !BridgeState.INSTANCE.getCxrConnected()) {
            DiagnosticStore.INSTANCE.add(context, "HUD recusado: CXR ainda não conectado.");
            return false;
        }

        String safe = (title + "\n\n" + text)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");

        String json =
                "{\"type\":\"LinearLayout\",\"props\":{\"width\":\"match_parent\"," +
                "\"height\":\"match_parent\",\"orientation\":\"vertical\"," +
                "\"gravity\":\"center\",\"backgroundColor\":\"#000000\",\"padding\":20}," +
                "\"children\":[{\"id\":\"translation_text\",\"type\":\"TextView\"," +
                "\"props\":{\"width\":\"match_parent\",\"height\":\"wrap_content\"," +
                "\"text\":\"" + safe + "\",\"textSize\":34," +
                "\"textColor\":\"#FFFFFF\",\"gravity\":\"center\"}}]}";

        try {
            if (sceneReady) {
                cxrLink.customViewClose();
            }
            boolean ok = cxrLink.customViewOpen(json);
            DiagnosticStore.INSTANCE.add(context, "CustomView enviado: " + ok);
            return ok;
        } catch (Throwable t) {
            DiagnosticStore.INSTANCE.add(context, "Erro HUD: " + t.getMessage());
            return false;
        }
    }

    public boolean startAudio() {
        if (cxrLink == null || !sceneReady) {
            DiagnosticStore.INSTANCE.add(context, "Microfone recusado: HUD/sessão ainda não pronta.");
            return false;
        }
        try {
            boolean ok = cxrLink.startAudioStream(1);
            DiagnosticStore.INSTANCE.add(context, "startAudioStream: " + ok);
            return ok;
        } catch (Throwable t) {
            DiagnosticStore.INSTANCE.add(context, "Erro startAudio: " + t.getMessage());
            return false;
        }
    }

    public void stopAudio() {
        if (cxrLink == null) return;
        try {
            cxrLink.stopAudioStream();
        } catch (Throwable ignored) {}
    }

    private ServiceConnection findServiceConnection(CXRLink link) {
        return search(link, new HashSet<>());
    }

    private ServiceConnection search(Object obj, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) return null;
        visited.add(obj);

        Class<?> c = obj.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(obj);
                    if (v instanceof ServiceConnection) return (ServiceConnection) v;

                    if (v != null &&
                            !(v instanceof String) &&
                            !(v instanceof Number) &&
                            !(v instanceof Boolean) &&
                            !(v instanceof Enum) &&
                            !(v instanceof Class)) {
                        ServiceConnection found = search(v, visited);
                        if (found != null) return found;
                    }
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
