package com.simpligility.android.databinding;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PhonewordUtils {

    private static final String SETTINGS_SAVED_PHONEWORDS = "net.opgenorth.phoneword.saved";
    private static final String SETTINGS_PHONEWORD = "net.opgenorth.phoneword";

    private PhonewordUtils() {

    }

    /**
     * Translate the phoneword into a phone number and formats it.
     *
     * @param phoneNumber
     * @return
     */
    public static String formatStringAsPhoneNumber(@NonNull String phoneNumber) {
        String output;

        switch (phoneNumber.length()) {
            case 7:
                output = String.format("%s-%s", phoneNumber.substring(0, 3), phoneNumber.substring(3, 7));
                break;
            case 10:
                output = String.format("(%s) %s-%s", phoneNumber.substring(0, 3), phoneNumber.substring(3, 6), phoneNumber.substring(6, 10));
                break;
            case 11:
                output = String.format("%s (%s) %s-%s", phoneNumber.substring(0, 1), phoneNumber.substring(1, 4), phoneNumber.substring(4, 7), phoneNumber.substring(7, 11));
                break;
            case 12:
                output = String.format("+%s (%s) %s-%s", phoneNumber.substring(0, 2), phoneNumber.substring(2, 5), phoneNumber.substring(5, 8), phoneNumber.substring(8, 12));
                break;
            default:
                return null;
        }
        return output;
    }

    /**
     * Takes a "phoneword" and translate it to a phone number.
     *
     * @param phoneWord
     * @return
     */
    public static String toNumber(String phoneWord) {
        if (TextUtils.isEmpty(phoneWord)) {
            return "";
        } else {
            phoneWord = phoneWord.toUpperCase();
        }

        StringBuilder newNumber = new StringBuilder();

        for (int i = 0; i < phoneWord.length(); i++) {
            String c = phoneWord.substring(i, i + 1);

            if ("0123456789".contains(c)) {
                newNumber.append(c);
            } else {
                int result = translateToNumber(c);
                if (result > 0) {
                    newNumber.append(result);
                }
            }
        }
        return formatStringAsPhoneNumber(newNumber.toString());
    }

    private static int translateToNumber(CharSequence c) {
        if ("ABC".contains(c)) {
            return 2;
        } else if ("DEF".contains(c)) {
            return 3;
        } else if ("GHI".contains(c)) {
            return 4;
        } else if ("JKL".contains(c)) {
            return 5;
        } else if ("MNO".contains(c)) {
            return 6;
        } else if ("PQRS".contains(c)) {
            return 7;
        } else if ("TUV".contains(c)) {
            return 8;
        } else if ("WXYZ".contains(c)) {
            return 9;
        } else {
            return -1;
        }
    }

    /**
     * Persistss the phone word for later use.
     *
     * @param context
     * @param number
     */
    public static void savePhoneword(@NonNull Context context, @NonNull String number) {

        SharedPreferences settings = context.getSharedPreferences(SETTINGS_PHONEWORD, 0);
        Set<String> phonewords = settings.getStringSet(SETTINGS_SAVED_PHONEWORDS, new HashSet<String>());

        phonewords.add(number);
        settings.edit().putStringSet(SETTINGS_SAVED_PHONEWORDS, phonewords).apply();
    }

    /**
     * Gets a list of all the phonewords that were saved.
     *
     * @param context
     * @return
     */
    public static List<String> getPhonewords(@NonNull Context context) {
        SharedPreferences settings = context.getSharedPreferences(SETTINGS_PHONEWORD, 0);
        Set<String> phonewords = settings.getStringSet(SETTINGS_SAVED_PHONEWORDS, new HashSet<String>());
        return new ArrayList<String>(phonewords);
    }

}