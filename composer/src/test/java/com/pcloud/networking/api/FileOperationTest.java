/*
 * Copyright (c) 2017 pCloud AG
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

package com.pcloud.networking.api;

import com.pcloud.networking.protocol.DataSource;
import okio.Buffer;
import okio.ByteString;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class FileOperationTest extends ApiIntegrationTest {

    private static final String TEST_IMAGE_FILE_NAME = "Nyan_cat_250px_frame.PNG";
    private static final String REMOTE_IMAGE_URL = "https://filedn.com/lmAEeoTE4jMmdP3fKVL5swu/pcloud-networking/" + TEST_IMAGE_FILE_NAME;

    @Test
    public void downloadFile_ShouldNotFail() throws Exception {

        FileResponse response = downloadApi.downloadRemoteUrl(REMOTE_IMAGE_URL, 0L);

        assertNotNull(response);
        assertEquals(0, response.getMetadata().get(0).getFolderid());
        assertEquals(TEST_IMAGE_FILE_NAME, response.getMetadata().get(0).getName());
    }

    @Test
    public void downloadFile_ShouldNotFailOnWrongFolderId() throws Exception {
        FileResponse response = downloadApi.downloadRemoteUrl(REMOTE_IMAGE_URL, -1);
        assertNotNull(response);
        assertEquals(0L, response.resultCode());
    }

    @Test
    public void downloadFile_ShouldNotFailOnEmptyUrl() throws Exception {
        FileResponse response = downloadApi.downloadRemoteUrl("", 0);
        assertNotNull(response);
        assertEquals(0, response.resultCode());
    }

    @Test
    public void downloadFile_ShouldNotFailOnNullUrl() throws Exception {
        downloadApi.downloadRemoteUrl(null, 0);
    }

    @Test
    public void downloadThumb_ShouldNotFailOnCorrectParameters() throws Exception {
        DataApiResponse response = downloadApi.getThumb("/" + TEST_IMAGE_FILE_NAME, "64x64");
        assertNotNull(response);
        assertEquals(response.message(), response.resultCode(), 0L);
        assertNotNull(response.responseData());

        Buffer buffer = new Buffer();
        response.responseData().writeTo(buffer);
        assertEquals(response.responseData().contentLength(), buffer.size());
    }

    @Test
    public void downloadThumb_ShouldNotFailOnWrongFileName() throws Exception {
        DataApiResponse response = downloadApi.getThumb("/abc", "64x64");
        assertNotNull(response);
        assertNull(response.responseData());
    }

    @Test
    public void downloadThumb_ShouldNotFailOnNullParameters() throws Exception {
        downloadApi.getThumb(null, "64x64");
        downloadApi.getThumb("", null);
    }

    @Test
    public void uploadFile_ShouldNotFail() throws Exception {
        ByteString string = ByteString.encodeString("abc123qwerty!@#$%^&*(", StandardCharsets.UTF_8);
        FileResponse response = downloadApi.uploadFile(0, "someFile.txt", DataSource.create(string));
        assertNotNull(response);
    }

    @Test
    public void uploadFile_ShouldFailOnNullDataSource() throws Exception {
        exception.expect(IllegalArgumentException.class);
        FileResponse response = downloadApi.uploadFile(0, "someFile.txt", null);
        assertNotNull(response);
    }

    @Test
    public void updateFile_ShouldFailOnNullDataSource() throws Exception {
        exception.expect(IllegalArgumentException.class);
        downloadApi.uploadFile(0, null, null);
    }
}
