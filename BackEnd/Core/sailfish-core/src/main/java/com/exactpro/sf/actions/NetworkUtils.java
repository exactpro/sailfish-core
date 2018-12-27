/*******************************************************************************
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

package com.exactpro.sf.actions;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;

@MatrixUtils
@ResourceAliases({"NetworkUtils"})
public class NetworkUtils extends AbstractCaller {

    @Description("Returns the IPv4 for the specified network interface name.<br>" +
            "<b>interfaceName</b> - the network interface name, the IP of which will be returned<br>" +
            "Example:<br/>" +
            "#{currentIpAddress(interfaceName)}")
    @UtilityMethod
    public String currentIpAddress(String interfaceName) {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
            if (networkInterface == null) {
                throw new EPSCommonException("Unknown interface name: " + interfaceName);
            }
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            InetAddress address;
            while (addresses.hasMoreElements()) {
                address = addresses.nextElement();
                if (address instanceof Inet4Address) {
                    return address.getHostAddress();
                }
            }
            throw new EPSCommonException(String.format("Network interface %s has not IPv4", interfaceName));
        } catch (Exception e) {
            throw new EPSCommonException(String.format("Can't get interface [%s]", interfaceName), e);
        }
    }

    @Description("Returns the IP address from which the remote host can be reached during the expected timeout.<br>" +
            "<b>remoteAddress</b> - remote host address<br>" +
            "<b>remotePort</b> - remote host port<br>" +
            "<b>timeout</b> - the expected time in milliseconds that it takes to get the answer from the remote host<br>" +
            "Example:<br/>" +
            "#{currentIpAddress(remoteAddress, remotePort, timeout)}")
    @UtilityMethod
    public String currentIpAddress(String remoteAddress, int remotePort, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(remoteAddress, remotePort), timeout);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            throw new EPSCommonException(String.format("Can't reach %s:%d during %d", remoteAddress, remotePort, timeout), e);
        }
    }
}
