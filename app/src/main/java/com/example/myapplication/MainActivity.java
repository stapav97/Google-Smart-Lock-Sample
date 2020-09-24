package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private static Integer RC_SAVE = 10;
    private static Integer RC_READ = 11;
    private static Integer RC_HINT = 12;
    CredentialsClient mCredentialsClient;
    TextView nameTv, passwordTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCredentialsClient = Credentials.getClient(this);
        final EditText loginEdt = findViewById(R.id.login);
        final EditText passwordEdt = findViewById(R.id.password);
        nameTv = findViewById(R.id.nameTv);
        passwordTv = findViewById(R.id.passwordTv);
        Button okBtn = findViewById(R.id.okBtn);
        Button requestBtn = findViewById(R.id.requestBtn);
        requestBtn.setOnClickListener(v -> requestCredential());
        okBtn.setOnClickListener(v ->
                onOkClick(loginEdt.getText().toString(), passwordEdt.getText().toString()));
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void onOkClick(String name, String password) {
        Credential credential = new Credential.Builder(name)
                .setPassword(password)  // Important: only store passwords in this field.
                // Android autofill uses this value to complete
                // sign-in forms, so repurposing this field will
                // likely cause errors.
                .build();

        mCredentialsClient.save(credential).addOnCompleteListener(
                new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d("STAS Complete", "SAVE: OK");
                            showToast("Credentials saved");
                            return;
                        }
                        Exception e = task.getException();
                        if (e instanceof ResolvableApiException) {
                            // Try to resolve the save request. This will prompt the user if
                            // the credential is new.
                            ResolvableApiException rae = (ResolvableApiException) e;
                            try {
                                rae.startResolutionForResult(MainActivity.this, RC_SAVE);
                            } catch (IntentSender.SendIntentException exception) {
                                // Could not resolve the request
                                Log.e("STAS FAIL", "Failed to send resolution.", exception);
                                showToast("Save failed");
                            }
                        } else {
                            // Request has no resolution
                            showToast("Save failed");
                        }
                    }
                });
    }

    private void requestCredential() {
        CredentialRequest mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .build();

        mCredentialsClient.disableAutoSignIn();
        mCredentialsClient.request(mCredentialRequest).addOnCompleteListener(
                new OnCompleteListener<CredentialRequestResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<CredentialRequestResponse> task) {

                        if (task.isSuccessful()) {
                            // See "Handle successful credential requests"
                            onCredentialRetrieved(task.getResult().getCredential());
                            return;
                        }

                        Exception e = task.getException();
                        if (e instanceof ResolvableApiException) {
                            // This is most likely the case where the user has multiple saved
                            // credentials and needs to pick one. This requires showing UI to
                            // resolve the read request.
                            ResolvableApiException rae = (ResolvableApiException) e;
                            if (rae.getStatusCode() == 4) {
                                return;
                            }
                            Log.d("QWERTY  ", rae.getStatusCode() + " ");
                            resolveResult(rae, RC_READ);
                        } else if (e instanceof ApiException) {
                            // The user must create an account or sign in manually.
                            Log.e("READ REQUEST", "Unsuccessful credential request.", e);

                            ApiException ae = (ApiException) e;
                            int code = ae.getStatusCode();
                            // ...
                        }


                        // See "Handle unsuccessful and incomplete credential requests"
                        // ...
                    }

                });
    }

    private void resolveResult(ResolvableApiException rae, int requestCode) {
        try {
            rae.startResolutionForResult(MainActivity.this, requestCode);
//            mIsResolving = true;
        } catch (IntentSender.SendIntentException e) {
            Log.e("READ EXCEPTION ", "Failed to send resolution.", e);
//            hideProgress();
        }
    }

    private void onCredentialRetrieved(Credential credential) {
        String accountType = credential.getAccountType();
        if (accountType == null) {
            // Sign the user in with information from the Credential.
            nameTv.setText(credential.getId());
            passwordTv.setText(credential.getPassword());
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SAVE) {
            if (resultCode == RESULT_OK) {
                Log.d("onActivityResult", "SAVE: OK");
                Toast.makeText(this, "Credentials saved", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("onActivityResult", "SAVE: Canceled by user");
            }
        }

        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {
                Log.d("onActivityResult", "READ: OK");
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                onCredentialRetrieved(credential);
            } else {
                Log.e("onActivityResult", "READ: Canceled by user");
                Toast.makeText(this, "Canceled by user", Toast.LENGTH_SHORT).show();
            }
        }
    }

}