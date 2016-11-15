/*
 * Copyright (c) 2016 Georgi Neykov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud;

import com.pcloud.value.ObjectValue;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;

import static com.pcloud.internal.IOUtils.closeQuietly;

public class ApiConnector {

    private BinaryProtocolCodec binaryProtocolCodec;
    private ConnectionPool connectionPool;
    private ConnectionFactory connectionFactory;
    private boolean eagerlyCheckConnectivity;
    private int retryCount;

    public ApiConnector() {
        this.binaryProtocolCodec = new BinaryProtocolCodec();
        this.connectionPool = new ConnectionPool();
        this.connectionFactory = new DefaultConnectionFactory();
        this.eagerlyCheckConnectivity = true;
    }

    public ApiResponse callMethod(String methodName, ObjectValue request) throws IOException {
        return callMethod(methodName, request, (Source)null);
    }

    public ApiResponse callMethod(String methodName, ObjectValue request, InputStream blobDataStream) throws IOException {
        return callMethod(methodName, request, Okio.source(blobDataStream));
    }

    public ApiResponse callMethod(String methodName, ObjectValue request, Source blobData) throws IOException {
        boolean success = false;
        Connection connection = obtainConnection();
        System.out.println("Making request using connection "+connection);
        try {
            binaryProtocolCodec.writeRequest(connection.getSink(), methodName, request, blobData, -1);
            ObjectValue responseBody = binaryProtocolCodec.readResponse(connection.getSource());
            success = true;
            return new ApiResponse(responseBody);
        } finally {
            if (success) {
                connectionPool.recycle(connection);
            } else {
                closeQuietly(connection);
            }
            closeQuietly(blobData);
        }
    }

    private Connection obtainConnection() throws IOException {
        Connection connection;
        while ((connection = connectionPool.get()) != null){
            if(connection.isHealthy(eagerlyCheckConnectivity)){
                return connection;
            } else {
                closeQuietly(connection);
            }
        }

        // No pooled connections, just create a new one.
        return connectionFactory.openConnection();
    }
}
