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

package com.pcloud;

import com.pcloud.protocol.streaming.*;
import okio.Buffer;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.pcloud.IOUtils.closeQuietly;

class RealMultiCall implements MultiCall {

    private List<Request> requests;
    private boolean executed;

    private boolean cancelled;
    private Connection connection;

    private ExecutorService callExecutor;
    private ConnectionProvider connectionProvider;
    private List<RequestInterceptor> interceptors;

    RealMultiCall(List<Request> requests, ExecutorService callExecutor, List<RequestInterceptor> interceptors, ConnectionProvider connectionProvider) {
        this.requests = requests;
        this.callExecutor = callExecutor;
        this.connectionProvider = connectionProvider;
        this.interceptors = interceptors;
    }

    @Override
    public List<Request> requests() {
        return null;
    }

    @Override
    public MultiResponse execute() throws IOException {
        checkAndMarkExecuted();

        return getMultiResponse();
    }

    private MultiResponse getMultiResponse() throws IOException {
        if (cancelled) {
            throw new IOException("Cancelled.");
        }

        Endpoint endpoint = requests.get(0).endpoint();
        RealConnection connection = connectionProvider.obtainConnection(endpoint);
        this.connection = connection;
        boolean success = false;
        Map<Integer, Response> responseMap = new TreeMap<>();
        try {

            // Write all requests.
            writeRequests(connection);

            // Start reading responses.
            final int expectedCount = requests.size();
            initializeResponseMap(responseMap, expectedCount);
            int completedCount = 0;
            while (completedCount < expectedCount){
                readNextResponse(connection, responseMap);
                completedCount++;
            }

            success = true;
        } finally {
            if (success) {
                connectionProvider.recycleConnection(connection);
            } else {
                closeAndClearCompletedResponses(responseMap);
                closeQuietly(connection);
            }
            this.connection = null;
        }

        return new MultiResponse(new ArrayList<>(responseMap.values()));
    }

    private void initializeResponseMap(Map<Integer, Response> responseMap, int expectedCount) {
        for (int i = 0; i < expectedCount; i++){
            responseMap.put(i, null);
        }
    }

    @Override
    public MultiResponse enqueueAndWait() throws IOException, InterruptedException {
        checkAndMarkExecuted();
        try {
            return callExecutor.submit(new Callable<MultiResponse>() {
                @Override
                public MultiResponse call() throws IOException {
                    return getMultiResponse();
                }
            }).get();
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        } catch (CancellationException e){
            throw new IOException(e);
        }
    }

    @Override
    public MultiResponse enqueueAndWait(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException, TimeoutException {
        checkAndMarkExecuted();
        try {
            return callExecutor.submit(new Callable<MultiResponse>() {
                @Override
                public MultiResponse call() throws IOException {
                    return getMultiResponse();
                }
            }).get(timeout, timeUnit);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

    @Override
    public void enqueue(final MultiCallback callback) {
        checkAndMarkExecuted();
        callExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final int expectedCount = requests.size();
                final Map<Integer, Response> responseMap = new TreeMap<>();
                initializeResponseMap(responseMap, expectedCount);

                Endpoint endpoint = requests.get(0).endpoint();
                RealConnection connection = null;
                boolean success = false;
                boolean callingCallback = false;
                try {
                    if (cancelled) {
                        throw new IOException("Cancelled.");
                    }

                    connection = connectionProvider.obtainConnection(endpoint);
                    RealMultiCall.this.connection = connection;

                    //Write the requests.
                    writeRequests(connection);

                    int completedCount = 0;
                    while (completedCount < expectedCount){
                        int key = readNextResponse(connection, responseMap);
                        completedCount++;
                        // Guard against calling onFailure() for IOException errors
                        // thrown inside the callback method.
                        callingCallback = true;
                        callback.onResponse(RealMultiCall.this, key, responseMap.get(key));
                        callingCallback = false;
                    }

                    success = true;
                    MultiResponse response = new MultiResponse(new ArrayList<>(responseMap.values()));
                    callingCallback = true;
                    callback.onComplete(RealMultiCall.this, response);
                } catch (IOException e){
                    List<Response> completedResponses =
                            Collections.unmodifiableList(new ArrayList<>(responseMap.values()));
                    if (!callingCallback) {
                        callback.onFailure(RealMultiCall.this, e, completedResponses);
                    } else {
                        // Some of the callbacks has failed, do cleanup by closing all responses.
                        closeAndClearCompletedResponses(responseMap);
                    }
                } finally {
                    if (success) {
                        connectionProvider.recycleConnection(connection);
                    } else {
                        closeQuietly(connection);
                        closeAndClearCompletedResponses(responseMap);
                    }
                    RealMultiCall.this.connection = null;
                }
            }
        });
    }

    private void closeAndClearCompletedResponses(Map<?, ? extends Closeable> responseMap) {
        for (Closeable r : responseMap.values()){
            closeQuietly(r);
        }
        responseMap.clear();
    }

    private void writeRequests(Connection connection) throws IOException {
        long requestKey = 0;
        for (Request request : requests){
            ProtocolRequestWriter writer = new BytesWriter(connection.sink());
            writer.beginRequest()
                    .writeMethodName(request.methodName());
            if (request.dataSource() != null) {
                writer.writeData(request.dataSource());
            }

            for (RequestInterceptor r: interceptors) {
                r.intercept(request, writer);
            }

            request.body().writeТо(writer);

            // Add the key at the end to avoid overwriting.
            writer.writeName("id", TypeToken.NUMBER).writeValue(requestKey);
            writer.endRequest();
            writer.flush();
            requestKey++;
        }
    }

    @Override
    public synchronized boolean isExecuted() {
        return executed;
    }

    @Override
    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            closeQuietly(connection);
            connection = null;
        }
    }

    private void checkAndMarkExecuted() {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
    }

    private int readNextResponse(final Connection connection, Map<Integer, Response> responseMap) throws IOException {

        final long responseLength = IOUtils.peekNumberLe(connection.source(), 4);
        final Buffer responseBuffer = new Buffer();
        connection.source().read(responseBuffer, responseLength + 4);

        final BytesReader reader = new BytesReader(responseBuffer) {
            @Override
            public void endObject() throws IOException {
                super.endObject();
                if (currentScope() == SCOPE_RESPONSE){
                    endResponse();
                }
            }
        };

        ResponseBody responseBody = new ResponseBody() {
            @Override
            public ProtocolReader reader() {
                return reader;
            }

            @Override
            public long contentLength() {
                return responseLength;
            }

            @Override
            public ResponseData data() throws IOException {
                return null;
            }

            @Override
            public void close() throws IOException {
                responseBuffer.close();
            }
        };

        // Find the 'id' key value nad check for a 'data' key
        // Clone the buffer to allow reading of the buffered data
        // without consuming the bytes form the original buffer.
        BytesReader valuesReader = new BytesReader(responseBuffer.clone());
        if (responseLength != valuesReader.beginResponse()){
            throw new AssertionError("Peeked and read response lengths are not equal.");
        }
        valuesReader.beginObject();
        int id = -1;
        long dataLength = -1;
        while (valuesReader.hasNext()){
            String name = valuesReader.readString();
            if (name.equals("id")) {
                id = (int) valuesReader.readNumber();
            }
            else {
                valuesReader.skipValue();
            }
            if ((dataLength = valuesReader.dataContentLength()) != -1) {
                break;
            }
        }
        if (dataLength > 0){
            throw new IOException("MultiCalls are not supported for responses returning data.");
        }

        if (id == -1){
            throw new IOException("Response is missing its id.");
        }

        Response response = Response.create()
                .request(requests.get(id))
                .responseBody(responseBody)
                .build();
        if(responseMap.put(id, response) != null) {
            throw new IOException("Server responded with multiple responses with id '"+id+"'.");
        }

        return id;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
