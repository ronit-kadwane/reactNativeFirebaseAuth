//  Created by react-native-create-bridge

package com.mynewapp.firebasephoneauthmodule;

import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
    private static final String TAG = "JBFirebasePhoneAuth";

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
        return REACT_CLASS;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("ON_VERIFICATION_COMPLETED", ON_VERIFICATION_COMPLETED);
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
        Log.i(TAG, "verifyPhoneNumber");
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
        Log.i(TAG, "resendVerificationCode");
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                getCurrentActivity(),
                verificationCallback(promise),
                phoneAuthResendToken);
    }

    @ReactMethod
    public void verifyPhoneNumberWithCode(String code, final Promise promise) {
        Log.i(TAG, "verifyPhoneNumberWithCode");
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(firebaseVerificationId, code);
        Log.i(TAG, "signInWithCode");
        signIn(credential, promise);
    }

    private  PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationCallback(final Promise promise) {
        phoneAuthProviderCallbacks =  new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.i(TAG, "onVerificationCompleted");
                Log.i(TAG, "signInWithPhoneAuth");
                signIn(credential, promise);
                sendEvent(ON_VERIFICATION_COMPLETED, credential.getSmsCode());
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.e(TAG, "onVerificationFailed" + e.getMessage());
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    promise.reject("101", "firebase_auth_phone_verification_failed : Invalid phone number.");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    promise.reject("102", "firebase_auth_phone_verification_failed : Quota exceeded.");
                } else {
                    promise.reject("103", e.getMessage());
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

    private void signIn(PhoneAuthCredential credential, final Promise promise){
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
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
                                promise.reject("201", "FirebaseAuthInvalidCredentialsException : Invalid code");
                            }
                            promise.reject("202", "signInWithCredential:failure "+task.getException());
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
