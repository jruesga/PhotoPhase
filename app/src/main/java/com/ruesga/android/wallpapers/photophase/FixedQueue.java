/*
 * Copyright (C) 2015 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represent a FIFO queue with a fixed size. When the queue reach the maximum defined
 * size then extract the next element from the queue.
 * @param <T> The type of object to hold.
 */
@SuppressWarnings("unchecked")
public class FixedQueue<T> {

    /**
     * An exception thrown when the queue hasn't more elements
     */
    public static class EmptyQueueException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private final Object[] mQueue;
    private final int mSize;
    private int mHead;
    private int mTail;

    /**
     * Constructor of <code>FixedQueue</code>
     *
     * @param size The size of the queue. The limit of objects in queue. Beyond this limits
     * the older objects are recycled.
     */
    public FixedQueue(int size) {
        super();
        this.mQueue = new Object[size];
        this.mSize = size;
        this.mHead = 0;
        this.mTail = 0;
    }

    /**
     * Method that inserts a new object to the queue.
     *
     * @param o The object to insert
     * @return The object inserted (for concatenation purpose)
     */
    public T insert(T o) {
        synchronized (this.mQueue) {
            if (o == null) throw new NullPointerException();
            if (this.mQueue[this.mHead] != null) {
                try {
                    noSynchronizedRemove();
                } catch (Throwable ex) {/*NON BLOCK*/}
            }
            this.mQueue[this.mHead] = o;
            this.mHead++;
            if (this.mHead >= this.mSize) {
                this.mHead = 0;
            }
            return o;
        }
    }

    /**
     * Method that extract the first element in the queue
     *
     * @return The item extracted
     * @throws EmptyQueueException If the queue hasn't element
     */
    public T remove() throws EmptyQueueException {
        synchronized (this.mQueue) {
            return noSynchronizedRemove();
        }
    }

    /**
     * Method that extract all the items from the queue
     *
     * @return The items extracted
     * @throws EmptyQueueException If the queue hasn't element
     */
    public List<T> removeAll() throws EmptyQueueException {
        synchronized (this.mQueue) {
            if (isEmpty()) throw new EmptyQueueException();
            List<T> l = new ArrayList<>();
            while (!isEmpty()) {
                l.add(noSynchronizedRemove());
            }
            return l;
        }
    }

    /**
     * Method that retrieves the first element in the queue. This method doesn't remove the item
     * from queue.
     *
     * @return The item retrieved
     * @throws EmptyQueueException If the queue hasn't element
     */
    public T peek() throws EmptyQueueException {
        synchronized (this.mQueue) {
            T o = (T)this.mQueue[this.mTail];
            if (o == null) throw new EmptyQueueException();
            return o;
        }

    }

    /**
     * Method that retrieves all the items from the queue. This method doesn't remove any item
     * from queue.
     *
     * @return The items retrieved
     * @throws EmptyQueueException If the queue hasn't element
     */
    public List<T> peekAll() throws EmptyQueueException {
        synchronized (this.mQueue) {
            if (isEmpty()) throw new EmptyQueueException();
            List<T> l = new ArrayList<>();
            int head = this.mHead;
            int tail = this.mTail;
            do {
                l.add((T)this.mQueue[tail]);
                tail++;
                if (tail >= this.mSize) {
                    tail = 0;
                }
            } while (head != tail);
            return l;
        }
    }

    /**
     * Method that returns if the queue is empty
     *
     * @return boolean If the queue is empty
     */
    public boolean isEmpty() {
        synchronized (this.mQueue) {
            return this.mQueue[this.mTail] == null;
        }
    }

    /**
     * Method that returns the number of items in the queue
     *
     * @return int The number of items
     */
    public int items() {
        int cc = 0;
        int head = this.mHead;
        int tail = this.mTail;
        do {
            if (this.mQueue[tail] == null) {
                return cc;
            }
            cc++;
            tail++;
            if (tail >= this.mSize) {
                tail = 0;
            }
        } while (head != tail);
        return cc;
    }

    /**
     * Method that remove one item without synchronization (for be called from
     * synchronized method).
     *
     * @return The item extracted
     * @throws EmptyQueueException If the queue hasn't element
     */
    private T noSynchronizedRemove() throws EmptyQueueException {
        T o = (T)this.mQueue[this.mTail];
        if (o == null) throw new EmptyQueueException();
        this.mQueue[this.mTail] = null;
        this.mTail++;
        if (this.mTail >= this.mSize) {
            this.mTail = 0;
        }
        return o;
    }

    /**
     * Method that returns the size of this queue.
     *
     * @return The size of this queue
     */
    public int size() {
        return mSize;
    }
}