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

package org.axonframework.test.eventscheduler;

import org.axonframework.domain.EventMessage;
import org.axonframework.eventhandling.scheduling.ScheduleToken;
import org.joda.time.DateTime;

/**
 * ScheduleToken returned by the StubEventScheduler.
 *
 * @author Allard Buijze
 * @since 1.1
 */
class StubScheduleToken implements ScheduleToken, Comparable<StubScheduleToken>, ScheduledItem {

    private static final long serialVersionUID = 3763093001261110665L;

    private final DateTime scheduleTime;
    private final EventMessage event;
    private final int counter;

    /**
     * Initialize the token with the given <code>scheduleTime</code>, <code>event</code> and <code>counter</code>.
     *
     * @param scheduleTime The time at which to trigger the event
     * @param event        The scheduled event
     * @param counter      A counter used for sorting purposes. When two events are scheduled for the same time, the
     *                     counter decides which comes first.
     */
    StubScheduleToken(DateTime scheduleTime, EventMessage event, int counter) {
        this.scheduleTime = scheduleTime;
        this.event = event;
        this.counter = counter;
    }

    @Override
    public DateTime getScheduleTime() {
        return scheduleTime;
    }

    @Override
    public EventMessage getEvent() {
        return event;
    }

    @Override
    public int compareTo(StubScheduleToken other) {
        if (scheduleTime.equals(other.scheduleTime)) {
            return counter - other.counter;
        }
        return scheduleTime.compareTo(other.scheduleTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StubScheduleToken that = (StubScheduleToken) o;

        if (counter != that.counter) {
            return false;
        }
        if (!scheduleTime.equals(that.scheduleTime)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return scheduleTime.hashCode();
    }
}
