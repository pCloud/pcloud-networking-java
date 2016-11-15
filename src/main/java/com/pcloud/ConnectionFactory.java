package com.pcloud;

import java.io.IOException;

/**
 * Created by Georgi on 11/10/2016.
 */

public interface ConnectionFactory {
    Connection openConnection() throws IOException;
}
