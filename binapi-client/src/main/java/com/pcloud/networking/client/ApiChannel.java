package com.pcloud.networking.client;

import com.pcloud.networking.protocol.ProtocolRequestWriter;
import com.pcloud.networking.protocol.ProtocolResponseReader;
import okio.BufferedSink;

import java.nio.channels.Channel;

/**
 * A low-level contract for interfacing with an pCloud API Host via
 * binary protocol-encoded messages.
 * <p>
 * The {@linkplain ApiChannel} interface exposes the lowest possible level
 * of detail when writing/reading messages from the API by still abstracting away
 * the details of connection establishment, TLS handshaking and so on.
 * <p>
 * The interface can be used in the cases where the flows using the {@linkplain Call} and {@linkplain MultiCall}
 * abstractions may yield a considerable amount of memory allocations, usually when making a chain
 * of repeating requests. The contract does not restrict in any way how the requests will be written, so
 * request pipelining can be implemented by writing multiple requests at once, without immediately reading the
 * responses, which may result in considerable speed improvements.
 * <p>
 * If otherwise stated, instances of this interface will not be thread-safe and reading/writing at the same time
 * from multiple threads will result in undetermined behavior. All implementations should allow calling
 * {@linkplain #close()} from multiple threads multiple times.
 * <p>
 * Calling {@linkplain #close()} on an idle {@linkplain ApiChannel} instance will result in connection recycling,
 * for other cases the underlying socket will be closed.
 * <br>
 * A {@linkplain ApiChannel} instance is considered idle when the number of sent requests is equal
 * to the number of fully read responses.
 * <br>
 * Each successful call to {@linkplain ProtocolRequestWriter#endRequest()} is counted as a sent request.
 * <br>
 * A fully read response is counted after each successful call to {@linkplain ProtocolResponseReader#endResponse()}
 * for non-data responses or to {@linkplain ProtocolResponseReader#readData(BufferedSink)} for data-containing responses.
 * <p>
 * Instances can be obtained via the {@linkplain PCloudAPIClient#newChannel(Endpoint)} and
 * {@linkplain PCloudAPIClient#newChannel()} methods.
 */
public interface ApiChannel extends Channel, AutoCloseable {
    /**
     * @return the non-null {@linkplain Endpoint} to which this channel is connected.
     */
    Endpoint endpoint();

    /**
     * @return a non-null {@linkplain ProtocolResponseReader} for reading data.
     */
    ProtocolResponseReader reader();

    /**
     * @return a non-null {@linkplain ProtocolRequestWriter} for writing data.
     */
    ProtocolRequestWriter writer();

    /**
     * Check whether the channel is idle
     * <p>
     * An {@linkplain ApiChannel} is idle when the number of completed requests
     * equals the number of completely read responses.
     * <p>
     *  A fresh {@linkplain ApiChannel} instance will always be idle.
     *  <p>
     *  The return value for already closed {@linkplain ApiChannel} instances is undetermined.
     *
     * @return {@code true} if channel is idle, {@code false} otherwise.
     */
    boolean isIdle();

    @Override
    void close();
}
