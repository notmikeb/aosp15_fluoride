/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.bluetooth.pbapclient;

import android.accounts.Account;
import android.util.Log;

import com.android.bluetooth.ObexAppParameters;
import com.android.obex.HeaderSet;
import com.android.vcard.VCardEntry;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

final class RequestPullPhoneBook extends PbapClientRequest {
    private static final String TAG = RequestPullPhoneBook.class.getSimpleName();

    private static final String TYPE = "x-bt/phonebook";

    private final String mPhonebook;
    private final byte mFormat;
    private final int mMaxListCount;
    private final int mListStartOffset;
    private Account mAccount;

    private PbapPhonebook mResponse;

    RequestPullPhoneBook(
            String phonebook,
            long propertySelector,
            byte format,
            int maxListCount,
            int listStartOffset,
            Account account) {

        if (format != PbapPhonebook.FORMAT_VCARD_21 && format != PbapPhonebook.FORMAT_VCARD_30) {
            throw new IllegalArgumentException("Format should be v2.1 or v3.0");
        }

        if (maxListCount < 0 || maxListCount > 65535) {
            throw new IllegalArgumentException("maxListCount should be [0..65535]");
        }

        if (listStartOffset < 0 || listStartOffset > 65535) {
            throw new IllegalArgumentException("listStartOffset should be [0..65535]");
        }

        mPhonebook = phonebook;
        mFormat = format;
        mMaxListCount = maxListCount;
        mListStartOffset = listStartOffset;
        mAccount = account;

        mHeaderSet.setHeader(HeaderSet.NAME, phonebook);
        mHeaderSet.setHeader(HeaderSet.TYPE, TYPE);

        ObexAppParameters oap = new ObexAppParameters();

        oap.add(OAP_TAGID_FORMAT, format);

        if (propertySelector != 0) {
            oap.add(OAP_TAGID_PROPERTY_SELECTOR, propertySelector);
        }

        if (listStartOffset > 0) {
            oap.add(OAP_TAGID_LIST_START_OFFSET, (short) listStartOffset);
        }

        // maxListCount == 0 indicates to fetch all, in which case we set it to the upper bound
        // Note that Java has no unsigned types. To capture an unsigned value in the range [0, 2^16)
        // we need to use an int and cast to a short (2 bytes). This packs the bits we want.
        if (mMaxListCount > 0) {
            oap.add(OAP_TAGID_MAX_LIST_COUNT, (short) mMaxListCount);
        } else {
            oap.add(OAP_TAGID_MAX_LIST_COUNT, (short) 65535);
        }

        oap.addToHeaderSet(mHeaderSet);
    }

    @Override
    protected void readResponse(InputStream stream) throws IOException {
        Log.v(TAG, "readResponse");

        mResponse = new PbapPhonebook(mPhonebook, mFormat, mListStartOffset, mAccount, stream);
        Log.d(TAG, "Read " + mResponse.getCount() + " entries");
    }

    public String getPhonebook() {
        return mPhonebook;
    }

    public List<VCardEntry> getList() {
        return mResponse.getList();
    }

    public PbapPhonebook getContacts() {
        return mResponse;
    }
}
