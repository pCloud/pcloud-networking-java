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

package com.pcloud.networking;

import com.pcloud.protocol.DataSource;

import java.io.IOException;

public interface FileOperationApi {

    @Method("downloadfile")
    FileResponse downloadRemoteUrl(@Parameter("url") String url, @Parameter("folderid") long folderId) throws IOException;

    @Method("getthumb")
    DataApiResponse getThumb(@Parameter("path") String filePath, @Parameter("size") String size) throws IOException;

    @Method("uploadfile")
    FileResponse uploadFile(@Parameter("folderid") int folderId, @Parameter("filename") String fileName, @RequestData DataSource dataSource) throws IOException;
}
