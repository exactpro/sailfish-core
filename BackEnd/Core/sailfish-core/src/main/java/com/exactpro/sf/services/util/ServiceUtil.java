/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.util;

import static com.exactpro.sf.services.ServiceEvent.Level.ERROR;
import static com.exactpro.sf.services.ServiceEvent.Level.INFO;
import static com.exactpro.sf.services.ServiceEvent.Type.convert;
import static com.exactpro.sf.services.ServiceEventFactory.createStatusUpdateEvent;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.splitByWholeSeparator;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.messages.service.ErrorMessage;
import com.exactpro.sf.messages.service.ServiceMessage;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceEvent.Level;
import com.exactpro.sf.services.ServiceStatus;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLong;

public class ServiceUtil {
    private static final Logger logger = LoggerFactory.getLogger(ServiceUtil.class);

    private static final byte BYTE_ZERO = (byte) 0;
    private static final long START_TIMEOUT = 10000L;
    public static final String ALIAS_PREFIX = "alias://";
    public static final String SERVICE_DICTIONARY_NAMESPACE = "Service";
    public static SailfishURI SERVICE_DICTIONARY_URI;

    static {
        try {
            SERVICE_DICTIONARY_URI = new SailfishURI(IVersion.GENERAL, null, "Service");
        } catch (SailfishURIException e) {
            logger.error("Can not create SailfishURI for '{}' dictionary", SERVICE_DICTIONARY_NAMESPACE);
        }
    }

    private ServiceUtil() {
    }

    public static void changeStatus(IService service, IServiceMonitor monitor, ServiceStatus status, String message, Throwable e) {
        boolean error = status == ServiceStatus.ERROR;
        Level level = error ? ERROR : INFO;
        Throwable throwable = error ? e : null;
        ServiceEvent event = createStatusUpdateEvent(service.getServiceName(), level, convert(status), message, "", throwable);

        if (monitor != null) {
            monitor.onEvent(event);
        }

        if (e != null && logger.isErrorEnabled()) {
            logger.error("Service {} have got error {}", service, e.getMessage(), e);
        }

        logger.info("Service {} status is {}", service, status);
    }

    public static BigDecimal convertFromUint64(byte[] array) {

    	if (array.length == 8 && array[0] != BYTE_ZERO) {
    		array = normalisate(array, 9);
    	}
		BigInteger bigInt = new BigInteger(array);

		return new BigDecimal(bigInt);
	}

    public static double convertFromUint64(byte[] array, long divider) {

		return convertFromUint64(array, UnsignedLong.valueOf(divider));
	}

    public static double convertFromUint64(byte[] array, UnsignedLong divider) {

		array = normalisate(array, 8);
		UnsignedLong unLong = UnsignedLong.fromLongBits(Longs.fromByteArray(array));

		UnsignedLong div = unLong.dividedBy(divider);
		UnsignedLong mod =  unLong.mod(divider);

		double result = mod.doubleValue() / divider.longValue();
		return result + div.longValue();
	}

    public static double divide(long dividend, long divider) {
        long div = dividend / divider;
        long mod =  dividend % divider;

        double result = (double)mod / divider;
        return result + div;
    }

    public static double divideDouble(int dividend, int divider) {
        int div = dividend / divider;
        int mod = dividend % divider;

        double result = (double)mod / divider;
        return result + div;
    }

    public static float divideFloat(int dividend, int divider) {
        int div = dividend / divider;
        int mod = dividend % divider;

        float result = (float)mod / divider;
        return result + div;
    }

    public static float divide(int dividend, int divider) {
        return divideFloat(dividend, divider);
    }

    public static byte[] normalisate(byte[] array, int size) {
        if(array == null) {
            throw new EPSCommonException("Array is null");
        }
    	if (size < 0) {
    		throw new EPSCommonException("Size cannot be negative");
    	}

		if (array.length > size) {
			int from = array.length - size;

			for (int i = from - 1; i > -1; i--) {
                if(array[i] != BYTE_ZERO) {
					throw new EPSCommonException("Massive compression does not execute without data loss");
				}
			}
			return Arrays.copyOfRange(array, from, array.length);
		} else if (array.length < size) {
			byte[] result = new byte[size];
			for (int i = array.length -1, j = size - 1; i > -1; i--, j--) {
				result[j] = array[i];
			}
			return result;
		}
		return array;
	}

    public static String loadStringFromAlias(IDataManager dataManager, String value, String delimiter) throws SailfishURIException {
    	if(!StringUtils.startsWith(value, ALIAS_PREFIX)) {
    		return value;
    	}

    	String alias = value.substring(ALIAS_PREFIX.length());
    	InputStream is = dataManager.getDataInputStream(SailfishURI.parse(alias));
		StringBuilder sb = new StringBuilder();

		try(Scanner sc = new Scanner(is)) {
		    while(sc.hasNextLine()) {
			    sb.append(sc.nextLine());

    			if(sc.hasNextLine()) {
    			    sb.append(delimiter);
		        }
    		}
		}

    	return sb.toString();
    }

    /**
     * Parses comma separated values into a set by splitting input by a delimiter. Values produced by splitting are then trimmed.
     * Values can be parsed either from an input string itself or from a text data file loaded via alias specified in the input string.
     * When input is loaded from the file, file's lines are first joined into a string delimited by specified delimiter and then this string is parsed as usual.
     * @param dataManager data manager used to load a text file if input is an alias
     * @param value string with comma separated values or an alias (alias://data) to a text file with values
     * @param delimiter delimiter to split values by
     * @return
     * @throws SailfishURIException
     */
    public static Set<String> loadValuesFromAlias(IDataManager dataManager, String value, String delimiter) throws SailfishURIException {
        if (isBlank(value)) {
            return Collections.emptySet();
        }

        String[] values = splitByWholeSeparator(loadStringFromAlias(dataManager, value, delimiter), delimiter);

        return stream(values)
                .map(StringUtils::stripToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static void startServices(List<String> serviceNames) throws InterruptedException {
        logger.info("List services for starting {}", serviceNames);
        Collections.sort(serviceNames);

        IConnectionManager conManager = SFLocalContext.getDefault().getConnectionManager();
        List<IService> services = new ArrayList<>();

        for (String serviceName : serviceNames) {
            IService service = conManager.getService(ServiceName.parse(serviceName));

            if (service != null) {
                ServiceDescription serviceDescr = conManager.getServiceDescription(service.getServiceName());
                IServiceSettings serviceSettings = serviceDescr.getSettings();

                try {
                    if (service.getStatus() != ServiceStatus.STARTED && service.getStatus() != ServiceStatus.WARNING) {
                        if(serviceSettings.getWaitingTimeBeforeStarting() != 0 ){
                            Thread.sleep(serviceSettings.getWaitingTimeBeforeStarting());
                        }

                        conManager.initService(service.getServiceName(), null).get();
                        conManager.startService(service.getServiceName(), null);

                        long expectedTimeOfStarting = serviceSettings.getExpectedTimeOfStarting();

                        if(expectedTimeOfStarting != 0) {
                            Thread.sleep(expectedTimeOfStarting);

                            if (service.getStatus() != ServiceStatus.STARTED && service.getStatus() != ServiceStatus.WARNING) {
                                logger.error("Service {} is not started. Status {}", serviceName, service.getStatus());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                services.add(service);
            }
        }

        for(IService service : services) {
            long waitUntil = System.currentTimeMillis() + START_TIMEOUT;

            while(waitUntil > System.currentTimeMillis()) {
                if (service.getStatus() == ServiceStatus.STARTED || service.getStatus() == ServiceStatus.ERROR) {
                    break;
                }

                Thread.sleep(1);
            }

            if (service.getStatus() != ServiceStatus.STARTED && service.getStatus() != ServiceStatus.WARNING) {
                throw new EPSCommonException("Service " + service.getName() + " is not started.");
            }
        }
    }

    public static void disposeServices(List<String> services) throws InterruptedException {
        logger.info("List services for stopping {}", services);

        List<IService> serviceList = new ArrayList<>();
        IConnectionManager conManager = SFLocalContext.getDefault().getConnectionManager();
        for (String serviceName : services) {
            IService service = conManager.getService(ServiceName.parse(serviceName));
            if (service != null) {
                serviceList.add(service);
                conManager.disposeService(service.getServiceName(), null);
            }
        }

        if (serviceList != null) {
            long endTime = System.currentTimeMillis() + (serviceList.size() * 2000);
            boolean allServicesDisposed = false;

            while (endTime > System.currentTimeMillis() && !allServicesDisposed)
            {
                Thread.sleep(100);

                allServicesDisposed = true;
                Iterator<IService> iterator = serviceList.iterator();

                while (allServicesDisposed && iterator.hasNext()) {
                    IService service = iterator.next();
                    if (service.getStatus() == ServiceStatus.DISPOSED
                            || service.getStatus() == ServiceStatus.ERROR) {
                        iterator.remove();
                    } else {
                        allServicesDisposed = false;
                    }
                }
            }

            if (!serviceList.isEmpty()) {
                StringBuilder builder = new StringBuilder("Services [");
                for (IService service : serviceList) {
                    builder.append(service.getName());
                    builder.append(", ");
                }
                builder.delete(builder.length() - 2, builder.length());
                builder.append("] are not stopped.");
                logger.warn("{}", builder);
            }
        }
    }

    public static IMessage createServiceMessage(String text, String from, String to, ServiceInfo serviceInfo, IMessageFactory factory) {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setText(text);

        return fillMetadata(serviceMessage.getMessage(), null, from, to, serviceInfo);
    }

    public static IMessage createErrorMessage(String cause, String from, String to, ServiceInfo serviceInfo, IMessageFactory factory) {
        return createErrorMessage(cause, null, from, to, serviceInfo, factory);
    }

    public static IMessage createErrorMessage(String cause, byte[] rawMessage, String from, String to, ServiceInfo serviceInfo, IMessageFactory factory) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setCause(cause);

        return fillMetadata(errorMessage.getMessage(), rawMessage, from, to, serviceInfo);
    }

    private static IMessage fillMetadata(IMessage message, byte[] rawMessage, String from, String to, ServiceInfo serviceInfo) {
        MsgMetaData metaData = message.getMetaData();

        metaData.setAdmin(true);
        metaData.setFromService(from);
        metaData.setToService(to);
        metaData.setRawMessage(rawMessage);
        metaData.setServiceInfo(serviceInfo);
        metaData.setDictionaryURI(SERVICE_DICTIONARY_URI);

        return message;
    }
}
