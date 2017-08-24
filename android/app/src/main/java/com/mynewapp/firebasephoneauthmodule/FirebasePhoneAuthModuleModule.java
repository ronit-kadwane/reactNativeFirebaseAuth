//  Created by react-native-create-bridge

package com.mynewapp.firebasephoneauthmodule;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FirebasePhoneAuthModuleModule extends ReactContextBaseJavaModule {
    private static final String TAG = "JBFirebase";

    public static final String REACT_CLASS = "FirebasePhoneAuthModule";
    private static ReactApplicationContext reactContext = null;
    public static final String ON_VERIFICATION_COMPLETED = "onFirebasePhoneVerificationCompleted";
    public static final String ON_CODE_AUTO_RETRIEVAL_TIMEOUT = "onFirebasePhoneCodeAutoRetrievalTimeOut";

    private FirebaseAuth firebaseAuth;
    private PhoneAuthProvider.ForceResendingToken phoneAuthResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks phoneAuthProviderCallbacks;
    private String firebaseVerificationId;

    public FirebasePhoneAuthModuleModule(ReactApplicationContext context) {
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
    public void exampleMethod(String name, Callback callback) {

        String greeting = "Welcome >> " + name;
        callback.invoke(greeting);
        // An example native method that you will expose to React
        // https://facebook.github.io/react-native/docs/native-modules-android.html#the-toast-module
    }

    @ReactMethod
    public void verifyPhoneNumber(String phoneNumber, Promise promise) {
        Log.i(TAG, "INSIDE verifyPhoneNumber");
        Log.i(TAG, "phoneNumber >> " + phoneNumber);
        firebaseAuth = FirebaseAuth.getInstance();

        PhoneAuthProvider.getInstance(firebaseAuth).verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                getCurrentActivity(),
                verificationCallback(promise));
    }

    @ReactMethod
    public void resendVerificationCode(String phoneNumber, Promise promise){
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                getCurrentActivity(),
                verificationCallback(promise),
                phoneAuthResendToken);
    }

    private  PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationCallback(final Promise promise) {
        phoneAuthProviderCallbacks =  new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.i(TAG, "onVerificationCompleted");
                firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.i(TAG, "signInWithCredential:success");
                                    FirebaseUser user = task.getResult().getUser();
                                    Log.i(TAG, "Phone Number >> "+ user.getPhoneNumber());
                                    Log.i(TAG, "UID >> "+ user.getUid());
                                    Log.i(TAG, "ProviderId >> "+ user.getProviderId());
                                    JSONObject object = new JSONObject();
                                    try {
                                        object.put("phoneNumber", user.getPhoneNumber());
                                        object.put("providerId", user.getProviderId());
                                        object.put("uid", user.getUid());
                                        promise.resolve(object.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                        Log.w(TAG, "Invalid code.");
                                        promise.resolve("Invalid code.");
                                    }
                                    promise.reject("signInWithCredential:failure \n", task.getException());
                                }
                            }
                        });
                sendEvent(ON_VERIFICATION_COMPLETED, credential.getSmsCode());
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.e(TAG, "onVerificationFailed" + e.getMessage());
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    promise.reject("firebase_auth_phone_verification_failed", "Invalid phone number.");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    promise.reject("firebase_auth_phone_verification_failed", "Quota exceeded.");
                } else {
                    promise.reject("firebase_auth_phone_verification_failed", e.getMessage());
                }

            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(verificationId, forceResendingToken);
                Log.i(TAG, "onCodeSent");
//                        sendEvent(ON_CODE_SENT, verificationId);
                firebaseVerificationId = verificationId;
                phoneAuthResendToken = forceResendingToken;
                promise.resolve(verificationId);
            }

            @Override
            public void onCodeAutoRetrievalTimeOut(String verificationId) {
                super.onCodeAutoRetrievalTimeOut(verificationId);
                Log.i(TAG, "onCodeAutoRetrievalTimeOut >>> " + verificationId);
                sendEvent(ON_CODE_AUTO_RETRIEVAL_TIMEOUT, verificationId);
            }
        };
        return phoneAuthProviderCallbacks;
    }

    @ReactMethod
    public void verifyPhoneNumberWithCode(String code, final Promise promise) {
        Log.i(TAG, "verifyPhoneNumberWithCode:  verificationId >> "+ firebaseVerificationId+ " > CODE >> "+ code);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(firebaseVerificationId, code);
        Log.i(TAG, "signInWithPhoneAuthCredential: credential >> "+credential);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            Log.i(TAG, "Phone Number >> "+ user.getPhoneNumber());
                            Log.i(TAG, "UID >> "+ user.getUid());
                            Log.i(TAG, "ProviderId >> "+ user.getProviderId());
                            JSONObject object = new JSONObject();
                            try {
                                object.put("phoneNumber", user.getPhoneNumber());
                                object.put("providerId", user.getProviderId());
                                object.put("uid", user.getUid());
                                promise.resolve(object.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Log.w(TAG, "Invalid code.");
                                promise.resolve("Invalid code.");
                            }
                            promise.reject("signInWithCredential:failure \n", task.getException());
                        }
                    }
                });
    }


    @ReactMethod
    public void signOut(final Promise promise) {
        Log.i(TAG, "signOut > "+ firebaseAuth.getCurrentUser().getPhoneNumber());
        firebaseAuth.signOut();
        promise.resolve("SignOut Successfully.");
    }
}
