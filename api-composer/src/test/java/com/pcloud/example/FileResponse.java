package com.pcloud.example;

import com.pcloud.networking.ApiResponse;
import com.pcloud.networking.ParameterValue;

import java.util.List;

/**
 * Created by Georgi on 4/21/2017.
 */
public class FileResponse extends ApiResponse{

    @ParameterValue("metadata")
    private List<FolderResponse.Metadata> metadata;

    public List<FolderResponse.Metadata> getMetadata() {
        return metadata;
    }
}
