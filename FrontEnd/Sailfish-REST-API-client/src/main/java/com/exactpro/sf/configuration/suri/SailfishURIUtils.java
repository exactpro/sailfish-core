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
package com.exactpro.sf.configuration.suri;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SailfishURIUtils {
    //FIXME: use "resolve once" strategy in managers instead of matching URIs every time
    private static final Logger LOGGER = LoggerFactory.getLogger(SailfishURIUtils.class);

	/**
     * Finds a value in multimap with key matching specified URI
     *
     *  @param  uri         URI for matching
     *  @param  multimap    multimap with values
     *  @param  defaultRule default URI rule
     *  @param  rules       array of URI rules
     *
     *  @return found value or {@code null}
     */
    public static <T> T getMatchingValue(SailfishURI uri, Multimap<SailfishURI, T> multimap, SailfishURIRule defaultRule, SailfishURIRule... rules) {
		Objects.requireNonNull(multimap, "multimap cannot be null");
        checkURI(uri, defaultRule, rules);

        Collection<T> c = getMatchingValue(uri, multimap.asMap(), defaultRule, rules);

        if(c == null || c.isEmpty()) {
            return null;
        }

        return c.iterator().next();
    }

    /**
     * Finds a value in map with key matching specified URI
     *
     *  @param  uri         URI for matching
     *  @param  map         map with values
     *  @param  defaultRule default URI rule
     *  @param  rules       array of URI rules
     *
     *  @return found value or {@code null}
     */
    public static <T> T getMatchingValue(SailfishURI uri, Map<SailfishURI, T> map, SailfishURIRule defaultRule, SailfishURIRule... rules) {
		Objects.requireNonNull(map, "map cannot be null");
        checkURI(uri, defaultRule, rules);

        for(SailfishURI key : map.keySet()) {
            if(uri.matches(key)) {
                LOGGER.debug("Matching: {} -> {}", uri, key);
                return map.get(key);
            }
        }

        return null;
    }

    /**
     * Finds values in multimap with keys matching specified URI
     *
     *  @param  uri         URI for matching
     *  @param  multimap    multimap with values
     *  @param  defaultRule default URI rule
     *  @param  rules       array of URI rules
     *
     *  @return set of found values
     */
    public static <T> Set<T> getMatchingValues(SailfishURI uri, Multimap<SailfishURI, T> multimap, SailfishURIRule defaultRule, SailfishURIRule... rules) {
		Objects.requireNonNull(multimap, "multimap cannot be null");
        checkURI(uri, defaultRule, rules);

        Set<T> values = new HashSet<T>();

        for(Collection<T> c : getMatchingValues(uri, multimap.asMap(), defaultRule, rules)) {
            values.addAll(c);
        }

        return values;
    }

    /**
     * Finds values in map with keys matching specified URI
     *
     *  @param  uri         URI for matching
     *  @param  map         map with values
     *  @param  defaultRule default URI rule
     *  @param  rules       array of URI rules
     *
     *  @return set of found values
     */
    public static <T> Set<T> getMatchingValues(SailfishURI uri, Map<SailfishURI, T> map, SailfishURIRule defaultRule, SailfishURIRule... rules) {
		Objects.requireNonNull(map, "map cannot be null");
        checkURI(uri, defaultRule, rules);

        Set<T> values = new HashSet<T>();

        for(SailfishURI key : map.keySet()) {
            if(uri.matches(key)) {
                LOGGER.debug("Matching: {} -> {}", uri, key);
                values.add(map.get(key));
            }
        }

        return values;
    }

    public static SailfishURI checkURI(SailfishURI uri, SailfishURIRule defaultRule, SailfishURIRule... rules) {
        Objects.requireNonNull(uri, "uri cannot be null");
        Objects.requireNonNull(defaultRule, "defaultRule cannot be null");
        Objects.requireNonNull(rules, "rules cannot be null");

        for(SailfishURIRule rule : ArrayUtils.add(rules, defaultRule)) {
            if(!rule.check(uri)) {
                throw new IllegalArgumentException(String.format("Invalid URI: %s (%s)", uri, rule.getDescription()));
            }
        }

        return uri;
    }

    public static String sanitize(String value) {
        if(value != null) {
            return value.replaceAll("[^\\w:.]", "_");
        }

        return null;
    }
}
