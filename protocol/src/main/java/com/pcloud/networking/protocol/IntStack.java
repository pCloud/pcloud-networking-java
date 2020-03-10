/*
 * Copyright (c) 2020 pCloud AG
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

package com.pcloud.networking.protocol;

import java.util.Arrays;
import java.util.NoSuchElementException;

class IntStack {
    private static final int INITIAL_CAPACITY = 10;

    private int[] data;
    private int elementCount;

    IntStack() {
        this(INITIAL_CAPACITY);
    }

    IntStack(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Capacity must be a positive number");
        }
        data = new int[initialCapacity];
        elementCount = 0;
    }

    IntStack(IntStack scopeStack) {
        this.data = Arrays.copyOf(scopeStack.data, scopeStack.data.length);
        this.elementCount = scopeStack.elementCount;
    }

    private void ensureCapacity(int minimumCapacity) {
        if (data.length < minimumCapacity) {
            int[] biggerArray = new int[minimumCapacity];
            System.arraycopy(data, 0, biggerArray, 0, elementCount);
            data = biggerArray;
        }
    }

    public boolean isEmpty() {
        return (elementCount == 0);
    }

    public int peek() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return data[elementCount - 1];
    }

    public int pop() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return data[--elementCount];
    }

    public void push(int item) {
        if (elementCount == data.length) {
            int newCapacity = elementCount << 1;
            if (newCapacity < 0) {
                throw new IllegalStateException("Stack size too big.");
            }
            ensureCapacity(newCapacity);
        }
        data[elementCount++] = item;
    }

    public int size() {
        return elementCount;
    }
}
