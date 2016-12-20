package com.firebase.ui.auth.ui.email;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class User implements Parcelable {
    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in.readString(),
                            in.readString(),
                            in.readString(),
                            in.<Uri>readParcelable(Uri.class.getClassLoader()));
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    private String mEmail;
    private String mName;
    private String mProvider;
    private Uri mProfilePicUri;

    private User(String email, String name, String provider, Uri profilePicUri) {
        mEmail = email;
        mName = name;
        mProvider = provider;
        mProfilePicUri = profilePicUri;
    }

    @NonNull
    public String getEmail() {
        return mEmail;
    }

    @Nullable
    public String getName() {
        return mName;
    }

    @Nullable
    public String getProvider() {
        return mProvider;
    }

    @Nullable
    public Uri getProfilePicUri() {
        return mProfilePicUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mEmail);
        dest.writeString(mName);
        dest.writeString(mProvider);
        dest.writeParcelable(mProfilePicUri, flags);
    }

    public static final class Builder {
        private String mEmail;
        private String mName;
        private String mProvider;
        private Uri mProfilePicUri;

        public Builder(@NonNull String email) {
            mEmail = email;
        }

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setProvider(String provider) {
            mProvider = provider;
            return this;
        }

        public Builder setProfilePicUri(Uri profilePicUri) {
            mProfilePicUri = profilePicUri;
            return this;
        }

        public User build() {
            return new User(mEmail, mName, mProvider, mProfilePicUri);
        }
    }
}
