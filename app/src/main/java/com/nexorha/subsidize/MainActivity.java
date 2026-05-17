package com.nexorha.subsidize;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private WebView webView;
    private RelativeLayout splashContainer;
    private RelativeLayout offlineContainer;
    private ProgressBar progressBar;
    
    // File Upload Variables
    private ValueCallback<Uri[]> uploadMessage;
    public static final int FILECHOOSER_RESULTCODE = 100;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        splashContainer = findViewById(R.id.splashContainer);
        offlineContainer = findViewById(R.id.offlineContainer);
        progressBar = findViewById(R.id.progressBar);
        Button btnRefresh = findViewById(R.id.btnRefresh);
        
        ImageView appIcon = findViewById(R.id.appIcon);
        TextView appName = findViewById(R.id.appName);
        LinearLayout poweredByGroup = findViewById(R.id.poweredByGroup);

        // --- 1. RUNTIME HARDWARE PERMISSIONS ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 101);
        }

        // --- 2. HIGH-TECH SPLASH ANIMATION SEQUENCE ---
        appIcon.setAlpha(0f); appIcon.setScaleX(0.3f); appIcon.setScaleY(0.3f); appIcon.setTranslationX(120f);
        appName.setAlpha(0f); appName.setTranslationX(-50f);
        poweredByGroup.setAlpha(0f); poweredByGroup.setTranslationY(40f);

        appIcon.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1000).setInterpolator(new OvershootInterpolator(1.2f)).start();
        new Handler().postDelayed(() -> {
            appIcon.animate().translationX(0f).setDuration(800).setInterpolator(new DecelerateInterpolator()).start();
            appName.animate().alpha(1f).translationX(0f).setDuration(800).setInterpolator(new DecelerateInterpolator()).start();
        }, 1000);
        new Handler().postDelayed(() -> {
            poweredByGroup.animate().alpha(1f).translationY(0f).setDuration(800).setInterpolator(new DecelerateInterpolator()).start();
        }, 1600);


        // --- 3. PRO WEBVIEW SETTINGS (E-Signature & Hardware Accel) ---
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true); // Required for Paystack and E-Signatures
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(true);
        
        // Optimize for E-signature canvas touches
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Forces Mobile Viewport
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // --- 4. HARDWARE BRIDGE (Camera, Files, Progress Bar) ---
        webView.setWebChromeClient(new WebChromeClient() {
            // Handles the tiny purple loading bar
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            // Intercepts HTML file inputs to open Android Camera/File Picker
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) { uploadMessage.onReceiveValue(null); uploadMessage = null; }
                uploadMessage = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");

                Intent[] intentArray = new Intent[]{takePictureIntent};
                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select File or Take Photo");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        // --- 5. NETWORK ROUTING & OFFLINE SCREEN ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Opens WhatsApp, Email, Phone links natively outside the app
                if (url.startsWith("tel:") || url.startsWith("whatsapp:") || url.startsWith("mailto:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false; // Keep standard http/https links (like Paystack) inside the app
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    webView.setVisibility(View.GONE);
                    offlineContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        // Offline Refresh Button Logic
        btnRefresh.setOnClickListener(v -> {
            offlineContainer.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.reload();
        });

        webView.loadUrl("https://subsidize.nexorha.com/login.php");

        // 6.5 Second Splash Delay
        new Handler().postDelayed(() -> {
            if (offlineContainer.getVisibility() != View.VISIBLE) {
                webView.setVisibility(View.VISIBLE);
            }
            splashContainer.animate().alpha(0f).setDuration(800).withEndAction(() -> {
                splashContainer.setVisibility(View.GONE);
            }).start();
        }, 6500); 
    }

    // --- 6. CATCHING THE FILE UPLOAD RESULT ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == uploadMessage) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            uploadMessage.onReceiveValue(result != null ? new Uri[]{result} : null);
            uploadMessage = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (offlineContainer.getVisibility() == View.VISIBLE) { super.onBackPressed(); }
        else if (webView.canGoBack()) { webView.goBack(); } 
        else { super.onBackPressed(); }
    }
}