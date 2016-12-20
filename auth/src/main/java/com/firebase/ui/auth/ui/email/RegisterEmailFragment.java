package com.firebase.ui.auth.ui.email;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.ui.BaseFragment;
import com.firebase.ui.auth.ui.ExtraConstants;
import com.firebase.ui.auth.ui.FlowParameters;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.firebase.ui.auth.ui.email.fieldvalidators.EmailFieldValidator;
import com.firebase.ui.auth.ui.email.fieldvalidators.PasswordFieldValidator;
import com.firebase.ui.auth.ui.email.fieldvalidators.RequiredFieldValidator;
import com.firebase.ui.auth.util.signincontainer.SaveSmartLock;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Fragment to display an email/name/password sign up form for new users.
 */
public class RegisterEmailFragment extends BaseFragment implements
        View.OnClickListener, View.OnFocusChangeListener {

    public static final String TAG = "RegisterEmailFragment";

    private EditText mEmailEditText;
    private EditText mNameEditText;
    private EditText mPasswordEditText;
    private TextView mAgreementText;
    private TextInputLayout mEmailInput;
    private TextInputLayout mPasswordInput;

    private EmailFieldValidator mEmailFieldValidator;
    private PasswordFieldValidator mPasswordFieldValidator;
    private RequiredFieldValidator mNameValidator;
    private SaveSmartLock mSaveSmartLock;

    public static RegisterEmailFragment getInstance(FlowParameters flowParameters,
                                                    @Nullable String email,
                                                    @Nullable String name,
                                                    @Nullable Uri profilePicUrl) {
        RegisterEmailFragment fragment = new RegisterEmailFragment();

        Bundle args = new Bundle();
        args.putParcelable(ExtraConstants.EXTRA_FLOW_PARAMS, flowParameters);
        args.putString(ExtraConstants.EXTRA_EMAIL, email);
        args.putString(ExtraConstants.EXTRA_NAME, name);
        args.putParcelable(ExtraConstants.EXTRA_PROFILE_PIC_URL, profilePicUrl);

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.register_email_layout, container, false);

        mPasswordFieldValidator = new PasswordFieldValidator(
                (TextInputLayout) v.findViewById(R.id.password_layout),
                getResources().getInteger(R.integer.min_password_length));
        mNameValidator = new RequiredFieldValidator(
                (TextInputLayout) v.findViewById(R.id.name_layout));
        mEmailFieldValidator = new EmailFieldValidator(
                (TextInputLayout) v.findViewById(R.id.email_layout));

        mEmailEditText = (EditText) v.findViewById(R.id.email);
        mNameEditText = (EditText) v.findViewById(R.id.name);
        mPasswordEditText = (EditText) v.findViewById(R.id.password);
        mAgreementText = (TextView) v.findViewById(R.id.create_account_text);
        mEmailInput = (TextInputLayout) v.findViewById(R.id.email_layout);
        mPasswordInput = (TextInputLayout) v.findViewById(R.id.password_layout);

        mEmailEditText.setOnFocusChangeListener(this);
        mNameEditText.setOnFocusChangeListener(this);
        mPasswordEditText.setOnFocusChangeListener(this);
        v.findViewById(R.id.button_create).setOnClickListener(this);

        // If email is passed in, fill in the field and move down to the name field.
        String email = getArguments().getString(ExtraConstants.EXTRA_EMAIL);
        if (!TextUtils.isEmpty(email)) {
            mEmailEditText.setText(email);
            mNameEditText.requestFocus();
        }

        // If name is passed in, fill in the field and move down to the password field.
        String name = getArguments().getString(ExtraConstants.EXTRA_NAME);
        if (!TextUtils.isEmpty(name)) {
            mNameEditText.setText(name);
            mPasswordEditText.requestFocus();
        }

        return v;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set title
        if (getActivity().getActionBar() != null) {
            getActivity().getActionBar().setTitle(R.string.title_register_email_activity);
        }

        mSaveSmartLock = mHelper.getSaveSmartLockInstance(getActivity());
        setUpTermsOfService();
    }

    private void setUpTermsOfService() {
        if (mHelper.getFlowParams().termsOfServiceUrl == null) {
            return;
        }
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(
                ContextCompat.getColor(getActivity().getApplicationContext(), R.color.linkColor));

        String preamble = getResources().getString(R.string.create_account_preamble);
        String link = getResources().getString(R.string.terms_of_service);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(preamble + link);
        int start = preamble.length();
        spannableStringBuilder.setSpan(foregroundColorSpan, start, start + link.length(), 0);

        mAgreementText.setText(spannableStringBuilder);
        mAgreementText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Getting default color
                TypedValue typedValue = new TypedValue();
                getActivity().getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
                @ColorInt int color = typedValue.data;

                new CustomTabsIntent.Builder()
                        .setToolbarColor(color)
                        .build()
                        .launchUrl(
                                getActivity(),
                                Uri.parse(mHelper.getFlowParams().termsOfServiceUrl));
            }
        });
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) return; // Only consider fields losing focus

        int id = view.getId();
        if (id == R.id.email) {
            mEmailFieldValidator.validate(mEmailEditText.getText());
        } else if (id == R.id.name) {
            mNameValidator.validate(mNameEditText.getText());
        } else if (id == R.id.password) {
            mPasswordFieldValidator.validate(mPasswordEditText.getText());
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_create) {
            String email = mEmailEditText.getText().toString();
            String password = mPasswordEditText.getText().toString();
            String name = mNameEditText.getText().toString();

            boolean emailValid = mEmailFieldValidator.validate(email);
            boolean passwordValid = mPasswordFieldValidator.validate(password);
            boolean nameValid = mNameValidator.validate(name);
            if (emailValid && passwordValid && nameValid) {
                mHelper.showLoadingDialog(R.string.progress_dialog_signing_up);
                registerUser(email, name, password);
            }
        }
    }

    private void registerUser(final String email, final String name, final String password) {
        mHelper.getFirebaseAuth()
                .createUserWithEmailAndPassword(email, password)
                .addOnFailureListener(new TaskFailureLogger(TAG, "Error creating user"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // Set display name
                        UserProfileChangeRequest changeNameRequest =
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .setPhotoUri(getArguments().<Uri>getParcelable(
                                                ExtraConstants.EXTRA_PROFILE_PIC_URL))
                                        .build();

                        final FirebaseUser user = authResult.getUser();
                        user.updateProfile(changeNameRequest)
                                .addOnFailureListener(new TaskFailureLogger(
                                        TAG, "Error setting display name"))
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        // This executes even if the name change fails, since
                                        // the account creation succeeded and we want to save
                                        // the credential to SmartLock (if enabled).
                                        mHelper.saveCredentialsOrFinish(
                                                mSaveSmartLock,
                                                getActivity(),
                                                user,
                                                password,
                                                new IdpResponse(EmailAuthProvider.PROVIDER_ID,
                                                                email));
                                        // TODO temporary fix for #409
                                        user.reauthenticate(
                                                EmailAuthProvider.getCredential(email, password));
                                    }
                                });
                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mHelper.dismissDialog();

                        if (e instanceof FirebaseAuthWeakPasswordException) {
                            // Password too weak
                            mPasswordInput.setError(getString(R.string.error_weak_password));
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Email address is malformed
                            mEmailInput.setError(getString(R.string.invalid_email_address));
                        } else if (e instanceof FirebaseAuthUserCollisionException) {
                            // Collision with existing user email, it should be very hard for
                            // the user to even get to this error due to CheckEmailFragment.
                            mEmailInput.setError(getString(R.string.error_user_collision));
                        } else {
                            // General error message, this branch should not be invoked but
                            // covers future API changes
                            mEmailInput.setError(getString(R.string.email_account_creation_error));
                        }
                    }
                });
    }
}
