package com.deepmindsit.adcenter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_ACCESS_STORAGE = 1;
    private WebView webView;
    private SwipeRefreshLayout pullRefresh;
    private TextView errorTitle, errorMessage;
    private LinearLayout errorView, noInternetView, splashView;
    private View errorMask;
    private Button tryAgain,checkSettings;
    private CoordinatorLayout snackBarView;

    private static final String TAG = MainActivity.class.getSimpleName();
    private String mCM;
    @SuppressWarnings("rawtypes")
    private ValueCallback mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;
    private final boolean multiple_files = true;

    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 5.1.1; SM-G928X Build/LMY47X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);

        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;
            if(resultCode== Activity.RESULT_OK){
                if(requestCode == FCR){
                    if(null == mUMA){
                        return;
                    }
                    if(intent == null || intent.getData() == null){
                        if(mCM != null){
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    }else{
                        String dataString = intent.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        } else {
                            if(multiple_files) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }
        else {
            if (requestCode == FCR) {
                if (null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                //noinspection unchecked
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAllId();
        loadWebView();
        loadURL();
        setPullRefresh();
        buttonsInit();


    }



       private void initAllId() {
        webView = findViewById(R.id.web_view);
        pullRefresh = findViewById(R.id.pull_refresh);
        errorTitle = findViewById(R.id.error_title);
        errorMessage = findViewById(R.id.error_message);
        errorView = findViewById(R.id.error_view);
        noInternetView = findViewById(R.id.no_internet_view);
        errorMask = findViewById(R.id.error_mask);
        tryAgain = findViewById(R.id.try_again);
        checkSettings = findViewById(R.id.check_settings);
        snackBarView = findViewById(R.id.snack_bar_view);
        splashView = findViewById(R.id.splash_view);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void loadWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportMultipleWindows(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setUserAgentString(USER_AGENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_ACCESS_STORAGE);
            } else {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file…");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                assert dm != null;
                dm.enqueue(request);
                snackBarDownload();
                if (errorView.getVisibility()==View.VISIBLE)errorView.setVisibility(View.GONE);
            }
        });
    }

    private void loadURL() {
        if (getIntent().getData() != null) {
            if (getIntent().getData().toString().startsWith("myapp")){
                webView.loadUrl(getIntent().getData().toString().replace("myapp", "https"));
            } else webView.loadUrl(getIntent().getData().toString());
            splashView.setVisibility(View.GONE);
        }
        else {
            webView.loadUrl(getString(R.string.base_url));
        }
    }

    private void setPullRefresh() {
        pullRefresh.setColorSchemeResources(R.color.colorAccent);
        pullRefresh.setProgressViewOffset(false, 0,180);
        pullRefresh.setOnRefreshListener(() -> webView.reload());
    }

    private void buttonsInit() {
        tryAgain.setOnClickListener(v -> {
            webView.reload();
            errorView.setVisibility(View.GONE);
            errorMask.setVisibility(View.VISIBLE);
        });
        checkSettings.setOnClickListener(v -> {
            Intent intent=new Intent(Settings.ACTION_WIFI_SETTINGS);
            startActivity(intent);
        });
    }

    private class MyWebViewClient extends WebViewClient {
        @SuppressLint("QueryPermissionsNeeded")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("tel:")) {
                Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(i);
                return true;
            } else if (url.startsWith("mailto:")) {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                startActivity(i);
                return true;
            } else if(url.contains("https://www.google.com/maps/") || url.contains("geo:")
            || url.contains("https://maps.google.com/")) {
                Uri IntentUri = Uri.parse(url);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, IntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
                return true;
            } else if (url.contains("https://play.google.com/store/apps/")
                    ||url.contains("www.youtube.com")
                    ||url.contains("twitter.com")
                    ||url.contains("facebook.com")
                    ||url.contains("reddit.com")
                    ||url.contains("whatsapp.com")
                    ||url.contains("skype.com")
                    ||url.contains("telegram.me")
                    ||url.contains("pinterest.com")
                    ||url.contains("linkedin.com")
                    ||url.contains("m.me")
                    ||url.contains("instagram.com") ){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            } else if (url.startsWith("intent://")) {
                try {
                    Context context = view.getContext();
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        view.stopLoading();
                        PackageManager packageManager = context.getPackageManager();
                        ResolveInfo info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                        if (info != null) {
                            context.startActivity(intent);
                        } else {
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
                            context.startActivity(browserIntent);
                        }
                        return true;
                    }
                } catch (URISyntaxException e) {
                    Toast.makeText(MainActivity.this, "Can't resolve intent://", Toast.LENGTH_SHORT).show();
                }
            } else if(url.startsWith("whatsapp://")) {
                try {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.setPackage("com.whatsapp");
                    startActivity(sendIntent);
                } catch (Exception exception) {
                    Toast.makeText(MainActivity.this, "WhatsApp not installed!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            else if(URLUtil.isNetworkUrl(url)) {
                return false;
            }
            return false;
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if(errorView.getVisibility()==View.GONE) errorView.setVisibility(View.VISIBLE);
            String message = null;
            String title = null;
            if (errorCode == WebViewClient.ERROR_AUTHENTICATION) {
                message = "User authentication failed on server";
                title = "Auth Error";
            } else if (errorCode == WebViewClient.ERROR_TIMEOUT) {
                message = "The server is taking too much time to communicate. Try again later.";
                title = "Connection Timeout";
            } else if (errorCode == WebViewClient.ERROR_TOO_MANY_REQUESTS) {
                message = "Too many requests during this load";
                title = "Too Many Requests";
            } else if (errorCode == WebViewClient.ERROR_UNKNOWN) {
                message = "Generic error";
                title = "Unknown Error";
            } else if (errorCode == WebViewClient.ERROR_BAD_URL) {
                message = "Check entered URL..";
                title = "Malformed URL";
            } else if (errorCode == WebViewClient.ERROR_CONNECT) {
                message = "Failed to connect to the server";
                title = "Connection";
            } else if (errorCode == WebViewClient.ERROR_FAILED_SSL_HANDSHAKE) {
                message = "Failed to perform SSL handshake";
                title = "SSL Handshake Failed";
            } else if (errorCode == WebViewClient.ERROR_HOST_LOOKUP) {
                message = "Server or proxy hostname lookup failed";
                title = "Host Lookup Error";
            } else if (errorCode == WebViewClient.ERROR_PROXY_AUTHENTICATION) {
                message = "User authentication failed on proxy";
                title = "Proxy Auth Error";
            } else if (errorCode == WebViewClient.ERROR_REDIRECT_LOOP) {
                message = "Too many redirects";
                title = "Redirect Loop Error";
            } else if (errorCode == WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME) {
                message = "Unsupported authentication scheme (not basic or digest)";
                title = "Auth Scheme Error";
            } else if (errorCode == WebViewClient.ERROR_UNSUPPORTED_SCHEME) {
                message = "Unsupported URI scheme";
                title = "URI Scheme Error";
            } else if (errorCode == WebViewClient.ERROR_FILE) {
                message = "Generic file error";
                title = "File";
            } else if (errorCode == WebViewClient.ERROR_FILE_NOT_FOUND) {
                message = "File not found";
                title = "File";
            } else if (errorCode == WebViewClient.ERROR_IO) {
                message = "The server failed to communicate. Try again later.";
                title = "IO Error";
            }
            if (message != null) {
                errorTitle.setText(title);
                errorMessage.setText(message);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            String message = "SSL Certificate error.";
            switch (error.getPrimaryError()) {
                case SslError.SSL_UNTRUSTED:
                    message = "The certificate authority is not trusted.";
                    break;
                case SslError.SSL_EXPIRED:
                    message = "The certificate has expired.";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message = "The certificate Hostname mismatch.";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message = "The certificate is not yet valid.";
                    break;
            }
            message += " Do you want to continue anyway?";
            builder.setTitle("SSL Certificate Error");
            builder.setMessage(message);
            builder.setPositiveButton("continue", (dialog, which) -> handler.proceed());
            builder.setNegativeButton("cancel", (dialog, which) -> handler.cancel());
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private class MyWebChromeClient extends WebChromeClient {

        public void onProgressChanged(final WebView view, int progress) {

            //noinspection RedundantIfStatement
            if (view.getUrl().startsWith("https://web.adcenter.co.in/")) pullRefresh.setEnabled(false);
            else pullRefresh.setEnabled(true);

            if (progress < 90) {
                pullRefresh.setRefreshing(true);
            } else {
                pullRefresh.setRefreshing(false);
                if (errorMask.getVisibility()==View.VISIBLE) errorMask.setVisibility(View.GONE);
            }


        }

        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.app_name))
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                    .create()
                    .show();
            return true;
        }

        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) { return null; }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }
        @SuppressLint("SourceLockedOrientationActivity")
        public void onHideCustomView() {
            ((FrameLayout)getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }
        @SuppressLint("SourceLockedOrientationActivity")
        public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout)getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    |View.SYSTEM_UI_FLAG_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        @SuppressLint("QueryPermissionsNeeded")
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED )) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_ACCESS_STORAGE);
                return false;
            } else {
                if(mUMA != null){
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null){
                    File photoFile = null;
                    try{
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    }catch(IOException ex){
                        Log.e(TAG, "File creation failed", ex);
                    }
                    if(photoFile != null){
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    }else{
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                if(multiple_files) {
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                Intent[] intentArray;
                if(takePictureIntent != null){
                    intentArray = new Intent[]{takePictureIntent};
                }else{
                    intentArray = new Intent[0];
                }
                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Choose File");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                if(multiple_files) {
                    chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                //noinspection deprecation
                startActivityForResult(chooserIntent, FCR);
                return true;
            }
        }
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack())webView.goBack();
        else if(Objects.equals(webView.getUrl(), getString(R.string.base_url)))exitDialog();
        else exitDialog();
    }

    private void exitDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View exitView = inflater.inflate(R.layout.layout_exit_alert, findViewById(R.id.exit_alert_view));

        final androidx.appcompat.app.AlertDialog exitDialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog).create();
        exitDialog.setView(exitView);
        exitView.findViewById(R.id.btnYes).setOnClickListener(v -> {
            finishAffinity();
            exitDialog.dismiss();
        });
        exitView.findViewById(R.id.btnNo).setOnClickListener(v -> exitDialog.dismiss());
        exitDialog.show();
    }

    private final BroadcastReceiver internetStateReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            if (noConnectivity) {
                if(noInternetView.getVisibility()==View.GONE)noInternetView.setVisibility(View.VISIBLE);
            } else {
                if(noInternetView.getVisibility()==View.VISIBLE)noInternetView.setVisibility(View.GONE);
                if(errorView.getVisibility()==View.VISIBLE) {
                    errorView.setVisibility(View.GONE);
                    errorMask.setVisibility(View.VISIBLE);
                    webView.reload();
                }
            }
        }
    };

    private void snackBarDownload() {
        Snackbar snackbar = Snackbar.make(snackBarView, "Downloading File", Snackbar.LENGTH_LONG)
                .setActionTextColor(Color.YELLOW)
                .setAction("VIEW", view -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
        snackbar.show();
    }

    @Override
    protected void onStart() {
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(internetStateReceiver, intentFilter);
        super.onStart();
    }
    @Override
    protected void onStop() {
        unregisterReceiver(internetStateReceiver);
        super.onStop();
    }
    @Override
    protected void onPause() {
        CookieSyncManager.getInstance().sync();
        CookieManager.getInstance().setAcceptCookie(true);
        webView.onPause();
        super.onPause();
    }
    @Override
    protected void onResume() {
        CookieSyncManager.getInstance().stopSync();
        webView.onResume();
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        webView.clearHistory();
        webView.destroy();
        webView = null;
        super.onDestroy();
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState ){
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (errorView.getVisibility() == View.VISIBLE) errorView.setVisibility(View.GONE);
                webView.reload();
                Toast.makeText(getApplicationContext(), "Permission granted! Now you can proceed!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Permission denied! Can't proceed further!", Toast.LENGTH_LONG).show();
            }
        }
    }
}