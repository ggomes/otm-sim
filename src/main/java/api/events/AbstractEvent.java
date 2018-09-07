/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.events;

public abstract class AbstractEvent {

    public final float timestamp;

    public AbstractEvent(float timestamp) {
        this.timestamp = timestamp;
    }
}
