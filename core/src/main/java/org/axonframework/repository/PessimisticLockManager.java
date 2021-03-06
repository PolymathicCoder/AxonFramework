/*
 * Copyright (c) 2010-2011. Axon Framework
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


package org.axonframework.repository;

import org.axonframework.common.Assert;
import org.axonframework.domain.AggregateRoot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the {@link LockManager} that uses a pessimistic locking strategy. Calls to obtainLock will block
 * until a lock could be obtained. If a lock is obtained by a thread, that thread has guaranteed unique access.
 *
 * @author Allard Buijze
 * @since 0.3
 */
class PessimisticLockManager implements LockManager {

    private final ConcurrentHashMap<Object, DisposableLock> locks = new ConcurrentHashMap<Object, DisposableLock>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateLock(AggregateRoot aggregate) {
        Object aggregateIdentifier = aggregate.getIdentifier();

        return isLockAvailableFor(aggregateIdentifier)
                && lockFor(aggregateIdentifier).isHeldByCurrentThread();
    }

    /**
     * Obtain a lock for an aggregate. This method will block until a lock was successfully obtained.
     *
     * @param aggregateIdentifier the identifier of the aggregate to obtains a lock for.
     */
    @Override
    public void obtainLock(Object aggregateIdentifier) {
        boolean lockObtained = false;
        while (!lockObtained) {
            createLockIfAbsent(aggregateIdentifier);
            DisposableLock lock = lockFor(aggregateIdentifier);
            lockObtained = lock.lock();
            if (!lockObtained) {
                locks.remove(aggregateIdentifier, lock);
            }
        }
    }

    /**
     * Release the lock held on the aggregate. If no valid lock is held by the current thread, an exception is thrown.
     *
     * @param aggregateIdentifier the identifier of the aggregate to release the lock for.
     * @throws IllegalStateException        if no lock was ever obtained for this aggregate
     * @throws IllegalMonitorStateException if a lock was obtained, but is not currently held by the current thread
     */
    @Override
    public void releaseLock(Object aggregateIdentifier) {
        Assert.state(locks.containsKey(aggregateIdentifier), "No lock for this aggregate was ever obtained");
        DisposableLock lock = lockFor(aggregateIdentifier);
        lock.unlock(aggregateIdentifier);
    }

    private void createLockIfAbsent(Object aggregateIdentifier) {
        if (!locks.contains(aggregateIdentifier)) {
            locks.putIfAbsent(aggregateIdentifier, new DisposableLock());
        }
    }

    private boolean isLockAvailableFor(Object aggregateIdentifier) {
        return locks.containsKey(aggregateIdentifier);
    }

    private DisposableLock lockFor(Object aggregateIdentifier) {
        return locks.get(aggregateIdentifier);
    }

    private final class DisposableLock {

        private final ReentrantLock lock;
        // guarded by "lock"
        private volatile boolean isClosed = false;

        private DisposableLock() {
            this.lock = new ReentrantLock();
        }

        private boolean isHeldByCurrentThread() {
            return lock.isHeldByCurrentThread();
        }

        private void unlock(Object aggregateIdentifier) {
            lock.unlock();
            disposeIfUnused(aggregateIdentifier);
        }

        private boolean lock() {
            lock.lock();
            if (isClosed) {
                lock.unlock();
                return false;
            }
            return true;
        }

        private void disposeIfUnused(Object aggregateIdentifier) {
            if (lock.tryLock()) {
                try {
                    if (lock.getHoldCount() == 1) {
                        // we now have a lock. We can shut it down.
                        isClosed = true;
                        locks.remove(aggregateIdentifier, this);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
