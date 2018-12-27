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
package com.exactpro.sf.aml.generator;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.aml.scriptutil.IMessagePropertyHandler;
import com.exactpro.sf.aml.scriptutil.MapPropertyHandler;
import com.exactpro.sf.configuration.suri.SailfishURI;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.math.MathProcessor;
import org.mvel2.optimizers.OptimizerFactory;

import java.util.Map;

public class MVELInitializer {
    private ParserContext ctx;
	private static volatile MVELInitializer singleton;

	private MVELInitializer() {
		MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true; // forcing to use PropertyHandler
        MathProcessor.COMPARE_WITH_PRECISION = true;
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE); //Workaround for MVEL-291 "Custom property handler optimization fails with RuntimeException unable to compileShared"
		PropertyHandlerFactory.registerPropertyHandler(IMessage.class, new IMessagePropertyHandler());
		PropertyHandlerFactory.registerPropertyHandler(Map.class, new MapPropertyHandler());

		ctx = new ParserContext();
		ctx.addImport(java.time.LocalDateTime.class);
		ctx.addImport(java.time.LocalDate.class);
		ctx.addImport(java.time.LocalTime.class);
		ctx.addImport(java.math.BigDecimal.class);
		ctx.addImport(SailfishURI.class);
		ctx.addImport(java.util.Objects.class);
	}

	@Override
	public void finalize() {
		PropertyHandlerFactory.unregisterPropertyHandler(IMessage.class);
		PropertyHandlerFactory.unregisterPropertyHandler(MapMessage.class);
	}

	public ParserContext getCtx() {
		return ctx;
	}

	public static MVELInitializer getInstance() {
		if(singleton == null) {
			singleton = new MVELInitializer();
		}

		return singleton;
	}
}
