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

package com.exactpro.sf.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;

/**
 * Work only with printable part of {@link IFieldStructure}
 */
public class HelpUtil {
    public static boolean equals(IFieldStructure first, IFieldStructure second){

        if(first == null || second == null){
            return false;
        }

        if(first == second){
            return true;
        }

        if(!first.getName().equals(second.getName())){
            return false;
        }

        if(first.isComplex() != second.isComplex()){
            return false;
        }

        if(first.isCollection() != second.isCollection()){
            return false;
        }

        if(first.isRequired() != second.isRequired()){
            return false;
        }

        String firstRefName = first.getReferenceName();
        String secondRefName = second.getReferenceName();

        if((firstRefName == null && secondRefName != null) || (firstRefName != null && secondRefName == null)
                || (firstRefName != null && secondRefName != null && !firstRefName.equals(secondRefName))){
            return false;
        }

        //first.isComplex() == second.isComplex() verified above
        if(!first.isComplex() && !first.getJavaType().value().equals(second.getJavaType().value())){
            return false;
        }

        if(!first.getAttributes().equals(second.getAttributes())){
            return false;
        }

        boolean hasChildren = first.isCollection() || first.isComplex();

        if(!hasChildren && !isMapEquals(first.getValues(), second.getValues())){
            return false;
        }

        if(first.isComplex()){
            List<IFieldStructure> firstFields = first.getFields();
            List<IFieldStructure> secondFields = second.getFields();
            if(firstFields.size() != secondFields.size()){
                return false;
            }

            for(int i = 0; i < firstFields.size(); i ++){
                if(!equals(firstFields.get(i),secondFields.get(i))){
                    return false;
                }
            }
        }

        return true;
    }

    public static IFieldStructure getEqualFieldStructure(Collection<IFieldStructure> fieldStructures, IFieldStructure fieldStructure){
        Iterator<IFieldStructure> iterator = fieldStructures.iterator();
        while (iterator.hasNext()){
            IFieldStructure equalField = iterator.next();
            if(equals(equalField, fieldStructure)){
                return equalField;
            }
        }
        return null;
    }

    public static Integer getHashCode(IFieldStructure fieldStructure){
        return getHashCode(fieldStructure, new StringBuilder());
    }

    private static int getHashCode(IFieldStructure fieldStructure, StringBuilder hashCode){
        boolean hasChildren = fieldStructure.isCollection() || fieldStructure.isComplex();

        hashCode.append(fieldStructure.getName());
        hashCode.append(fieldStructure.isComplex()? 1: 0);
        hashCode.append(fieldStructure.isCollection()? 1: 0);
        hashCode.append(fieldStructure.isRequired() ? 1 : 0);
        hashCode.append(fieldStructure.getReferenceName());

        if (!fieldStructure.isComplex()) {
            hashCode.append(fieldStructure.getJavaType().value());
        }

        for (Map.Entry<String, IAttributeStructure> entry : fieldStructure.getAttributes().entrySet()) {
            hashCode.append(entry.getKey());
            hashCode.append(entry.getValue().getValue());
        }


        if(!hasChildren){
            for(Map.Entry<String, IAttributeStructure> entry : fieldStructure.getValues().entrySet()){
                hashCode.append(entry.getKey());
                hashCode.append(entry.getValue().getValue());
            }
        }


        if(fieldStructure.isComplex()){
            for (IFieldStructure field : fieldStructure.getFields()){
                getHashCode(field, hashCode);
            }
        }

        return hashCode.toString().hashCode();
    }

    private static boolean isMapEquals(Map<String, IAttributeStructure> firstMap, Map<String, IAttributeStructure> secondMap){
        if((firstMap == null && secondMap != null) || (firstMap != null && secondMap == null)){
            return false;
        }

        if(firstMap != null && secondMap != null){
            if(firstMap.keySet().size() != secondMap.keySet().size()){
                return false;
            }

            for(Map.Entry<String, IAttributeStructure> firstEntry : firstMap.entrySet()){
                IAttributeStructure secondVal = secondMap.get(firstEntry.getKey());
                if(secondVal == null || !firstEntry.getValue().getValue().equals(secondVal.getValue())){
                    return false;
                }
            }
        }

        return true;
    }
}

