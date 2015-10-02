package com.simpligility.android.databinding;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.TextUtils;
import android.view.View;

import static com.simpligility.android.databinding.PhonewordUtils.toNumber;

public class PhonewordViewModel extends BaseObservable {
    private final String TAG = PhonewordViewModel.class.getName();
    private boolean mIsTranslated = false;
    private String mPhoneNumber = "";
    private String mPhoneWord = "";
    private String mCallButtonText = "Call";

    @Bindable
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    @Bindable
    public String getCallButtonText() {
        return mCallButtonText;
    }

    @Bindable
    public boolean getIsTranslated() {
        return mIsTranslated;
    }

    @Bindable
    public String getPhoneWord() {
        return mPhoneWord;
    }


    public void setPhoneWord(String phoneWord) {
        mPhoneWord = phoneWord;
        notifyPropertyChanged(com.simpligility.android.databinding.BR.phoneWord);
    }

    public void translatePhoneWord() {
        mPhoneNumber = toNumber(mPhoneWord);

        if (TextUtils.isEmpty(mPhoneNumber)) {
            mCallButtonText = "Call";
            mIsTranslated = false;
        } else {
            mIsTranslated = true;
            mCallButtonText = "Call " + mPhoneNumber + "?";
        }
        notifyPropertyChanged(com.simpligility.android.databinding.BR.phoneNumber);
        notifyPropertyChanged(com.simpligility.android.databinding.BR.isTranslated);
        notifyPropertyChanged(com.simpligility.android.databinding.BR.callButtonText);
    }

    public void onTranslate(View v) {
        translatePhoneWord();
    }
}
