package com.cloudoki.artoolkitplugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.artoolkit.ar.base.ARToolKit;
import org.artoolkit.ar.base.AndroidUtils;
import org.artoolkit.ar.base.assets.AssetHelper;
import org.artoolkit.ar.base.camera.CameraPreferencesActivity;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.GLSurfaceView;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

public class ARToolKitPlugin extends CordovaPlugin {
    private static final String TAG = ARToolKitPlugin.class.getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSIONS = 133;

    // Load the native libraries.
    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("artoolkitnft");
    }

    // Lifecycle functions.
    public static native boolean nativeCreate(Context ctx);
    public static native boolean nativeStart();
    public static native boolean nativeStop();
    public static native boolean nativeDestroy();
    // Camera functions.
    public static native boolean nativeVideoInit(int w, int h, int cameraIndex, boolean cameraIsFrontFacing);
    public static native void nativeVideoFrame(byte[] image);
    // OpenGL functions.
    public static native void nativeSurfaceCreated();
    public static native void nativeSurfaceChanged(int w, int h);
    public static native void nativeDrawFrame();
    // Other functions.
    public static native void nativeDisplayParametersChanged(int orientation, int w, int h, int dpi); // 0 = portrait, 1 = landscape (device rotated 90 degrees ccw), 2 = portrait upside down, 3 = landscape reverse (device rotated 90 degrees cw).
    public static native void nativeSetInternetState(int state);

    public static native int nativeGetMarker();

    private GLSurfaceView glView;
    private CameraSurface camSurface;

    private View mainLayout;

    private Activity activity;
    private Renderer renderer;

    private boolean didIntent = false;
    private CallbackContext cb;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        activity = cordova.getActivity();
        PreferenceManager.setDefaultValues(activity.getApplicationContext(), org.artoolkit.ar.base.R.xml.preferences, false);

        super.initialize(cordova, webView);

        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        updateNativeDisplayParameters();
        AndroidUtils.reportDisplayInformation(activity);

        ARToolKitPlugin.nativeCreate(activity);

        AssetHelper assetHelper = new AssetHelper(activity.getAssets());
        assetHelper.cacheAssetFolder(activity.getApplication(), "Data");
        assetHelper.cacheAssetFolder(activity.getApplication(), "DataNFT");
    }

    @Override
    public boolean execute(String action, JSONArray data,
                           CallbackContext callbackContext) throws JSONException {
        if (action.equals("greet")) {
            Log.i(TAG, "greet called");
            String name = data.getString(0);
            String message = "Hello, " + name;
            callbackContext.success(message);
            return true;
        }
        if (action.equals("init")) {
            Log.i(TAG, "init called");
            renderer.setCallbackContext(callbackContext);//getMarkerStatus(callbackContext);
            return true;
        }
        if(action.equals("menu")) {
            activity.startActivity(new Intent(activity, CameraPreferencesActivity.class));
            cb = callbackContext;
            didIntent = true;
            return true;
        }
        return false;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        Log.i(TAG, "onStart(): Activity starting");

        if(!ARToolKit.getInstance().initialiseNative(activity.getCacheDir().getAbsolutePath())) {
            new AlertDialog.Builder(activity)
                    .setMessage("The native library is not loaded. The application cannot continue.")
                    .setTitle("Error")
                    .setCancelable(true)
                    .setNeutralButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.finish();
                                }
                            })
                    .show();
        }

        if(!checkCameraPermission()) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSIONS);
        }

        mainLayout = webView.getView();
        mainLayout.setBackgroundColor(0x00000000);
        mainLayout.bringToFront();

        ARToolKitPlugin.nativeStart();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("deprecation") // FILL_PARENT still required for API level 7 (Android 2.1)
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);

        // Update info on whether we have an Internet connection.
        ConnectivityManager cm = (ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        nativeSetInternetState(isConnected ? 1 : 0);

        // In order to ensure that the GL surface covers the camera preview each time onStart
        // is called, remove and add both back into the FrameLayout.
        // Removing GLSurfaceView also appears to cause the GL surface to be disposed of.
        // To work around this, we also recreate GLSurfaceView. This is not a lot of extra
        // work, since Android has already destroyed the OpenGL context too, requiring us to
        // recreate that and reload textures etc.

        // Create the camera view.
        camSurface = new CameraSurface(activity);

        // Create/recreate the GL view.
        glView = new GLSurfaceView(activity);
        //glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Do we actually need a transparent surface? I think not, (default is RGB888 with depth=16) and anyway, Android 2.2 barfs on this.
        renderer = new Renderer();
        glView.setRenderer(renderer);
        glView.setZOrderMediaOverlay(true); // Request that GL view's SurfaceView be on top of other SurfaceViews (including CameraPreview's SurfaceView).

        ((ViewGroup) mainLayout.getParent()).addView(camSurface, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((ViewGroup) mainLayout.getParent()).addView(glView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mainLayout.setBackgroundColor(0x00000000);
        mainLayout.bringToFront();

        if (glView != null) glView.onResume();

        if(didIntent) {
            cb.success();
            didIntent = false;
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        if (glView != null) glView.onPause();

        // System hardware must be release in onPause(), so it's available to
        // any incoming activity. Removing the CameraPreview will do this for the
        // camera. Also do it for the GLSurfaceView, since it serves no purpose
        // with the camera preview gone.
        ((ViewGroup) mainLayout.getParent()).removeView(glView);
        ((ViewGroup) mainLayout.getParent()).removeView(camSurface);
    }

    @Override
    public void onStop() {
        super.onStop();

        ARToolKitPlugin.nativeStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ARToolKitPlugin.nativeDestroy();
    }

    private void updateNativeDisplayParameters()
    {
        Display d = activity.getWindowManager().getDefaultDisplay();
        int orientation = d.getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        d.getMetrics(dm);
        int w = dm.widthPixels;
        int h = dm.heightPixels;
        int dpi = dm.densityDpi;
        nativeDisplayParametersChanged(orientation, w, h, dpi);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        // We won't use the orientation from the config, as it only tells us the layout type
        // and not the actual orientation angle.
        //int nativeOrientation;
        //int orientation = newConfig.orientation; // Only portrait or landscape.
        //if (orientation == Configuration.ORIENTATION_LANSCAPE) nativeOrientation = 0;
        //else /* orientation == Configuration.ORIENTATION_PORTRAIT) */ nativeOrientation = 1;
        updateNativeDisplayParameters();
    }
}
