package com.quratio.bestfruit;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PrivacyPolicy extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;

    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;
    private WebSettings setBew;
    private static long back_pressed;

    private String param = "";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_privacy_policy);

        sharedPreferences = getApplicationContext().getSharedPreferences("DATA", Context.MODE_PRIVATE);
        param = sharedPreferences.getString("param", "");

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        sets();

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        } else {
            cookieManager.setAcceptCookie(true);
        }

        webView.requestFocus(View.FOCUS_DOWN);

        webView.setWebViewClient(new ProWebViewClient());

        Intent intent = getIntent();
        String per = intent.getStringExtra("privpo");

        if (isNetworkConnected()) {
            if (!param.equals("")) {
                webView.loadUrl(param);
            } else if (per != null)
                webView.loadUrl("file:///android_asset/privacy_policy.html");
        } else {
            webView.setWebViewClient(new ProWebViewClient());
            webView.loadUrl("file:///android_asset/error_page.html");
        }

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                showMessageOKCancel(message);
                result.cancel();
                return true;
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                PrivacyPolicy.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), PrivacyPolicy.FCR);
            }

            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                int permissionStatus = ContextCompat.checkSelfPermission(PrivacyPolicy.this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                    if (mUMA != null) {
                        mUMA.onReceiveValue(null);
                    }
                    mUMA = filePathCallback;
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(PrivacyPolicy.this.getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                            takePictureIntent.putExtra("PhotoPath", mCM);
                        } catch (IOException ignored) {
                        }
                        if (photoFile != null) {
                            mCM = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }
                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType("*/*");
                    Intent[] intentArray;
                    if (takePictureIntent != null) {
                        intentArray = new Intent[]{takePictureIntent};
                    } else {
                        intentArray = new Intent[0];
                    }

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, FCR);

                    return true;
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        check_permission();
                    }
                    return false;
                }
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    public void sets(){
        setBew = webView.getSettings();
        setBew.setAppCacheEnabled(true);
        setBew.setDomStorageEnabled(true);
        setBew.setDatabaseEnabled(true);
        setBew.setSupportZoom(false);
        setBew.setAllowFileAccess(true);
        setBew.setAllowContentAccess(true);
        setBew.setJavaScriptEnabled(true);
        setBew.setLoadWithOverviewMode(true);
        setBew.setUseWideViewPort(true);
        setBew.setJavaScriptCanOpenWindowsAutomatically(true);
        setBew.setPluginState(WebSettings.PluginState.ON);
        setBew.setSavePassword(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            CookieSyncManager.getInstance().startSync();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            CookieSyncManager.getInstance().stopSync();
        } catch (Exception ignored) {
        }
    }

    private void showMessageOKCancel(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setTitle("Notification");
        builder.setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Uri[] results = null;
        if (resultCode == PrivacyPolicy.RESULT_OK) {
            if (requestCode == FCR) {
                if (null == mUMA) {
                    return;
                }
                if (intent == null) {
                    if (mCM != null) {
                        results = new Uri[]{Uri.parse(mCM)};
                    }
                } else {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
        }
        mUMA.onReceiveValue(results);
        mUMA = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void check_permission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                }, 1);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if (back_pressed + 2000 > System.currentTimeMillis())
                super.onBackPressed();
            else
                Toast.makeText(getBaseContext(), "Press once again to exit!",
                        Toast.LENGTH_SHORT).show();
            back_pressed = System.currentTimeMillis();
        }
    }

    public void openDeepLink(WebView view_data) {
        try {
            WebView.HitTestResult result = view_data.getHitTestResult();
            String data = result.getExtra();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
            view_data.getContext().startActivity(intent);
        } catch (Exception ex) {
        }
    }

    public void openOtherApp(String url_intent) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url_intent)));
        } catch (Exception ex) {
        }
    }

    private class ProWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUri(view, url);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUri(view, request.getUrl().toString());
        }

        private boolean handleUri(WebView view, final String url) {
            if (url.startsWith("mailto:")) {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                startActivity(i);
                return true;
            } else if (url.startsWith("whatsapp://")) {
                openDeepLink(view);
                return true;
            } else if (url.startsWith("tel:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception ex) {
                }
                return true;
            } else if (url.contains("youtube.com")) {
                openOtherApp(url);
                return true;
            } else if (url.contains("play.google.com/store/apps")) {
                openOtherApp(url);
                return true;
            } else if (url.startsWith("samsungpay://")) {
                openOtherApp(url);
                return true;
            } else if (url.startsWith("viber://")) {
                openDeepLink(view);
                return true;
            } else if (url.startsWith("tg://")) {
                openDeepLink(view);
                return true;
            } else if (url.startsWith("reload://")) {
                Intent intent = new Intent(getApplicationContext(), SplashScreen.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
                if (getApplicationContext() instanceof Activity) {
                    ((Activity) getApplicationContext()).finish();
                }
                Runtime.getRuntime().exit(0);
                return true;
            }else if(url.startsWith("intent://")){
                openOtherApp(url);
                return true;
            }
            else {
                return false;
            }

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putString("param", url);
            ed.apply();

        }
    }
}