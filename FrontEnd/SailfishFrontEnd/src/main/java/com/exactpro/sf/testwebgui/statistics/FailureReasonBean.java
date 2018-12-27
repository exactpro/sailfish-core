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
package com.exactpro.sf.testwebgui.statistics;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nikita.smirnov
 *
 */
@ManagedBean(name = "flReasonBean")
@ViewScoped
public class FailureReasonBean implements Serializable {

    private static final long serialVersionUID = -1994951382435945613L;

    private static final Logger logger = LoggerFactory.getLogger(FailureReasonBean.class);
    
    private String header;

    private String text;

    @PostConstruct
    public void created() {
        logger.info("FailureReasonBean [{}] constructed", hashCode());
    }
    
    @PreDestroy
    public void destroy() {
        logger.info("FailureReasonBean [{}] destroy", hashCode());
    }

    public void init(String header, String text) {
        logger.info("FailureReasonBean init [{}:{}]", hashCode(), text);
        this.header = header;
        this.text = text;
    }

    public String getHeader() {
        return header;
    }

    public String getText() {
        return text;
    }
}
