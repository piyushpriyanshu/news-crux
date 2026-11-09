package app.newsbreak;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import app.newsbreak.Engine;
import app.newsbreak.core.Ist;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MainActivity extends Activity {
    static final int REQ_BACKUP = 41;
    static final int REQ_RESTORE = 42;
    WebView printView;
    WebView web;
    boolean pageReady = false;
    String pendingTarget = null;
    final Handler ui = new Handler(Looper.getMainLooper());

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#1D2527"));
        window.setNavigationBarColor(Color.parseColor("#1D2527"));
        this.web = new WebView(this);
        this.web.setBackgroundColor(Color.parseColor("#1D2527"));
        this.web.setOverScrollMode(2);
        WebSettings settings = this.web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setTextZoom(100);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportZoom(false);
        settings.setCacheMode(2);
        this.web.addJavascriptInterface(new Bridge(this), "NBNative");
        this.web.setWebViewClient(new WebViewClient() { // from class: app.newsbreak.MainActivity.1
            @Override // android.webkit.WebViewClient
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
                return MainActivity.this.external(webResourceRequest.getUrl().toString());
            }

            @Override // android.webkit.WebViewClient
            public boolean shouldOverrideUrlLoading(WebView webView, String str) {
                return MainActivity.this.external(str);
            }

            @Override // android.webkit.WebViewClient
            public void onPageFinished(WebView webView, String str) {
                MainActivity.this.pageReady = true;
                if (MainActivity.this.pendingTarget != null) {
                    MainActivity.this.openTarget(MainActivity.this.pendingTarget);
                    MainActivity.this.pendingTarget = null;
                }
            }
        });
        this.web.setWebChromeClient(new WebChromeClient() { // from class: app.newsbreak.MainActivity.2
            @Override // android.webkit.WebChromeClient
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("NewsBreak", consoleMessage.message() + " @" + consoleMessage.lineNumber());
                return true;
            }
        });
        setContentView(this.web);
        Engine.listener = new Engine.Listener() { // from class: app.newsbreak.MainActivity.3
            @Override // app.newsbreak.Engine.Listener
            public void onEvent(final String str) {
                MainActivity.this.ui.post(new Runnable() { // from class: app.newsbreak.MainActivity.3.1
                    @Override // java.lang.Runnable
                    public void run() {
                        if (MainActivity.this.pageReady) {
                            MainActivity.this.web.evaluateJavascript("window.NB&&NB.onNative(" + str + ")", null);
                        }
                    }
                });
            }
        };
        handleIntent(getIntent());
        this.web.loadUrl("file:///android_asset/www/index.html");
        SyncJobService.schedule(getApplicationContext(), false);
        DeliverReceiver.arm(getApplicationContext());
        Notifier.ensureChannels(this);
    }

    void startBackup() {
        this.ui.post(new Runnable() { // from class: app.newsbreak.MainActivity.4
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
                    intent.addCategory("android.intent.category.OPENABLE");
                    intent.setType("application/json");
                    intent.putExtra("android.intent.extra.TITLE", "NewsBreak-backup-" + Ist.dateKey(System.currentTimeMillis()) + ".json");
                    MainActivity.this.startActivityForResult(intent, MainActivity.REQ_BACKUP);
                } catch (Exception e) {
                    MainActivity.this.say("backup", false, "No file picker available: " + e.getMessage());
                }
            }
        });
    }

    void startRestore() {
        this.ui.post(new Runnable() { // from class: app.newsbreak.MainActivity.5
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
                    intent.addCategory("android.intent.category.OPENABLE");
                    intent.setType("*/*");
                    MainActivity.this.startActivityForResult(intent, MainActivity.REQ_RESTORE);
                } catch (Exception e) {
                    MainActivity.this.say("restore", false, "No file picker available: " + e.getMessage());
                }
            }
        });
    }

    void say(String str, boolean z, String str2) {
        try {
            Engine.emit(str, new JSONObject().put("ok", z).put("msg", str2));
        } catch (Exception e) {
        }
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == REQ_BACKUP || i == REQ_RESTORE) {
            if (i2 == -1 && intent != null && intent.getData() != null) {
                final Uri data = intent.getData();
                final boolean z = i == REQ_BACKUP;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (z) {
                                byte[] bytes = Backup.export(MainActivity.this.getApplicationContext()).getBytes("UTF-8");
                                OutputStream out = null;
                                try {
                                    try { out = MainActivity.this.getContentResolver().openOutputStream(data, "wt"); }
                                    catch (Exception e) { out = MainActivity.this.getContentResolver().openOutputStream(data, "w"); }
                                    if (out == null) throw new Exception("Could not open the file");
                                    out.write(bytes);
                                    out.flush();
                                } finally {
                                    if (out != null) try { out.close(); } catch (Exception ignored) {}
                                }
                                say("backup", true, "Backup saved (" + (bytes.length / 1024) + " KB)");
                                return;
                            }
                            InputStream in = MainActivity.this.getContentResolver().openInputStream(data);
                            if (in == null) throw new Exception("Could not open the file");
                            try {
                                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                                byte[] chunk = new byte[16384];
                                long total = 0;
                                int n;
                                while ((n = in.read(chunk)) > 0) {
                                    total += n;
                                    if (total > 62914560L) throw new Exception("That file is too large to be a News Crux backup");
                                    buf.write(chunk, 0, n);
                                }
                                say("restore", true, Backup.restore(MainActivity.this.getApplicationContext(), buf.toString("UTF-8")));
                            } finally {
                                try { in.close(); } catch (Exception ignored) {}
                            }
                        } catch (Throwable t) {
                            say(z ? "backup" : "restore", false, t.getMessage() != null ? t.getMessage() : String.valueOf(t));
                        }
                    }
                }, "nb-backup").start();
                return;
            }
            say(i == REQ_BACKUP ? "backup" : "restore", false, "Cancelled");
        }
    }

    void printHtml(final String str, final String str2) {
        this.ui.post(new Runnable() { // from class: app.newsbreak.MainActivity.7
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (MainActivity.this.printView != null) {
                        try {
                            MainActivity.this.printView.destroy();
                        } catch (Exception e) {
                        }
                    }
                    WebView webView = new WebView(MainActivity.this);
                    MainActivity.this.printView = webView;
                    webView.getSettings().setJavaScriptEnabled(false);
                    webView.setWebViewClient(new WebViewClient() { // from class: app.newsbreak.MainActivity.7.1
                        boolean done;

                        @Override // android.webkit.WebViewClient
                        public void onPageFinished(WebView webView2, String str3) {
                            if (!this.done) {
                                this.done = true;
                                try {
                                    PrintManager printManager = (PrintManager) MainActivity.this.getSystemService("print");
                                    PrintAttributes build = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
                                    if (printManager != null) {
                                        printManager.print(str2, webView2.createPrintDocumentAdapter(str2), build);
                                    }
                                } catch (Exception e2) {
                                    MainActivity.this.say("print", false, String.valueOf(e2.getMessage()));
                                }
                            }
                        }
                    });
                    webView.loadDataWithBaseURL("file:///android_asset/www/", str, "text/html", "UTF-8", null);
                } catch (Exception e2) {
                    MainActivity.this.say("print", false, String.valueOf(e2.getMessage()));
                }
            }
        });
    }

    boolean external(String str) {
        if (str.startsWith("file:")) {
            return false;
        }
        if (str.startsWith("http://") || str.startsWith("https://")) {
            try {
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
            } catch (Exception e) {
            }
        }
        return true;
    }

    void handleIntent(Intent intent) {
        String stringExtra;
        if (intent != null && (stringExtra = intent.getStringExtra("open")) != null) {
            intent.removeExtra("open");
            if (this.pageReady) {
                openTarget(stringExtra);
            } else {
                this.pendingTarget = stringExtra;
            }
        }
    }

    void openTarget(String str) {
        this.web.evaluateJavascript("window.NB&&NB.openTarget(" + JSONObject.quote(str) + ")", null);
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (System.currentTimeMillis() - Store.prefs(this).getLong("lastSync", 0L) > 600000) {
            startSync("open");
        }
        if (this.pageReady) {
            this.web.evaluateJavascript("window.NB&&NB.onResume()", null);
        }
        Updater.checkAsync(this, Updater.OPEN_GAP, true);
    }

    void startSync(String str) {
        if (!Engine.isRunning()) {
            WorkService.start(this, "sync", null, str);
        }
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.pageReady) {
            this.web.evaluateJavascript("window.NB&&NB.onResume()", null);
        }
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (this.pageReady) {
            this.web.evaluateJavascript("window.NB?NB.back():false", new ValueCallback<String>() { // from class: app.newsbreak.MainActivity.8
                @Override // android.webkit.ValueCallback
                public void onReceiveValue(String str) {
                    if (!"true".equals(str)) {
                        MainActivity.this.moveTaskToBack(true);
                    }
                }
            });
        } else {
            super.onBackPressed();
        }
    }

    void setBars(final String str, final boolean z) {
        this.ui.post(new Runnable() { // from class: app.newsbreak.MainActivity.9
            @Override // java.lang.Runnable
            public void run() {
                try {
                    int parseColor = Color.parseColor(str);
                    Window window = MainActivity.this.getWindow();
                    window.setStatusBarColor(parseColor);
                    window.setNavigationBarColor(parseColor);
                    View decorView = window.getDecorView();
                    int systemUiVisibility = decorView.getSystemUiVisibility();
                    int i = (Build.VERSION.SDK_INT >= 26 ? 16 : 0) | 8192;
                    decorView.setSystemUiVisibility(z ? (i ^ (-1)) & systemUiVisibility : i | systemUiVisibility);
                    MainActivity.this.web.setBackgroundColor(parseColor);
                } catch (Exception e) {
                }
            }
        });
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        Engine.listener = null;
        if (this.printView != null) {
            try {
                this.printView.destroy();
            } catch (Exception e) {
            }
        }
        if (this.web != null) {
            this.web.destroy();
        }
        super.onDestroy();
    }
}
