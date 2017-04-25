package com.pcloud.example;

import com.pcloud.networking.Method;
import com.pcloud.networking.Parameter;

import java.io.IOException;

/**
 * Created by Georgi on 4/21/2017.
 */
public interface FolderApi {

    @Method("createfolder")
    FolderResponse createFolder(@Parameter("name") String name, @Parameter("folderid") long folderId) throws IOException;

    @Method("listfolder")
    FolderResponse listFolder(@Parameter("folderid") long folderId) throws IOException;


}
