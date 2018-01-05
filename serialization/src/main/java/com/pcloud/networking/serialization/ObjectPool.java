/*
 * Copyright (c) 2018 pCloud AG
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

package com.pcloud.networking.serialization;

class ObjectPool<T> {
    private final Object[] pool;
    private int mPoolSize;

    ObjectPool(int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("The max pool size must be > 0");
        }
        pool = new Object[maxPoolSize];
    }

    @SuppressWarnings("unchecked")
    public T acquire() {
        synchronized (pool) {
            if (mPoolSize > 0) {
                final int lastPooledIndex = mPoolSize - 1;
                T instance = (T) pool[lastPooledIndex];
                pool[lastPooledIndex] = null;
                mPoolSize--;
                return instance;
            }
            return null;
        }
    }

    public boolean recycle(T instance) {
        synchronized (pool) {
            if (mPoolSize < pool.length) {
                pool[mPoolSize] = instance;
                mPoolSize++;
                return true;
            }
            return false;
        }
    }

}
