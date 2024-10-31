/*
 * Copyright 2022 The Android Open Source Project
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

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import android.accounts.Account;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RequestPullPhoneBookTest {

    private static final String PB_NAME = "phonebook";
    private static final Account ACCOUNT = mock(Account.class);

    @Test
    public void constructor_wrongMaxListCount_throwsIAE() {
        final long filter = 0;
        final byte format = PbapPhonebook.FORMAT_VCARD_30;
        final int listStartOffset = 10;

        final int wrongMaxListCount = -1;

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RequestPullPhoneBook(
                                PB_NAME,
                                filter,
                                format,
                                wrongMaxListCount,
                                listStartOffset,
                                ACCOUNT));
    }

    @Test
    public void constructor_wrongListStartOffset_throwsIAE() {
        final long filter = 0;
        final byte format = PbapPhonebook.FORMAT_VCARD_30;
        final int maxListCount = 100;

        final int wrongListStartOffset = -1;

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RequestPullPhoneBook(
                                PB_NAME,
                                filter,
                                format,
                                maxListCount,
                                wrongListStartOffset,
                                ACCOUNT));
    }

    @Test
    public void readResponse_failWithInputStreamThatThrowsIOEWhenRead() {
        final long filter = 1;
        final byte format = 0; // Will be properly handled as VCARD_TYPE_21.
        final int maxListCount = 0; // Will be specially handled as 65535.
        final int listStartOffset = 10;
        RequestPullPhoneBook request =
                new RequestPullPhoneBook(
                        PB_NAME, filter, format, maxListCount, listStartOffset, ACCOUNT);

        final InputStream is =
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException();
                    }

                    @Override
                    public int read(byte[] b) throws IOException {
                        throw new IOException();
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        throw new IOException();
                    }
                };

        assertThrows(IOException.class, () -> request.readResponse(is));
    }
}
