package com.pcloud.example;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by Georgi on 4/24/2017.
 */
public class FolderTest extends AbstractTest{



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
