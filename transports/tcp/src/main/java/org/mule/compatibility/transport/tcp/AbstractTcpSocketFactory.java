/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.runtime.core.util.MapUtils;

import java.io.IOException;
import java.net.Socket;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a client socket using the socket address extracted from the endpoint.  Addtional
 * socket parameters will also be set from the connector
 */
public abstract class AbstractTcpSocketFactory implements KeyedPoolableObjectFactory
{

    /**
     * logger used by this class
     */
    private static final Logger logger = LoggerFactory.getLogger(TcpSocketFactory.class);

    private int connectionTimeout = Connector.INT_VALUE_NOT_SET;

    @Override
    public Object makeObject(Object key) throws Exception
    {
        TcpSocketKey socketKey = (TcpSocketKey) key;

        Socket socket = createSocket(socketKey);
        socket.setReuseAddress(true);

        TcpConnector connector = socketKey.getConnector();
        connector.configureSocket(TcpConnector.CLIENT, socket);

        return socket;
    }

    protected abstract Socket createSocket(TcpSocketKey key) throws IOException;

    @Override
    public void destroyObject(Object key, Object object) throws Exception
    {
        Socket socket = (Socket) object;
        if(!socket.isClosed())
        {
            socket.close();
        }
    }

    @Override
    public boolean validateObject(Object key, Object object)
    {
        Socket socket = (Socket) object;
        return !socket.isClosed();
    }

    @Override
    public void activateObject(Object key, Object object) throws Exception
    {
        // cannot really activate a Socket
    }

    @Override
    public void passivateObject(Object key, Object object) throws Exception
    {
        TcpSocketKey socketKey = (TcpSocketKey) key;

        boolean keepSocketOpen = MapUtils.getBooleanValue(socketKey.getEndpoint().getProperties(),
            TcpConnector.KEEP_SEND_SOCKET_OPEN_PROPERTY, socketKey.getConnector().isKeepSendSocketOpen());
        Socket socket = (Socket) object;

        if (!keepSocketOpen)
        {
            try
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
            catch (IOException e)
            {
                logger.debug("Failed to close socket after dispatch: " + e.getMessage());
            }
        }
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }
}
