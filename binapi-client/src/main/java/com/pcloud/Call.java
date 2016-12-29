package com.pcloud;

import java.io.IOException;

/**
 * Created by Georgi on 26.12.2016 г..
 */
public interface Call {
    Request request();

    Response execute() throws IOException;

    void execute(Callback callback);

    boolean isExecuted();

    void cancel();

    boolean isCancelled();
}
