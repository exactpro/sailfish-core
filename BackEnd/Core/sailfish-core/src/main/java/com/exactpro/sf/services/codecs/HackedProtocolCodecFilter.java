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
package com.exactpro.sf.services.codecs;

import java.net.SocketAddress;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.NothingWrittenException;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderException;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProtocolCodecFilter stores ProtocolDecoderOutput in session. So if we use more then one ProtocolCodecFilter
 * ProtocolDecoderOutput instance will be shared between all of them.
 * Hacked filter does not store ProtocolDecoderOutput instance in session
 */
@Deprecated
public class HackedProtocolCodecFilter extends IoFilterAdapter {
    /** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolCodecFilter.class);

    private static final Class<?>[] EMPTY_PARAMS = new Class[0];

    private static final IoBuffer EMPTY_BUFFER = IoBuffer.wrap(new byte[0]);

    private final AttributeKey ENCODER = new AttributeKey(ProtocolCodecFilter.class, "encoder");

    private final AttributeKey DECODER = new AttributeKey(ProtocolCodecFilter.class, "decoder");

    private ProtocolDecoderOutput decOut = new ProtocolDecoderOutputImpl();

    private ProtocolEncoderOutput encOut;

    /** The factory responsible for creating the encoder and decoder */
    private final ProtocolCodecFactory factory;

    /**
     *
     * Creates a new instance of ProtocolCodecFilter, associating a factory
     * for the creation of the encoder and decoder.
     *
     * @param factory The associated factory
     */
    public HackedProtocolCodecFilter(ProtocolCodecFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        this.factory = factory;
    }

    /**
     * Creates a new instance of ProtocolCodecFilter, without any factory.
     * The encoder/decoder factory will be created as an inner class, using
     * the two parameters (encoder and decoder).
     *
     * @param encoder The class responsible for encoding the message
     * @param decoder The class responsible for decoding the message
     */
    public HackedProtocolCodecFilter(ProtocolEncoder encoder, ProtocolDecoder decoder) {
        if (encoder == null) {
            throw new IllegalArgumentException("encoder");
        }
        if (decoder == null) {
            throw new IllegalArgumentException("decoder");
        }

        // Create the inner Factory based on the two parameters
        this.factory = new ProtocolCodecFactory() {
            @Override
            public ProtocolEncoder getEncoder(IoSession session) {
                return encoder;
            }

            @Override
            public ProtocolDecoder getDecoder(IoSession session) {
                return decoder;
            }
        };
    }

    /**
     * Creates a new instance of ProtocolCodecFilter, without any factory.
     * The encoder/decoder factory will be created as an inner class, using
     * the two parameters (encoder and decoder), which are class names. Instances
     * for those classes will be created in this constructor.
     *
     * @param encoder The class responsible for encoding the message
     * @param decoder The class responsible for decoding the message
     */
    public HackedProtocolCodecFilter(Class<? extends ProtocolEncoder> encoderClass,
            Class<? extends ProtocolDecoder> decoderClass) {
        if (encoderClass == null) {
            throw new IllegalArgumentException("encoderClass");
        }
        if (decoderClass == null) {
            throw new IllegalArgumentException("decoderClass");
        }
        if (!ProtocolEncoder.class.isAssignableFrom(encoderClass)) {
            throw new IllegalArgumentException("encoderClass: " + encoderClass.getName());
        }
        if (!ProtocolDecoder.class.isAssignableFrom(decoderClass)) {
            throw new IllegalArgumentException("decoderClass: " + decoderClass.getName());
        }
        try {
            encoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("encoderClass doesn't have a public default constructor.");
        }
        try {
            decoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("decoderClass doesn't have a public default constructor.");
        }

        ProtocolEncoder encoder;

        try {
            encoder = encoderClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("encoderClass cannot be initialized");
        }

        ProtocolDecoder decoder;

        try {
            decoder = decoderClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("decoderClass cannot be initialized");
        }

        // Create the inner factory based on the two parameters.
        this.factory = new ProtocolCodecFactory() {
            @Override
            public ProtocolEncoder getEncoder(IoSession session) throws Exception {
                return encoder;
            }

            @Override
            public ProtocolDecoder getDecoder(IoSession session) throws Exception {
                return decoder;
            }
        };
    }

    /**
     * Get the encoder instance from a given session.
     *
     * @param session The associated session we will get the encoder from
     * @return The encoder instance, if any
     */
    public ProtocolEncoder getEncoder(IoSession session) {
        return (ProtocolEncoder) session.getAttribute(ENCODER);
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        // Clean everything
        disposeCodec(parent.getSession());
    }

    /**
     * Process the incoming message, calling the session decoder. As the incoming
     * buffer might contains more than one messages, we have to loop until the decoder
     * throws an exception.
     *
     *  while ( buffer not empty )
     *    try
     *      decode ( buffer )
     *    catch
     *      break;
     *
     */
    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        LOGGER.debug("Processing a MESSAGE_RECEIVED for session {}", session.getId());

        if (!(message instanceof IoBuffer)) {
            nextFilter.messageReceived(session, message);
            return;
        }

        IoBuffer in = (IoBuffer) message;
        ProtocolDecoder decoder = factory.getDecoder(session);
        ProtocolDecoderOutput decoderOut = getDecoderOut(session, nextFilter);

        // Loop until we don't have anymore byte in the buffer,
        // or until the decoder throws an unrecoverable exception or
        // can't decoder a message, because there are not enough
        // data in the buffer
        while (in.hasRemaining()) {
            int oldPos = in.position();

            try {
                synchronized (decoderOut) {
                    // Call the decoder with the read bytes
                    decoder.decode(session, in, decoderOut);
                }

                // Finish decoding if no exception was thrown.
                decoderOut.flush(nextFilter, session);
            } catch (Exception e) {
                ProtocolDecoderException pde = e instanceof ProtocolDecoderException ? (ProtocolDecoderException)e : new ProtocolDecoderException(e);

                if (pde.getHexdump() == null) {
                    // Generate a message hex dump
                    int curPos = in.position();
                    in.position(oldPos);
                    pde.setHexdump(in.getHexDump());
                    in.position(curPos);
                }

                // Fire the exceptionCaught event.
                decoderOut.flush(nextFilter, session);
                nextFilter.exceptionCaught(session, pde);

                // Retry only if the type of the caught exception is
                // recoverable and the buffer position has changed.
                // We check buffer position additionally to prevent an
                // infinite loop.
                if (!(e instanceof RecoverableProtocolDecoderException) || (in.position() == oldPos)) {
                    break;
                }
            }
        }
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        if (writeRequest instanceof EncodedWriteRequest) {
            return;
        }

        if (writeRequest instanceof MessageWriteRequest) {
            MessageWriteRequest wrappedRequest = (MessageWriteRequest) writeRequest;
            nextFilter.messageSent(session, wrappedRequest.getParentRequest());
        } else {
            nextFilter.messageSent(session, writeRequest);
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();

        // Bypass the encoding if the message is contained in a IoBuffer,
        // as it has already been encoded before
        if ((message instanceof IoBuffer) || (message instanceof FileRegion)) {
            nextFilter.filterWrite(session, writeRequest);
            return;
        }

        // Get the encoder in the session
        ProtocolEncoder encoder = factory.getEncoder(session);

        ProtocolEncoderOutput encoderOut = getEncoderOut(session, nextFilter, writeRequest);

        if (encoder == null) {
            throw new ProtocolEncoderException("The encoder is null for the session " + session);
        }

        if (encoderOut == null) {
            throw new ProtocolEncoderException("The encoderOut is null for the session " + session);
        }

        try {
            // Now we can try to encode the response
            encoder.encode(session, message, encoderOut);

            // Send it directly
            Queue<Object> bufferQueue = ((AbstractProtocolEncoderOutput)encoderOut).getMessageQueue();

            // Write all the encoded messages now
            while(!bufferQueue.isEmpty()) {
                Object encodedMessage = bufferQueue.poll();

                if(encodedMessage == null) {
                    break;
                }

                // Flush only when the buffer has remaining.
                if(!(encodedMessage instanceof IoBuffer) || ((IoBuffer)encodedMessage).hasRemaining()) {
                    SocketAddress destination = writeRequest.getDestination();
                    WriteRequest encodedWriteRequest = new EncodedWriteRequest(encodedMessage, null, destination);

                    nextFilter.filterWrite(session, encodedWriteRequest);
                }
            }

            // Call the next filter
            nextFilter.filterWrite(session, new MessageWriteRequest(writeRequest));
        } catch(ProtocolEncoderException e) {
            throw e;
        } catch (Exception e) {
            // Generate the correct exception
            throw new ProtocolEncoderException(e);
        }
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        // Call finishDecode() first when a connection is closed.
        ProtocolDecoder decoder = factory.getDecoder(session);
        ProtocolDecoderOutput decoderOut = getDecoderOut(session, nextFilter);

        try {
            decoder.finishDecode(session, decoderOut);
        } catch(ProtocolDecoderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProtocolDecoderException(e);
        } finally {
            // Dispose everything
            disposeCodec(session);
            decoderOut.flush(nextFilter, session);
        }

        // Call the next filter
        nextFilter.sessionClosed(session);
    }

    private static class EncodedWriteRequest extends DefaultWriteRequest {
        public EncodedWriteRequest(Object encodedMessage, WriteFuture future, SocketAddress destination) {
            super(encodedMessage, future, destination);
        }

        @Override
        public boolean isEncoded() {
            return true;
        }
    }

    private static class MessageWriteRequest extends WriteRequestWrapper {
        public MessageWriteRequest(WriteRequest writeRequest) {
            super(writeRequest);
        }

        @Override
        public Object getMessage() {
            return EMPTY_BUFFER;
        }

        @Override
        public String toString() {
            return "MessageWriteRequest, parent : " + super.toString();
        }
    }

    private static class ProtocolDecoderOutputImpl extends AbstractProtocolDecoderOutput {
        public ProtocolDecoderOutputImpl() {
            // Do nothing
        }

        @Override
        public void flush(NextFilter nextFilter, IoSession session) {
            Queue<Object> messageQueue = getMessageQueue();

            while (!messageQueue.isEmpty()) {
                nextFilter.messageReceived(session, messageQueue.poll());
            }
        }
    }

    private static class ProtocolEncoderOutputImpl extends AbstractProtocolEncoderOutput {
        private final IoSession session;

        private final NextFilter nextFilter;

        /** The WriteRequest destination */
        private final SocketAddress destination;

        public ProtocolEncoderOutputImpl(IoSession session, NextFilter nextFilter, WriteRequest writeRequest) {
            this.session = session;
            this.nextFilter = nextFilter;

            // Only store the destination, not the full WriteRequest.
            destination = writeRequest.getDestination();
        }

        @Override
        public WriteFuture flush() {
            Queue<Object> bufferQueue = getMessageQueue();
            WriteFuture future = null;

            while (!bufferQueue.isEmpty()) {
                Object encodedMessage = bufferQueue.poll();

                if (encodedMessage == null) {
                    break;
                }

                // Flush only when the buffer has remaining.
                if (!(encodedMessage instanceof IoBuffer) || ((IoBuffer) encodedMessage).hasRemaining()) {
                    future = new DefaultWriteFuture(session);
                    nextFilter.filterWrite(session, new EncodedWriteRequest(encodedMessage, future, destination));
                }
            }

            if (future == null) {
                // Creates an empty writeRequest containing the destination
                WriteRequest writeRequest = new DefaultWriteRequest(null, null, destination);
                return DefaultWriteFuture.newNotWrittenFuture(session, new NothingWrittenException(writeRequest));
            }

            return future;
        }
    }

    //----------- Helper methods ---------------------------------------------
    /**
     * Dispose the encoder, decoder, and the callback for the decoded
     * messages.
     */
    private void disposeCodec(IoSession session) {
        // We just remove the two instances of encoder/decoder to release resources
        // from the session
        disposeEncoder(session);
        disposeDecoder(session);

        // We also remove the callback
        disposeDecoderOut(session);
    }

    /**
     * Dispose the encoder, removing its instance from the
     * session's attributes, and calling the associated
     * dispose method.
     */
    private void disposeEncoder(IoSession session) {
        ProtocolEncoder encoder = (ProtocolEncoder) session.removeAttribute(ENCODER);
        if (encoder == null) {
            return;
        }

        try {
            encoder.dispose(session);
        } catch (Exception e) {
            LOGGER.warn("Failed to dispose: {} ({})", encoder.getClass().getName(), encoder);
        }
    }

    /**
     * Dispose the decoder, removing its instance from the
     * session's attributes, and calling the associated
     * dispose method.
     */
    private void disposeDecoder(IoSession session) {
        ProtocolDecoder decoder = (ProtocolDecoder) session.removeAttribute(DECODER);
        if (decoder == null) {
            return;
        }

        try {
            decoder.dispose(session);
        } catch (Exception e) {
            LOGGER.warn("Failed to dispose: {} ({})", decoder.getClass().getName(), decoder);
        }
    }

    /**
     * Return a reference to the decoder callback. If it's not already created
     * and stored into the session, we create a new instance.
     */
    private ProtocolDecoderOutput getDecoderOut(IoSession session, NextFilter nextFilter) {
        //ProtocolDecoderOutput out = (ProtocolDecoderOutput) session.getAttribute(DECODER_OUT);

        if(decOut == null) {
            // Create a new instance, and stores it into the session
            this.decOut = new ProtocolDecoderOutputImpl();
            //session.setAttribute(DECODER_OUT, out);
        }

        return decOut;
    }

    private ProtocolEncoderOutput getEncoderOut(IoSession session, NextFilter nextFilter, WriteRequest writeRequest) {
        //ProtocolEncoderOutput out = (ProtocolEncoderOutput) session.getAttribute(ENCODER_OUT);

        if(encOut == null) {
            // Create a new instance, and stores it into the session
        	this.encOut = new ProtocolEncoderOutputImpl(session, nextFilter, writeRequest);
            //session.setAttribute(ENCODER_OUT, out);
        }

        return encOut;
    }

    /**
     * Remove the decoder callback from the session's attributes.
     */
    private void disposeDecoderOut(IoSession session) {
        //session.removeAttribute(DECODER_OUT);
    }
}
