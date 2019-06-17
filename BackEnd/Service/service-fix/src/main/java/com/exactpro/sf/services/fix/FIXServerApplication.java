/******************************************************************************
 * Copyright (c) 2009-2018, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactpro.sf.services.fix;

import com.exactpro.sf.services.ISession;

public interface FIXServerApplication extends IQuickfixApplication {

    ISession getServerSession();
}
