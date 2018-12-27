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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

import com.exactpro.sf.aml.generator.AggregateAlert.AlertBuilder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class AlertCollector implements Serializable {
	
    private static final long serialVersionUID = 2247021046408551093L;
    
    private final SetMultimap<AlertType, Alert> alerts = HashMultimap.create();
    
    public Collection<AggregateAlert> aggregate(AlertType type) {
        
        Map<String, SetMultimap<String, Alert>> map = new HashMap<>();
        for (Alert alert : getAlerts(type)) {
            map.computeIfAbsent(alert.getMessage(), key -> HashMultimap.create())
                .put(alert.getColumn(), alert);
        }
        
        List<AggregateAlert> alerts = new ArrayList<>();
        for (Entry<String, SetMultimap<String, Alert>> entry : map.entrySet()) {
            SetMultimap<String, Alert> columnToAlert = entry.getValue();
            for (String column : columnToAlert.keySet()) {
                AlertBuilder builder = AggregateAlert.builder()
                        .setMessage(entry.getKey())
                        .setColumn(column);
                for (Alert alert : columnToAlert.get(column)) {
                    builder.process(alert)
                        .addLine(alert.getLine())
                        .addUid(alert.getUid());
                }
                
                alerts.add(builder.build());
            }
        }
        return alerts;
    }
    
    public boolean contains(AlertType type, String message) {
        return getAlerts(type).stream()
                .anyMatch(alert -> Objects.equals(alert.getMessage(), message));
    }
    
    public void add(Alert alert) {
        alerts.put(alert.getType(), alert);
    }

    public void add(Stream<Alert> alerts) {
        alerts.forEach(this::add);
    }
    
    public void add(AlertCollector alertCollector) {
        this.alerts.putAll(alertCollector.alerts);
    }

    public Collection<Alert> getAlerts(AlertType type) {
        if (type != null) {
            return this.alerts.get(type);
        }
        
        return this.alerts.values();
    }
    
    public Collection<Alert> getAlerts() {
        return getAlerts(null);
    }

    public int getCount(AlertType type) {
        if (type != null) {
            return this.alerts.get(type).size();
        }
        
        return this.alerts.values().size();
    }
    
    public int getCount() {
        return getCount(null);
    }
}
