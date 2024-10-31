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

import com.android.bluetooth.ObexAppParameters;
import com.android.obex.HeaderSet;

final class RequestPullPhoneBookSize extends PbapClientRequest {
    private static final String TAG = RequestPullPhoneBookSize.class.getSimpleName();

    private static final String TYPE = "x-bt/phonebook";

    private int mSize = -1;

    RequestPullPhoneBookSize(String phonebook, PbapApplicationParameters params) {
        mHeaderSet.setHeader(HeaderSet.NAME, phonebook);
        mHeaderSet.setHeader(HeaderSet.TYPE, TYPE);

        // Set MaxListCount in the request to 0 to get PhonebookSize in the response.
        // If a vCardSelector is present in the request, then the result shall
        // contain the number of items that satisfy the selector’s criteria.
        // See PBAP v1.2.3, Sec. 5.1.4.5.
        ObexAppParameters oap = new ObexAppParameters();
        oap.add(PbapApplicationParameters.OAP_MAX_LIST_COUNT, (short) 0);

        // Otherwise, listen to the property selector criteria passed in and ignore the rest
        long properties = params.getPropertySelectorMask();
        if (properties != PbapApplicationParameters.PROPERTIES_ALL) {
            oap.add(PbapApplicationParameters.OAP_PROPERTY_SELECTOR, properties);
        }
        oap.addToHeaderSet(mHeaderSet);
    }

    @Override
    protected void readResponseHeaders(HeaderSet headerset) {
        ObexAppParameters oap = ObexAppParameters.fromHeaderSet(headerset);
        if (oap.exists(PbapApplicationParameters.OAP_PHONEBOOK_SIZE)) {
            mSize = oap.getShort(PbapApplicationParameters.OAP_PHONEBOOK_SIZE);
        }
    }

    public int getSize() {
        return mSize;
    }
}
