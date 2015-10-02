/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.simpligility.android.databinding.library;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

public class NameModel extends BaseObservable {
    private final String TAG = NameModel.class.getName();
    private String mFirstName = "";
    private String mLastName = "";

    @Bindable
    public String getFirstName() {
        return mFirstName;
    }

    public void setFirstName(String firstName) {
        mFirstName = firstName;
        notifyPropertyChanged(com.simpligility.android.databinding.library.BR.firstName);
    }

    @Bindable
    public String getLastName() {
        return mLastName;
    }

    public void setLastName(String lastName) {
        this.mLastName = lastName;
        notifyPropertyChanged(com.simpligility.android.databinding.library.BR.lastName);
    }
}
