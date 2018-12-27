/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
 ******************************************************************************/
package com.exactpro.sf.scriptrunner;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);
	private static final String PAUSED = "Paused by user: ";
    private final AtomicReference<ControllerStatus> currentStataus = new AtomicReference<DebugController.ControllerStatus>(ControllerStatus.RUNNED);
	private long id;
	private IPauseListener pauseListener;
	private String reason;
	private long timeout;

	public DebugController(long id, IPauseListener pauseListener) {
		this.id = id;
		this.pauseListener = pauseListener;
	}

	// Called from GUI only (user press Resume button)
	public synchronized void resumeScript() {
	    this.currentStataus.compareAndSet(ControllerStatus.PAUSED, ControllerStatus.RUNNED);
		this.notifyAll();
	}

	// Called from GUI only (user press Pause button)
	public synchronized void pauseScript() {
		// change script state to PAUSED
		this.reason = PAUSED;
		this.timeout = 0;
		this.currentStataus.compareAndSet(ControllerStatus.RUNNED, ControllerStatus.PAUSED);
	}

	// Called from GUI only (user press Stop button)
    public synchronized void stopScript() {
        this.currentStataus.set(ControllerStatus.STOPPED);
        this.notifyAll();
    }
	
	// Called from AskForContinue only
	public synchronized void pauseScript(long timeout, String reason) {
		// change script state to PAUSED
		this.timeout = timeout;
		this.reason = reason;
		this.currentStataus.compareAndSet(ControllerStatus.RUNNED, ControllerStatus.PAUSED);
	}

	// Called from GUI only (user press Next Step button)
	public synchronized void pauseScriptOnNextStep() {
		// notify Object
		// change script state to RUNNED
		//
		this.reason = PAUSED;
		this.timeout = 0;
		this.notifyAll();
		this.currentStataus.compareAndSet(ControllerStatus.RUNNED, ControllerStatus.PAUSED);
	}

	// Called from from AskForContinue or before each script step 
	public synchronized void doWait(String description) throws InterruptedException {
	    ControllerStatus status = this.currentStataus.get();
	    switch (status) {
            case RUNNED:
                return;
            case PAUSED:
                this.pauseListener.onScriptPaused(id, this.reason + description, this.timeout);
                if (this.timeout > 0) {
                    this.wait(this.timeout);
                } else {
                    this.wait();
                }
                this.pauseListener.onScriptResumed(id);
                break;
            case STOPPED:
                throw new InterruptedException("Stopped by user");
            default:
                logger.warn("Unknown debug controller status {}", status);
                break;
        }
	}
	
	// Called from from AskForContinue or before each script step 
	public synchronized void doWait() throws InterruptedException {
		doWait("");
	}
	
	private enum ControllerStatus {
	    RUNNED,
	    PAUSED,
	    STOPPED
	}
}
