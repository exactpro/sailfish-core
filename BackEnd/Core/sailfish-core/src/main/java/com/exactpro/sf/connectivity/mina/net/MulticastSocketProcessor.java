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
package com.exactpro.sf.connectivity.mina.net;

import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.util.ExceptionMonitor;
import org.apache.mina.util.NamePreservingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.HexDumper;


public class MulticastSocketProcessor implements IoProcessor<MulticastSocketSession> {

	private static final Logger logger = LoggerFactory.getLogger(MulticastSocketProcessor.class);

	private boolean wakeup = true;
	private final Object sessionLock = new Object();

	//Chain for reading
	//read in run, fire
	//IoFilterChain filterChain = session.getFilterChain();
    //filterChain.fireMessageReceived(buf);
    //buf = null;

	private static final AtomicLong threadID = new AtomicLong(0);

	private static final String threadNamePrefix = MulticastSocketProcessor.class.getSimpleName();


	private int bufSize = 65540;

	private final Object lock = new Object();

	private final String threadName;

	private final Executor executor;

    private final Object disposalLock = new Object();

    private volatile boolean disposing;

    private volatile boolean disposed;

    final DefaultIoFuture disposalFuture = new DefaultIoFuture(null);

    /** The processor thread : it handles the incoming messages */
    private Processor processor;

    /** A Session queue containing the newly created sessions */
    private MulticastSocketSession currentSession = null;

	//Chain for writing
	//when flush is called you need to check WriteRequest queue


	public MulticastSocketProcessor(Executor executor) {

		if (executor == null) {
            throw new NullPointerException("executor");
        }

        this.threadName = nextThreadName();
        this.executor = executor;
	}



	/**
     * {@inheritDoc}
     */
    @Override
    public final void add(MulticastSocketSession session) {

    	if (isDisposing())
            throw new IllegalStateException("Already disposed.");

        synchronized (sessionLock) {

        	if (this.currentSession != null) {
        		throw new IllegalStateException("Session already processed");
        	}

        	this.currentSession = session;

        	this.addNow(session);
		}

        startupProcessor();
    }


	/**
     * Starts the inner Processor, asking the executor to pick a thread in its
     * pool. The Runnable will be renamed
     */
    private void startupProcessor() {
        synchronized (lock) {
            if (processor == null) {
                processor = new Processor();
                executor.execute(new NamePreservingRunnable(processor, threadName));
            }
        }

        wakeup();
    }


    private void wakeup() {
    	synchronized (this.sessionLock) {
    	    this.wakeup = true;
    		this.sessionLock.notify();
		}
    }


    @Override
    public final void dispose() {
        if (disposed) {
            return;
        }

        synchronized (disposalLock) {
            if (!disposing) {
                disposing = true;
                startupProcessor();
            }
        }

        disposalFuture.awaitUninterruptibly();
        disposed = true;
    }


	@Override
	public void flush(MulticastSocketSession session) {
		// do nothing
	}


	/**
     * {@inheritDoc}
     */
    @Override
    public final boolean isDisposing() {
        return disposing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isDisposed() {
        return disposed;
    }


	@Override
	public void remove(MulticastSocketSession session) {

		synchronized ( sessionLock ) {
			removeNow(session);
			currentSession = null;
		}

	}


	@Override
	public void updateTrafficControl(MulticastSocketSession session) {
		// do nothing
	}


	private void destroy(MulticastSocketSession session) throws Exception {
		MulticastSocket socket = session.getSocket();

		socket.close();
	}


	boolean removeNow(MulticastSocketSession session) {
		//FIXME
        //clearWriteRequestQueue(session);

        try {
            destroy(session);
            return true;
        } catch (Exception e) {
            IoFilterChain filterChain = session.getFilterChain();
            filterChain.fireExceptionCaught(e);
        } finally {
        	//FIXME
            //clearWriteRequestQueue(session);
            ((AbstractIoService) session.getService()).getListeners().fireSessionDestroyed(session);
        }
        return false;
    }



	private boolean addNow(MulticastSocketSession session) {

        boolean registered = false;
        boolean notified = false;
        try {
            registered = true;

            // Build the filter chain of this session.
            session.getService().getFilterChainBuilder().buildFilterChain(
                    session.getFilterChain());

            // DefaultIoFilterChain.CONNECT_FUTURE is cleared inside here
            // in AbstractIoFilterChain.fireSessionOpened().
            ((AbstractIoService) session.getService()).getListeners().fireSessionCreated(session);
            notified = true;
            bufSize = session.getConfig().getReadBufferSize();
        } catch (Throwable e) {
            if (notified)
            {
                // Clear the DefaultIoFilterChain.CONNECT_FUTURE attribute
                // and call ConnectFuture.setException().
                this.remove(session);
                IoFilterChain filterChain = session.getFilterChain();
                filterChain.fireExceptionCaught(e);
                wakeup();
            } else {
                ExceptionMonitor.getInstance().exceptionCaught(e);
                try {
                    destroy(session);
                } catch (Exception e1) {
                    ExceptionMonitor.getInstance().exceptionCaught(e1);
                } finally {
                    registered = false;
                }
            }
        }
        return registered;
    }


	class Processor implements Runnable {

        @Override
        public void run() {

        	byte[] buf = new byte[bufSize];
        	DatagramPacket packet = new DatagramPacket(buf, buf.length);

        	while (!isDisposing()) {

        		try {

        			boolean received = false;

        			MulticastSocket socket = null;

        			synchronized ( sessionLock ) {
		        		if ( currentSession != null ) {
		        			socket = currentSession.getSocket();
		        		} else {
		        		    if (!wakeup) {
		        		        sessionLock.wait();
		        		    }
		        		    wakeup = false;
		        		}
        			}

        			if (socket != null) {
	        			try {
	        	        	socket.receive(packet);
	        	        	received = true;
	        	        } catch ( SocketTimeoutException e ) {
	        	        	//It's normal situation.
	        	        } catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
        			}

        			if (received) {

        				Object filterVal = currentSession.getAttribute("AddressFilter");

        				boolean process = true;

        				if (filterVal != null) {
        					String strFilterVal = (String)filterVal;

        					if (!strFilterVal.equals(packet.getAddress().getHostName()))
        						process = false;
        				}

        				String address = packet.getAddress().getHostAddress() + ":" + packet.getPort();

        				byte[] message = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());

        				if (process) {

        					IoFilterChain filterChain = currentSession.getFilterChain();

            				IoBuffer buffer = IoBuffer.allocate(packet.getLength());

							IoBufferWithAddress bufferWithAddress =
								new IoBufferWithAddress(buffer, address);

							bufferWithAddress.setAutoExpand(true);

                            bufferWithAddress.put(message);

							bufferWithAddress.flip();

							filterChain.fireMessageReceived(bufferWithAddress);

        				} else {
        					IoBuffer buffer = IoBuffer.wrap(message);

        					if(logger.isWarnEnabled()) {
        					    logger.warn("Packet from [{}] was filtered. Data [{}]", address, HexDumper.getHexdump(buffer, buffer.limit()));
        					}
        				}
        			}

        		} catch (Throwable t) {

        			ExceptionMonitor.getInstance().exceptionCaught(t);

        			try {
        				Thread.sleep(1000);
        			} catch (InterruptedException e1) {
        				ExceptionMonitor.getInstance().exceptionCaught(e1);
        			}
        		}

        	}

        	synchronized (sessionLock) {
        		if (currentSession != null)
            		removeNow(currentSession);
			}

            disposalFuture.setValue(true);

        }
	}


	private String nextThreadName() {
        return threadNamePrefix + '-' + threadID.getAndIncrement();
    }



	@Override
	public void write(MulticastSocketSession session, WriteRequest writeRequest) {
		throw new UnsupportedOperationException("MulticastSocketProcessor doesn't provide write(MulticastSocketSession session, WriteRequest writeRequest)");
	}

}
