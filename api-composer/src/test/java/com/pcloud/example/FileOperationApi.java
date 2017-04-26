package com.pcloud.example;

import com.pcloud.networking.DataApiResponse;
import com.pcloud.networking.Method;
import com.pcloud.networking.Parameter;
import com.pcloud.networking.RequestData;
import com.pcloud.protocol.DataSource;

import java.io.IOException;

/**
 * Created by Georgi on 4/21/2017.
 */
public interface FileOperationApi {

    @Method("downloadfile")
    FileResponse downloadRemoteUrl(@Parameter("url") String url, @Parameter("folderid") long folderId) throws IOException;

    @Method("getthumb")
    DataApiResponse getThumb(@Parameter("path") String filePath, @Parameter("size") String size) throws IOException;

    @Method("uploadfile")
    FileResponse uploadFile(@Parameter("folderid") int folderId, @Parameter("filename") String fileName, @RequestData DataSource dataSource) throws IOException;
}
