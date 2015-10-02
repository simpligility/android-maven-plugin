package com.simpligility.android.databinding;

import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simpligility.android.databinding.R;

//import com.simpligility.android.databinding.databinding.FragmentMainBinding;

public class MainActivityFragment extends Fragment {

    private PhonewordViewModel mPhonewordViewModel;
//    private FragmentMainBinding mBinding;

    public MainActivityFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mPhonewordViewModel = new PhonewordViewModel();
        return  null;
//        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
//        mBinding.setPhonewordVM(mPhonewordViewModel);
//        View v = mBinding.getRoot();

//        mBinding.callButton.setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        final Intent callIntent = new Intent(Intent.ACTION_CALL);
//                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
//                        alertDialogBuilder
//                                .setMessage(mBinding.callButton.getText())
//                                .setNeutralButton(R.string.call_button_text, new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        callIntent.setData(Uri.parse("tel:" + mPhonewordViewModel.getPhoneNumber()));
//                                        PhonewordUtils.savePhoneword(getActivity(), mPhonewordViewModel.getPhoneWord());
//                                        startActivity(callIntent);
//                                    }
//                                })
//                                .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        // Nothing to do here.
//                                    }
//                                })
//                                .show();
//                    }
//                }
//        );


//        mBinding.translateButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mPhonewordViewModel.setPhoneWord(mBinding.phonewordText.getText().toString());
//                mPhonewordViewModel.translatePhoneWord();
//            }
//        });

//        return v;
    }
}
