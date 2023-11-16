package com.pcloud.networking.api;

import com.pcloud.networking.client.Response;
import com.pcloud.networking.protocol.ProtocolReader;

import java.io.IOException;
import java.util.Collection;

abstract class InterceptingResponseAdapter<T> implements ResponseAdapter<T> {

    private final Collection<ResponseInterceptor> interceptors;
    private final boolean responsePeekingRequired;

    InterceptingResponseAdapter(Class<T> type, Collection<ResponseInterceptor> interceptors) {
        this.interceptors = interceptors;
        this.responsePeekingRequired = !ApiResponse.class.isAssignableFrom(type);
    }

    @Override
    public T adapt(Response response) throws IOException {
        if (!interceptors.isEmpty()) {
            final T result;
            final ApiResponse interceptorTarget;

            if (responsePeekingRequired) {
                interceptorTarget = peekApiResponse(response);
                result = doAdapt(response);
            } else {
                result = doAdapt(response);
                interceptorTarget = (ApiResponse) result;
            }

            callInterceptors(response, interceptorTarget);
            return result;
        } else {
            return doAdapt(response);
        }
    }

    protected abstract T doAdapt(Response response) throws IOException;

    private void callInterceptors(Response response, ApiResponse interceptorTarget) {
        for (ResponseInterceptor interceptor : interceptors) {
            try {
                interceptor.intercept(interceptorTarget);
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format("Error while calling ResponseInterceptor of type '%s' for '%s' call.",
                                interceptor.getClass(), response.request().methodName()), e
                );
            }
        }
    }

    private ApiResponse peekApiResponse(Response response) throws IOException {
        final ProtocolReader peekingReader = response.responseBody().reader().newPeekingReader();

        long result = -1L;
        String message = null;

        peekingReader.beginObject();
        while_loop:
        while (peekingReader.hasNext()) {
            switch (peekingReader.readString()) {
                case "result": {
                    result = peekingReader.readNumber();
                    if (result == ApiResponse.RESULT_SUCCESS) {
                        break while_loop;
                    }
                    break;
                }
                case "message": {
                    message = peekingReader.readString();
                    if (result != -1) {
                        break while_loop;
                    }
                    break;
                }
                default:
                    peekingReader.skipValue();
                    break;
            }
        }
        if (result == -1L) {
            throw new IOException("Response did not contain 'result' value, call name=" + response.request().methodName());
        }

        return new ApiResponse(result, message);
    }
}
