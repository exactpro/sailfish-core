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
package com.exactpro.sf.scriptrunner.actionmanager;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.aml.AMLAction;
import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionManagerException;

public class ActionRequirements {
    private Set<String> requiredColumns = new HashSet<String>();
    private Set<String> deprecatedColumns = new HashSet<String>();

    protected ActionRequirements(CommonColumns commonColumns, CustomColumns customColumns) {
        if(commonColumns != null) {
            for(CommonColumn column : commonColumns.value()) {
                addColumn(column.value().getName(), column.required(), column.deprecated());
            }
        }

        if(customColumns != null) {
            for(CustomColumn column : customColumns.value()) {
                addColumn(column.value(), column.required(), column.deprecated());
            }
        }
    }

    private void addColumn(String name, boolean required, boolean deprecated) {
        if(requiredColumns.contains(name) || deprecatedColumns.contains(name)) {
            throw new ActionManagerException("Duplicate annotation for column: " + name);
        }

        if(required) {
            requiredColumns.add(name);
        }

        if(deprecated) {
            deprecatedColumns.add(name);
        }
    }

    public Set<String> getRequiredColumns() {
        return requiredColumns;
    }

    public Set<String> getDeprecatedColumns() {
        return deprecatedColumns;
    }

     public void checkRequirements(AMLAction action, AlertCollector alertCollector) {
         Set<String> definedColumns = action.getDefinedColumns();

         for(String column : requiredColumns) {
             if(!definedColumns.contains(column)) {
                 alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Column is missing for action: " + action.getActionURI()));
             }
         }

         for(String column : deprecatedColumns) {
             if(definedColumns.contains(column)) {
                 alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Column is deprecated for action: " + action.getActionURI(), AlertType.WARNING));
             }
         }
     }

     @Override
     public boolean equals(Object o) {
         if(!(o instanceof ActionRequirements)) {
             return false;
         }

         ActionRequirements that = (ActionRequirements)o;
         EqualsBuilder builder = new EqualsBuilder();

         builder.append(this.requiredColumns, that.requiredColumns);
         builder.append(this.deprecatedColumns, that.deprecatedColumns);

         return builder.isEquals();
     }

     @Override
     public int hashCode() {
         HashCodeBuilder builder = new HashCodeBuilder();

         builder.append(requiredColumns);
         builder.append(deprecatedColumns);

         return builder.toHashCode();
     }

     @Override
     public String toString() {
         ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

         builder.append("requiredColumns", requiredColumns);
         builder.append("deprecatedColumns", deprecatedColumns);

         return builder.toString();
     }
}

