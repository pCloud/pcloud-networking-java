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

package com.pcloud.networking.client;

import com.pcloud.networking.protocol.BytesReader;
import com.pcloud.networking.protocol.BytesWriter;
import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolRequestWriter;
import com.pcloud.networking.protocol.ProtocolResponseReader;
import com.pcloud.utils.IOUtils;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.pcloud.utils.IOUtils.closeQuietly;

class RealMultiCall implements MultiCall {

    private static final int RESPONSE_LENGTH = 4;

    private volatile boolean executed;
    private volatile boolean cancelled;

    private final List<Request> requests;
    private Connection connection;
    private ExecutorService callExecutor;
    private List<RequestInterceptor> interceptors;
    private ConnectionProvider connectionProvider;
    private Endpoint endpoint;

    RealMultiCall(List<Request> requests, ExecutorService callExecutor,
                  List<RequestInterceptor> interceptors, ConnectionProvider connectionProvider, Endpoint endpoint) {
        this.requests = requests;
        this.callExecutor = callExecutor;
        this.connectionProvider = connectionProvider;
        this.interceptors = interceptors;
        this.endpoint = endpoint;
    }

    @Override
    public List<Request> requests() {
        return requests;
    }

    @Override
    public MultiResponse execute() throws IOException {
        checkAndMarkExecuted();

        return getMultiResponse();
    }

    private MultiResponse getMultiResponse() throws IOException {
        throwIfCancelled();

        Connection connection = endpoint != null ?
                connectionProvider.obtainConnection(endpoint) :
                connectionProvider.obtainConnection();
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
            while (completedCount < expectedCount) {
                readNextBufferedResponse(connection, responseMap);
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
        } catch (CancellationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public MultiResponse enqueueAndWait(long timeout, TimeUnit timeUnit)
            throws IOException, InterruptedException, TimeoutException {
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

                Connection connection = null;
                boolean success = false;
                boolean callingCallback = false;
                try {
                    throwIfCancelled();

                    connection = endpoint != null ?
                            connectionProvider.obtainConnection(endpoint) :
                            connectionProvider.obtainConnection();
                    RealMultiCall.this.connection = connection;

                    //Write the requests.
                    writeRequests(connection);

                    int completedCount = 0;
                    while (completedCount < expectedCount) {
                        int key = readNextBufferedResponse(connection, responseMap);
                        completedCount++;
                        // Guard against calling onFailure() for IOException errors
                        // thrown inside the callback method.
                        callingCallback = true;
                        callback.onResponse(RealMultiCall.this, key, responseMap.get(key));
                        callingCallback = false;
                    }

                    success = true;
                    RealMultiCall.this.connection = null;
                    MultiResponse response = new MultiResponse(new ArrayList<>(responseMap.values()));
                    callingCallback = true;
                    callback.onComplete(RealMultiCall.this, response);
                } catch (IOException e) {
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
                }
            }
        });
    }

    @Override
    public Interactor start() {
        checkAndMarkExecuted();
        return new RealInteractor();
    }

    @Override
    public boolean isExecuted() {
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public MultiCall clone() {
        return new RealMultiCall(requests, callExecutor, interceptors, connectionProvider, endpoint);
    }

    private void initializeResponseMap(Map<Integer, Response> responseMap, int expectedCount) {
        for (int i = 0; i < expectedCount; i++) {
            responseMap.put(i, null);
        }
    }

    private void closeAndClearCompletedResponses(Map<?, ? extends Closeable> responseMap) {
        for (Closeable r : responseMap.values()) {
            closeQuietly(r);
        }
        responseMap.clear();
    }

    private void writeRequests(Connection connection) throws IOException {
        long requestKey = 0;
        for (Request request : requests) {
            writeRequest(connection, requestKey, request);
            requestKey++;
        }
    }

    private void writeRequest(Connection connection, long requestKey, Request request) throws IOException {
        ProtocolRequestWriter writer = new BytesWriter(connection.sink());
        writer.beginRequest()
                .writeMethodName(request.methodName());
        if (request.dataSource() != null) {
            writer.writeData(request.dataSource());
        }

        for (RequestInterceptor r : interceptors) {
            r.intercept(request, writer);
        }

        request.body().writeTo(writer);

        // Add the key at the end to avoid overwriting.
        writer.writeName("id").writeValue(requestKey);
        writer.endRequest();
        connection.sink().flush();
    }

    private void checkAndMarkExecuted() {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
    }

    private void throwIfCancelled() throws IOException {
        if (cancelled) {
            throw new IOException("Cancelled.");
        }
    }

    private int readNextBufferedResponse(final Connection connection,
                                         Map<Integer, Response> responseMap) throws IOException {
        ResponseBody responseBody = createBufferedResponseBody(connection);
        int id = scanResponseParameters((ProtocolResponseReader) responseBody.reader(), true);
        Response response = Response.create()
                .request(requests.get(id))
                .responseBody(responseBody)
                .build();
        responseMap.put(id, response);
        return id;
    }

    private Response nextUnsafeResponse(Connection connection, boolean recycleOnClose) throws IOException {
        FixedLengthResponseBody responseBody = createUnsafeResponseBody(connection, recycleOnClose);
        int id = scanResponseParameters((ProtocolResponseReader) responseBody.reader(), true);
        return Response.create()
                .request(requests.get(id))
                .responseBody(responseBody)
                .build();
    }

    private BufferedResponseBody createBufferedResponseBody(final Connection connection) throws IOException {
        final long responseLength = IOUtils.peekNumberLe(connection.source(), RESPONSE_LENGTH);

        final Buffer responseBuffer = new Buffer();
        connection.source().read(responseBuffer, responseLength + RESPONSE_LENGTH);

        final BytesReader reader = new SelfEndingBytesReader(responseBuffer);
        checkPeekAndActualContentLengths(responseLength, reader.beginResponse());
        return new BufferedResponseBody(responseBuffer, reader, responseLength, connection.endpoint());
    }

    private FixedLengthResponseBody createUnsafeResponseBody(final Connection connection,
                                                             final boolean recycleOnClose) throws IOException {
        final long responseLength = IOUtils.peekNumberLe(connection.source(), RESPONSE_LENGTH);

        final FixedLengthSource source = new FixedLengthSource(connection.source(),
                responseLength + RESPONSE_LENGTH,
                0, TimeUnit.MILLISECONDS) {
            @Override
            protected void exhausted(boolean reuseSource) {
                if (reuseSource && recycleOnClose) {
                    connectionProvider.recycleConnection(connection);
                    RealMultiCall.this.connection = null;
                }
            }
        };

        final BufferedSource bufferedSource = Okio.buffer(source);
        final BytesReader reader = new NoDataBytesReader(bufferedSource);
        checkPeekAndActualContentLengths(responseLength, reader.beginResponse());
        return new FixedLengthResponseBody(bufferedSource, source, reader, responseLength, connection.endpoint());
    }

    private int scanResponseParameters(ProtocolResponseReader reader, boolean readUntilEnd) throws IOException {
        // Find the 'id' key value and check for a 'data' key
        // Clone the buffer to allow reading of the buffered data
        // without consuming the bytes form the original buffer.
        ProtocolResponseReader peekingReader = reader.newPeekingReader();
        peekingReader.beginObject();
        int id = -1;
        long dataLength = -1;
        while (peekingReader.hasNext()) {
            String name = peekingReader.readString();
            if (name.equals("id")) {
                id = (int) peekingReader.readNumber();
            } else {
                peekingReader.skipValue();
            }
            if (!readUntilEnd && (dataLength = peekingReader.dataContentLength()) != -1) {
                break;
            }
        }
        if (dataLength > 0) {
            throw new IOException("MultiCalls are not supported for responses returning data.");
        }

        if (id == -1) {
            throw new IOException("Response is missing its id.");
        }

        if ((id < 0 || id >= requests.size() || (requests.get(id)) == null)) {
            throw new IOException("Received a response with an unknown id '" + id + "'.");
        }

        return id;
    }

    private static void checkPeekAndActualContentLengths(long peeked, long actual) throws IOException {
        if (peeked != actual) {
            throw new AssertionError("Peeked and actual content lengths are different.");
        }
    }

    private static class NoDataBytesReader extends SelfEndingBytesReader {

        NoDataBytesReader(BufferedSource bufferedSource) {
            super(bufferedSource);
        }

        @Override
        public boolean endResponse() throws IOException {
            boolean hasData = super.endResponse();
            if (hasData) {
                throw new IOException("MultiCalls do not support calls that return data.");
            }

            return hasData;
        }
    }

    private static class BufferedResponseBody extends ResponseBody {

        private final Buffer source;
        private final ProtocolReader reader;
        private final long contentLength;
        private final Endpoint endpoint;

        BufferedResponseBody(Buffer source, ProtocolReader reader, long contentLength, Endpoint endpoint) {
            this.source = source;
            this.reader = reader;
            this.contentLength = contentLength;
            this.endpoint = endpoint;
        }

        @Override
        public ProtocolReader reader() {
            return reader;
        }

        @Override
        public ByteString valuesBytes() throws IOException {
            return source.readByteString();
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public ResponseData data() throws IOException {
            // MultiCalls do not allow responses with data.
            return null;
        }

        @Override
        public Endpoint endpoint() {
            return endpoint;
        }

        @Override
        public void close() {
            source.close();
        }
    }

    private static class FixedLengthResponseBody extends ResponseBody {
        private final BufferedSource bufferedSource;
        private final FixedLengthSource source;
        private final ProtocolReader reader;
        private final long contentLength;
        private final Endpoint endpoint;

        FixedLengthResponseBody(BufferedSource bufferedSource,
                                FixedLengthSource source,
                                BytesReader reader,
                                long contentLength,
                                Endpoint endpoint) {
            this.bufferedSource = bufferedSource;
            this.source = source;
            this.reader = reader;
            this.contentLength = contentLength;
            this.endpoint = endpoint;
        }

        @Override
        public ProtocolReader reader() {
            return reader;
        }

        @Override
        public ByteString valuesBytes() throws IOException {
            return bufferedSource.readByteString();
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public ResponseData data() throws IOException {
            // MultiCalls do not allow responses with data.
            return null;
        }

        @Override
        public Endpoint endpoint() {
            return endpoint;
        }

        @Override
        public void close() throws IOException {
            source.close();
        }

        long bytesRemaining() {
            return source.bytesRemaining();
        }
    }

    private class RealInteractor implements Interactor {

        private final int requestCount;
        private final AtomicInteger remainingRequests;
        private final AtomicInteger handledResponses;
        private final AtomicReference<Response> lastResultReference;
        private volatile boolean closed;

        private RealInteractor() {
            requestCount = requests.size();
            remainingRequests = new AtomicInteger(requestCount);
            handledResponses = new AtomicInteger(0);
            lastResultReference = new AtomicReference<>(null);
        }

        @Override
        public boolean hasMoreRequests() {
            return remainingRequests.get() > 0;
        }

        @Override
        public int submitRequests(int count) throws IOException {
            if (count < 0) {
                throw new IllegalArgumentException("Count parameter cannot be a negative number.");
            }

            synchronized (this) {
                if (!isCancelled() && !closed && connection == null) {
                    connection = endpoint != null ?
                            connectionProvider.obtainConnection(endpoint) :
                            connectionProvider.obtainConnection();
                }
            }
            throwIfCancelled();
            throwIfClosed();

            int sent = 0;
            while (remainingRequests.get() > 0 && sent < count) {
                int key = (requestCount - remainingRequests.get());
                writeRequest(connection, key, requests.get(key));
                remainingRequests.decrementAndGet();
                sent++;
            }

            return sent;
        }

        @Override
        public boolean hasNextResponse() {
            return handledResponses.get() < requestCount;
        }

        @Override
        public Response nextResponse() throws IOException {
            throwIfCancelled();
            throwIfClosed();

            boolean readSuccess = false;
            if (!hasNextResponse()) {
                throw new IllegalStateException("Cannot read next response, no more elements to read.");
            }

            if (handledResponses.get() == requestCount - remainingRequests.get()) {
                throw new IllegalStateException("Cannot read next response, submit at least one more request.");
            }

            Response lastResult = lastResultReference.get();
            if (lastResult != null) {
                FixedLengthResponseBody responseBody = (FixedLengthResponseBody) lastResult.responseBody();
                if (responseBody.bytesRemaining() > 0L) {
                    throw new IOException("Previously returned Result object has not been read fully.");
                }
            }

            try {
                Response result = nextUnsafeResponse(connection, handledResponses.get() >= (requestCount - 1));
                lastResultReference.set(result);
                handledResponses.incrementAndGet();
                readSuccess = true;
                return result;
            } finally {
                if (!readSuccess) {
                    synchronized (this) {
                        closeQuietly(connection);
                        RealMultiCall.this.connection = null;
                    }
                }
            }

        }

        @Override
        public void close() {
            synchronized (this) {
                closed = true;
                if (connection != null) {
                    // Check if the last response was being read.
                    Response lastResponse;
                    if (!hasNextResponse() && (lastResponse = lastResultReference.get()) != null) {
                        FixedLengthResponseBody body = (FixedLengthResponseBody) lastResponse.responseBody();
                        if (body.bytesRemaining() == 0L) {
                            connectionProvider.recycleConnection(connection);
                        }
                    }
                }
                closeQuietly(connection);
                connection = null;
            }
        }

        private void throwIfClosed() throws IOException {
            if (closed) {
                throw new IOException("This Interactor has been closed.");
            }
        }
    }
}
