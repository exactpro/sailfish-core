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
package com.exactpro.sf.services.fast;

import org.openfast.FieldValue;
import org.openfast.debug.Trace;
import org.openfast.template.Field;
import org.openfast.template.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.HexDumper;

/**
 * @author nikita.smirnov
 *
 */
public class LoggingTrace implements Trace {

    private static final Logger logger = LoggerFactory.getLogger(LoggingTrace.class);
    
    @Override
    public void groupStart(Group group) {
        logger.debug("Start group {}", group);
    }

    @Override
    public void groupEnd() {
        logger.debug("End group");
    }

    @Override
    public void field(Field field, FieldValue value, FieldValue encoded, byte[] encoding, int pmapIndex) {
        if (logger.isDebugEnabled()) {
            logger.debug("Field '{}', PMap index '{}', value '{}', encoded '{}' hex '{}'", field, pmapIndex, value, encoded, HexDumper.getHexdump(encoding));
        }
    }

    @Override
    public void pmap(byte[] pmap) {
        if (logger.isDebugEnabled()) {
            logger.debug("PMap hex {}", HexDumper.getHexdump(pmap));
        }
    }
    
}
