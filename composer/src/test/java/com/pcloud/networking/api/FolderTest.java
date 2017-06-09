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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FolderTest extends ApiIntegrationTest {

    @Test
    public void listFolder_ShouldNotFailOnCorrectParameters() throws Exception {

        FolderResponse response = folderApi.listFolder(0);
        assertNotNull(response);
        assertNotNull(response.getMetadata());
        assertEquals(0, response.getMetadata().getFolderid());
    }

    @Test
    public void listFolder_ShouldNotFailOnWrongFolderId() throws Exception {

        FolderResponse response = folderApi.listFolder(-1);
        assertNotNull(response);
        assertNull(response.getMetadata());
    }
}
