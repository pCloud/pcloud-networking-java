package com.pcloud.example;

import com.pcloud.networking.DataApiResponse;
import com.pcloud.protocol.DataSource;
import okio.ByteString;
import okio.Okio;
import org.junit.Test;
import java.nio.charset.Charset;
import static org.junit.Assert.*;

/**
 * Created by Georgi on 4/24/2017.
 */
public class FIleOperationTest extends AbstractTest{


    @Test
    public void donwloadFile_ShouldNotFail() throws Exception {

        FileResponse response = donwloadApi.downloadRemoteUrl("http://cms.hostelbookers.com/hbblog/wp-content/uploads/sites/3/2012/02/cat-happy-cat-e1329931204797.jpg", 0L);

        assertNotNull(response);
        assertEquals(0, response.getMetadata().get(0).getFolderid());
        assertEquals("cat-happy-cat-e1329931204797.jpg", response.getMetadata().get(0).getName());
    }

    @Test
    public void downloadFile_ShouldNotFailOnWrongFolderId() throws Exception {

        FileResponse response = donwloadApi.downloadRemoteUrl("http://cms.hostelbookers.com/hbblog/wp-content/uploads/sites/3/2012/02/cat-happy-cat-e1329931204797.jpg", -1);
        assertNotNull(response);
        assertEquals(0L, response.resultCode());
    }

    @Test
    public void downloadFile_ShouldNotFailOnEmptyUrl() throws Exception {

        FileResponse response = donwloadApi.downloadRemoteUrl("", 0);
        assertNotNull(response);
        assertEquals(0, response.resultCode());
    }

    @Test
    public void downloadFile_ShouldFailOnNullUrl() throws Exception {

        exception.expect(IllegalArgumentException.class);
        donwloadApi.downloadRemoteUrl(null, 0);
    }

    @Test
    public void downloadThumb_ShouldNotFailOnCorrectParameters() throws Exception {

        DataApiResponse response = donwloadApi.getThumb("/cat-happy-cat-e1329931204797.jpg", "64x64");
        assertNotNull(response);
        assertNotNull(response.responseData());
        assertNotEquals(0, response.responseData().contentLength());
        response.responseData().writeTo(Okio.buffer(Okio.blackhole()));
    }

    @Test
    public void downloadThumb_ShouldNotFailOnWrongFileName() throws Exception {

        DataApiResponse response = donwloadApi.getThumb("/abc", "64x64");
        assertNotNull(response);
        assertNull(response.responseData());
    }

    @Test
    public void downloadThumb_ShouldFailOnNullParameters() throws Exception {

        exception.expect(IllegalArgumentException.class);
        donwloadApi.getThumb(null, "64x64");
        donwloadApi.getThumb("", null);
    }

    @Test
    public void uploadFile_ShouldNotFail() throws Exception {

        ByteString string = ByteString.encodeString("abc123qwerty!@#$%^&*(", Charset.forName("UTF-8"));
        FileResponse response = donwloadApi.uploadFile(0, "someFile.txt", DataSource.create(string));
        assertNotNull(response);
    }

    @Test
    public void uploadFile_ShouldNotFailOnNullDataSource() throws Exception {

        FileResponse response = donwloadApi.uploadFile(0, "someFile.txt", null);
        assertNotNull(response);
    }

    @Test
    public void updateFile_ShouldFailOnNullFileName() throws Exception {

        exception.expect(IllegalArgumentException.class);
        donwloadApi.uploadFile(0, null, null);
    }
}
