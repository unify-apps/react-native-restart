package com.reactnativerestart;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class RestartModule extends ReactContextBaseJavaModule {

    private static final String REACT_APPLICATION_CLASS_NAME = "com.facebook.react.ReactApplication";
    private static final String REACT_NATIVE_HOST_CLASS_NAME = "com.facebook.react.ReactNativeHost";
    private static String restartReason = null;

    private LifecycleEventListener mLifecycleEventListener = null;

    public RestartModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    private static void reloadReactNative(Activity activity) {
        if (activity == null) return;

        Context context = activity.getApplicationContext();

        // Destroy the React instance so RN resets fully
        if (context instanceof ReactApplication) {
            ReactInstanceManager reactInstanceManager =
                    ((ReactApplication) context).getReactNativeHost().getReactInstanceManager();

            if (reactInstanceManager != null) {
                reactInstanceManager.destroy();
            }
        }

        // Restart the activity WITHOUT showing the splash screen
        Intent intent = new Intent(context, activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);

        // Disable transition animation (important for avoiding flicker)
        activity.overridePendingTransition(0, 0);
    }

    private void loadBundleLegacy() {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            return;
        }

        reloadReactNative(currentActivity);
    }

    private void loadBundle() {
        clearLifecycleEventListener();
        try {
            final ReactInstanceManager instanceManager = resolveInstanceManager();
            if (instanceManager == null) {
                return;
            }

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        instanceManager.recreateReactContextInBackground();
                    } catch (Throwable t) {
                        loadBundleLegacy();
                    }
                }
            });

        } catch (Throwable t) {
            loadBundleLegacy();
        }
    }

    private static ReactInstanceHolder mReactInstanceHolder;

    static ReactInstanceManager getReactInstanceManager() {
        if (mReactInstanceHolder == null) {
            return null;
        }
        return mReactInstanceHolder.getReactInstanceManager();
    }

    private ReactInstanceManager resolveInstanceManager() throws NoSuchFieldException, IllegalAccessException {
        ReactInstanceManager instanceManager = getReactInstanceManager();
        if (instanceManager != null) {
            return instanceManager;
        }

        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            return null;
        }

        ReactApplication reactApplication = (ReactApplication) currentActivity.getApplication();
        instanceManager = reactApplication.getReactNativeHost().getReactInstanceManager();

        return instanceManager;
    }

    private void clearLifecycleEventListener() {
        if (mLifecycleEventListener != null) {
            getReactApplicationContext().removeLifecycleEventListener(mLifecycleEventListener);
            mLifecycleEventListener = null;
        }
    }

    @ReactMethod
    public void Restart(String reason) {
        restartReason = reason;
        loadBundle();
    }

    @ReactMethod
    public void restart(String reason) {
        restartReason = reason;
        loadBundle();
    }

    @ReactMethod
    public void getReason(Promise promise) {
        try {
            promise.resolve(restartReason);
        } catch(Exception e) {
            promise.reject("Create Event Error", e);
        }
    }


    @Override
    public String getName() {
        return "RNRestart";
    }

}
