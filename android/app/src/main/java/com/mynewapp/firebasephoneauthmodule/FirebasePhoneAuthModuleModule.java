//  Created by react-native-create-bridge

package com.mynewapp.firebasephoneauthmodule;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FirebasePhoneAuthModuleModule extends ReactContextBaseJavaModule {
    private static final String TAG = "JBFirebase";

    public static final String REACT_CLASS = "FirebasePhoneAuthModule";
    private static ReactApplicationContext reactContext = null;
    public static final String ON_VERIFICATION_COMPLETED = "onFirebasePhoneVerificationCompleted";
    public static final String ON_CODE_AUTO_RETRIEVAL_TIMEOUT = "onFirebasePhoneCodeAutoRetrievalTimeOut";

    public FirebasePhoneAuthModuleModule(ReactApplicationContext context) {
        // Pass in the context to the constructor and save it so you can emit events
        // https://facebook.github.io/react-native/docs/native-modules-android.html#the-toast-module
        super(context);

        reactContext = context;
    }

    @Override
    public String getName() {
        // Tell React the name of the module
        // https://facebook.github.io/react-native/docs/native-modules-android.html#the-toast-module
        return REACT_CLASS;
    }

    @Override
    public Map<String, Object> getConstants() {
        // Export any constants to be used in your native module
        // https://facebook.github.io/react-native/docs/native-modules-android.html#the-toast-module
        final Map<String, Object> constants = new HashMap<>();
        constants.put("ON_VERIFICATION_COMPLETED", ON_VERIFICATION_COMPLETED);
//        constants.put("ON_VERIFICATION_FAILED", ON_VERIFICATION_FAILED);
//        constants.put("ON_CODE_SENT", ON_CODE_SENT);
        constants.put("ON_CODE_AUTO_RETRIEVAL_TIMEOUT", ON_CODE_AUTO_RETRIEVAL_TIMEOUT);
        return constants;
    }

    private void sendEvent(String eventName, Object params) {
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @ReactMethod
    public void exampleMethod (String name, Callback callback) {

        String greeting = "Welcome >> " + name;
        callback.invoke(greeting);
        // An example native method that you will expose to React
        // https://facebook.github.io/react-native/docs/native-modules-android.html#the-toast-module
    }

    @ReactMethod
    public void verifyPhoneNumber(final String phoneNumber, final Promise promise){
        Log.i(TAG, "INSIDE verifyPhoneNumber");
        Log.i(TAG, "phoneNumber >> "+ phoneNumber);
        FirebaseApp firebaseApp = FirebaseApp.initializeApp(reactContext);
        final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(firebaseApp);

        PhoneAuthProvider.getInstance(firebaseAuth).verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                getCurrentActivity(),
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        Log.i(TAG, "onVerificationCompleted");
                        Log.i(TAG, "Credential >> " + credential.getSmsCode() + " >> " + credential.getProvider());
                        sendEvent(ON_VERIFICATION_COMPLETED, credential.getSmsCode());
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.e(TAG, "onVerificationFailed", e);
//                        sendEvent(ON_VERIFICATION_FAILED, e);
                        promise.reject("firebase_auth_phone_verification_failed", e.getMessage());
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verificationId, forceResendingToken);
                        Log.i(TAG, "onCodeSent");
//                        sendEvent(ON_CODE_SENT, verificationId);
                        promise.resolve(verificationId);
                    }

                    @Override
                    public void onCodeAutoRetrievalTimeOut(String verificationId) {
                        super.onCodeAutoRetrievalTimeOut(verificationId);
                        Log.i(TAG, "onCodeAutoRetrievalTimeOut >>> " + verificationId);
                        sendEvent(ON_CODE_AUTO_RETRIEVAL_TIMEOUT, verificationId);
                    }
                });
    }
}
