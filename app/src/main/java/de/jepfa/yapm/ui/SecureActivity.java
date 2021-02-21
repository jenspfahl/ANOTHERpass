package de.jepfa.yapm.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.SecretKey;

import de.jepfa.yapm.R;
import de.jepfa.yapm.model.Encrypted;
import de.jepfa.yapm.model.Key;
import de.jepfa.yapm.model.Password;
import de.jepfa.yapm.service.encrypt.SecretService;

public abstract class SecureActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkSecret();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);

        checkSecret();
    }

    protected abstract void refresh(boolean before);


    protected synchronized SecretKey getMasterSecretKey() {
        SecretService.Secret secret =  SecretChecker.getOrAskForSecret(this);
        if (secret.isDeclined()) {
            return null;
        }
        else {
            return secret.get();
        }
    }

    private synchronized void checkSecret() {
        SecretChecker.getOrAskForSecret(this);
    }


    /**
     * Helper class to check the user secret.
     */
    public static class SecretChecker {

        public static final String PREF_HASHED_PIN = "YAPM/pref:hpin";
        public static final String PREF_MASTER_PASSWORD = "YAPM/pref:mpwd";
        private static final String PREF_SALT = "YAPM/pref:application.salt";

        private static final long DELTA_DIALOG_OPENED = TimeUnit.SECONDS.toMillis(5);
        private static int MAX_PASSWD_ATTEMPTS = 3;

        private static volatile long secretDialogOpened;

        public static synchronized SecretService.Secret getOrAskForSecret(BaseActivity activity) {

            SecretService.Secret secret = activity.getApp().getSecretService().getSecret();

            if (secret.isLockedOrOutdated()) {
                // make all not readable by setting key as invalid
                secret.lock();
                // open user secret dialog
                openDialog(secret, activity);
            } else {
                secret.update();
            }

            return secret;

        }


        public static synchronized Key getSalt(BaseActivity activity) {
            Context context = activity.getApplicationContext();
            SharedPreferences defaultSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(context);
            String saltBase64 = defaultSharedPreferences
                    .getString(PREF_SALT, null);
            SecretService secretService = activity.getApp().getSecretService();
            Key salt;
            if (saltBase64 == null) {
                SharedPreferences.Editor editor = defaultSharedPreferences.edit();
                salt = secretService.generateKey(128);
                editor.putString(PREF_SALT, Base64.encodeToString(salt.getData(), Base64.DEFAULT));

                editor.commit();
            }
            else {
               salt = new Key(Base64.decode(saltBase64, 0));
            }

            return salt;
        }


        private static void openDialog(final SecretService.Secret secret, final BaseActivity activity) {

            if (isRecentlyOpened(secretDialogOpened)) {
                return;
            }
            secretDialogOpened = System.currentTimeMillis();

            if (activity instanceof SecureActivity) {
                ((SecureActivity)activity).refresh(true); // show all data as invalid
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            // TODO this should be an activity instead of a dialog !!

            final EditText input = new EditText(activity);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.requestFocus();

            final AlertDialog dialog = builder.setTitle(R.string.title_encryption_pin_required)
                    .setMessage(R.string.message_encrypt_pin_required)
                    .setView(input)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            secretDialogOpened = 0;
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false)
                    .create();

            input.setImeOptions(EditorInfo.IME_ACTION_DONE);
            input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    return true;
                }
            });

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {

                    final AtomicInteger failCounter = new AtomicInteger();
                    Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    buttonPositive.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            SecretService secretService = activity.getApp().getSecretService();
                            Password masterPin = toPassword(input.getText());
                            try {
                                if (masterPin.isEmpty()) {
                                    input.setError(activity.getString(R.string.error_field_required));
                                    return;
                                } else if (isPinStored(activity) &&
                                        !isPinValid(masterPin, activity, getSalt(activity))) {
                                    input.setError(activity.getString(R.string.wrong_pin));
                                    if (failCounter.incrementAndGet() < MAX_PASSWD_ATTEMPTS) {
                                        return; // try again
                                    }
                                } else {
                                    Password masterPassword;
                                    if (isPasspraseStored(activity)) {
                                        masterPassword = getStoredPassword(activity);
                                    }
                                    else {
                                        masterPassword = getPasswordFromUser(activity);
                                    }
                                    secretService.login(masterPin, masterPassword, getSalt(activity));
                                    if (activity instanceof SecureActivity) {
                                        ((SecureActivity)activity).refresh(false); // show correct encrypted data
                                    }
                                }
                            } finally {
                                masterPin.clear();
                            }

                            secretDialogOpened = 0;

                            dialog.dismiss();
                        }
                    });

                }
            });
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();

        }

        public static boolean isPasspraseStored(Activity activity) {
            SharedPreferences defaultSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(activity);
            return defaultSharedPreferences.getString(PREF_MASTER_PASSWORD, null) != null;
        }

        public static Password getStoredPassword(BaseActivity activity) {
            SharedPreferences defaultSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(activity);
            String storedPasswordBase64 =  defaultSharedPreferences.getString(PREF_MASTER_PASSWORD, null);

            SecretService secretService = activity.getApp().getSecretService();

            SecretKey androidSecretKey = secretService.getAndroidSecretKey(secretService.getALIAS_KEY_HPIN());
            Encrypted storedEncPassword = Encrypted.Companion.fromBase64String(storedPasswordBase64);

            return secretService.decryptPassword(androidSecretKey, storedEncPassword);
        }

        public static Password getPasswordFromUser(BaseActivity activity) {
            //TODO another dialog to input that
            return new Password(""); // mockup
        }

        public static boolean isPinStored(Activity activity) {
            SharedPreferences defaultSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(activity);
            return defaultSharedPreferences.getString(PREF_HASHED_PIN, null) != null;
        }

        public static boolean isPinValid(Password userPin, BaseActivity activity, Key salt) {

            SharedPreferences defaultSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(activity);
            String storedPinBase64 = defaultSharedPreferences
                    .getString(PREF_HASHED_PIN, null);

            SecretService secretService = activity.getApp().getSecretService();
            Key hashedPin = secretService.hashPassword(userPin, salt);

            SecretKey androidSecretKey = secretService.getAndroidSecretKey(secretService.getALIAS_KEY_HPIN());
            Encrypted storedEncPin = Encrypted.Companion.fromBase64String(storedPinBase64);
            Key storedPin = secretService.decryptKey(androidSecretKey, storedEncPin);

            return hashedPin.equals(storedPin);

        }

        private static boolean isRecentlyOpened(long secretDialogOpened) {
            long current = System.currentTimeMillis();

            return secretDialogOpened >= current - DELTA_DIALOG_OPENED;
        }

        private static Password toPassword(Editable editable) {
            if (editable == null) {
                return null;
            }
            int l = editable.length();
            char[] chararray = new char[l];
            editable.getChars(0, l, chararray, 0);
            return new Password(chararray);
        }
    }

}
