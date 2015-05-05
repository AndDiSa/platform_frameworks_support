/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.v4.hardware.fingerprint;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.CancellationSignal;

import java.security.Signature;

import javax.crypto.Cipher;

/**
 * A class that coordinates access to the fingerprint hardware.
 * <p>
 * On platforms before MNC, this class behaves as there would be no fingerprint hardware available.
 */
public class FingerprintManagerCompat {

    private Context mContext;

    /** Get a {@link FingerprintManagerCompat} instance for a provided context. */
    public static FingerprintManagerCompat from(Context context) {
        return new FingerprintManagerCompat(context);
    }

    private FingerprintManagerCompat(Context context) {
        mContext = context;
    }

    static final FingerprintManagerCompatImpl IMPL;
    static {
        final int version = Build.VERSION.SDK_INT;
        // STOPSHIP: Remove "MNC" check once the API's are final for MNC
        if (version >= 23 || "MNC".equals(Build.VERSION.CODENAME)) {
            IMPL = new Api23FingerprintManagerCompatImpl();
        } else {
            IMPL = new LegacyFingerprintManagerCompatImpl();
        }
    }

    /**
     * Determine if there is at least one fingerprint enrolled.
     *
     * @return true if at least one fingerprint is enrolled, false otherwise
     */
    public boolean hasEnrolledFingerprints() {
        return IMPL.hasEnrolledFingerprints(mContext);
    }

    /**
     * Determine if fingerprint hardware is present and functional.
     *
     * @return true if hardware is present and functional, false otherwise.
     */
    public boolean isHardwareDetected() {
        return IMPL.isHardwareDetected(mContext);
    }

    /**
     * Request authentication of a crypto object. This call warms up the fingerprint hardware
     * and starts scanning for a fingerprint. It terminates when
     * {@link AuthenticationCallback#onAuthenticationError(int, CharSequence)} or
     * {@link AuthenticationCallback#onAuthenticationSucceeded(AuthenticationResult) is called, at
     * which point the object is no longer valid. The operation can be canceled by using the
     * provided cancel object.
     *
     * @param crypto object associated with the call or null if none required.
     * @param cancel an object that can be used to cancel authentication
     * @param callback an object to receive authentication events
     * @param flags optional flags; should be 0
     */
    public void authenticate(@Nullable CryptoObject crypto,
            @Nullable CancellationSignal cancel, @NonNull AuthenticationCallback callback,
            int flags) {
        IMPL.authenticate(mContext, crypto, cancel, callback, flags);
    }

    /**
     * A wrapper class for the crypto objects supported by FingerprintManager. Currently the
     * framework supports {@link Signature} and {@link Cipher} objects.
     */
    public static class CryptoObject {

        private final Signature mSignature;
        private final Cipher mCipher;

        public CryptoObject(Signature signature) {
            mSignature = signature;
            mCipher = null;
        }

        public CryptoObject(Cipher cipher) {
            mCipher = cipher;
            mSignature = null;
        }

        /**
         * Get {@link Signature} object.
         * @return {@link Signature} object or null if this doesn't contain one.
         */
        public Signature getSignature() { return mSignature; }

        /**
         * Get {@link Cipher} object.
         * @return {@link Cipher} object or null if this doesn't contain one.
         */
        public Cipher getCipher() { return mCipher; }
    }

    /**
     * Container for callback data from {@link FingerprintManagerCompat#authenticate(CryptoObject,
     *     CancellationSignal, AuthenticationCallback, int)}.
     */
    public static final class AuthenticationResult {
        private CryptoObject mCryptoObject;

        public AuthenticationResult(CryptoObject crypto) {
            mCryptoObject = crypto;
        }

        /**
         * Obtain the crypto object associated with this transaction
         * @return crypto object provided to {@link FingerprintManagerCompat#authenticate(
         *         CryptoObject, CancellationSignal, AuthenticationCallback, int)}.
         */
        public CryptoObject getCryptoObject() { return mCryptoObject; }
    }

    /**
     * Callback structure provided to {@link FingerprintManagerCompat#authenticate(CryptoObject,
     * CancellationSignal, AuthenticationCallback, int)}. Users of {@link
     * FingerprintManagerCompat#authenticate(CryptoObject, CancellationSignal,
     * AuthenticationCallback, int) } must provide an implementation of this for listening to
     * fingerprint events.
     */
    public static abstract class AuthenticationCallback {
        /**
         * Called when an unrecoverable error has been encountered and the operation is complete.
         * No further callbacks will be made on this object.
         * @param errMsgId An integer identifying the error message
         * @param errString A human-readable error string that can be shown in UI
         */
        public void onAuthenticationError(int errMsgId, CharSequence errString) { }

        /**
         * Called when a recoverable error has been encountered during authentication. The help
         * string is provided to give the user guidance for what went wrong, such as
         * "Sensor dirty, please clean it."
         * @param helpMsgId An integer identifying the error message
         * @param helpString A human-readable string that can be shown in UI
         */
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) { }

        /**
         * Called when a fingerprint is recognized.
         * @param result An object containing authentication-related data
         */
        public void onAuthenticationSucceeded(AuthenticationResult result) { }

        /**
         * Called when a fingerprint is valid but not recognized.
         */
        public void onAuthenticationFailed() { }
    }

    private interface FingerprintManagerCompatImpl {
        boolean hasEnrolledFingerprints(Context context);
        boolean isHardwareDetected(Context context);
        void authenticate(Context context, CryptoObject crypto, CancellationSignal cancel,
                AuthenticationCallback callback, int flags);
    }

    private static class LegacyFingerprintManagerCompatImpl
            implements FingerprintManagerCompatImpl {

        public LegacyFingerprintManagerCompatImpl() {
        }

        @Override
        public boolean hasEnrolledFingerprints(Context context) {
            return false;
        }

        @Override
        public boolean isHardwareDetected(Context context) {
            return false;
        }

        @Override
        public void authenticate(Context context, CryptoObject crypto, CancellationSignal cancel,
                AuthenticationCallback callback, int flags) {
            // TODO: Figure out behavior when there is no fingerprint hardware available
        }
    }

    private static class Api23FingerprintManagerCompatImpl implements FingerprintManagerCompatImpl {

        public Api23FingerprintManagerCompatImpl() {
        }

        @Override
        public boolean hasEnrolledFingerprints(Context context) {
            return FingerprintManagerCompatApi23.hasEnrolledFingerprints(context);
        }

        @Override
        public boolean isHardwareDetected(Context context) {
            return FingerprintManagerCompatApi23.isHardwareDetected(context);
        }

        @Override
        public void authenticate(Context context, CryptoObject crypto, CancellationSignal cancel,
                AuthenticationCallback callback, int flags) {
            FingerprintManagerCompatApi23.authenticate(context, wrapCryptoObject(crypto),
                    cancel != null ? cancel.getCancellationSignalObject() : null,
                    wrapCallback(callback), flags);
        }

        private static FingerprintManagerCompatApi23.CryptoObject wrapCryptoObject(
                CryptoObject cryptoObject) {
            if (cryptoObject.getCipher() != null) {
                return new FingerprintManagerCompatApi23.CryptoObject(cryptoObject.getCipher());
            } else {
                return new FingerprintManagerCompatApi23.CryptoObject(cryptoObject.getSignature());
            }
        }

        private static CryptoObject unwrapCryptoObject(
                FingerprintManagerCompatApi23.CryptoObject cryptoObject) {
            if (cryptoObject.getCipher() != null) {
                return new CryptoObject(cryptoObject.getCipher());
            } else {
                return new CryptoObject(cryptoObject.getSignature());
            }
        }

        private static FingerprintManagerCompatApi23.AuthenticationCallback wrapCallback(
                final AuthenticationCallback callback) {
            return new FingerprintManagerCompatApi23.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    callback.onAuthenticationError(errMsgId, errString);
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                    callback.onAuthenticationHelp(helpMsgId, helpString);
                }

                @Override
                public void onAuthenticationSucceeded(
                        FingerprintManagerCompatApi23.AuthenticationResultInternal result) {
                    callback.onAuthenticationSucceeded(new AuthenticationResult(
                            unwrapCryptoObject(result.getCryptoObject())));
                }

                @Override
                public void onAuthenticationFailed() {
                    callback.onAuthenticationFailed();
                }
            };
        }
    }
}
