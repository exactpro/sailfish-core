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

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.exactpro.sf.common.util.EPSCommonException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SailfishURI implements Comparable<SailfishURI>, Serializable {
    private static final long serialVersionUID = -7920972587219368580L;

    private static final String URI_REGEX = "^(?!$)((?<pluginAlias>\\w+)\\:)?((?<classAlias>\\w+)\\.)?(?<resourceName>\\w+)?$";
    private static final Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

    private final String pluginAlias;
    private final String classAlias;
    private final String resourceName;
    private final int hashPluginAlias;
    private final int hashClassAlias;
    private final int hashResourceName;

    public SailfishURI(String pluginAlias) throws SailfishURIException {
        this(pluginAlias, null);
    }

    public SailfishURI(String pluginAlias, String classAlias) throws SailfishURIException {
        this(pluginAlias, classAlias, null);
    }

    @JsonCreator
    public SailfishURI(@JsonProperty("pluginAlias") String pluginAlias, @JsonProperty("classAlias") String classAlias, @JsonProperty("resourceName") String resourceName) throws SailfishURIException {
        if(pluginAlias == null && classAlias == null && resourceName == null) {
            throw new SailfishURIException("At least one argument must not be null");
        }

        this.pluginAlias = validateElement(pluginAlias);
        this.classAlias = validateElement(classAlias);
        this.resourceName = validateElement(resourceName);
        
        this.hashPluginAlias = getHash(this.pluginAlias);
        this.hashClassAlias = getHash(this.classAlias);
        this.hashResourceName = getHash(this.resourceName);
    }

    public static SailfishURI parse(String uri) throws SailfishURIException {
        return parse(uri, SailfishURIRule.REQUIRE_RESOURCE);
    }

    public static SailfishURI parse(String uri, SailfishURIRule defaultRule, SailfishURIRule... rules) throws SailfishURIException {
        if(uri == null) {
            return null;
        }

        Matcher matcher = URI_PATTERN.matcher(uri);

        if(matcher.matches()) {
            try {
                SailfishURI result = new SailfishURI(matcher.group("pluginAlias"), matcher.group("classAlias"), matcher.group("resourceName"));
                return SailfishURIUtils.checkURI(result, defaultRule, rules);
            } catch(SailfishURIException e) {
                throw new SailfishURIException("Invalid URI: " + uri, e);
            }
        }

        throw new SailfishURIException("Invalid URI: " + uri);
    }

    /**
     * This method behaves as {@link #parse(String)}, but throws an <b>unchecked exception</b> instead of a checked one
     *
     * @throws EPSCommonException
     */
    public static SailfishURI unsafeParse(String uri) {
        try {
            return parse(uri);
        } catch(SailfishURIException e) {
            throw new EPSCommonException(e);
        }
    }

    public SailfishURI merge(SailfishURI uri) throws SailfishURIException {
        Objects.requireNonNull(uri, "URI cannot be null");

        return new SailfishURI(ObjectUtils.defaultIfNull(pluginAlias, uri.pluginAlias),
                               ObjectUtils.defaultIfNull(classAlias, uri.classAlias),
                               ObjectUtils.defaultIfNull(resourceName, uri.resourceName));
    }

    public boolean matches(SailfishURI uri) {
        if(uri == null) {
            return false;
        }

        return matchElements(pluginAlias, uri.pluginAlias, hashPluginAlias, uri.hashPluginAlias) &&
               matchElements(classAlias, uri.classAlias, hashClassAlias, uri.hashClassAlias) &&
               matchElements(resourceName, uri.resourceName, hashResourceName, uri.hashResourceName);
    }

    public String getPluginAlias() {
        return pluginAlias;
    }

    public String getClassAlias() {
        return classAlias;
    }

    public String getResourceName() {
        return resourceName;
    }

    @JsonIgnore
    public boolean isAbsolute() {
        return pluginAlias != null && classAlias != null && resourceName != null;
    }

    private String validateElement(String element) throws SailfishURIException {
        // it's better to use isName method here but we can't because of if/else actions
        if(element == null || SourceVersion.isIdentifier(element)) {
            return element;
        }

        throw new SailfishURIException("Invalid URI element: " + element);
    }

    private boolean matchElements(String element1, String element2, int hash1, int hash2) {
        return element1 == null || element2 == null || 
                (hash1 == hash2 && element1.equalsIgnoreCase(element2));
    }

    private int getHash(String value) {
        return value != null ? value.toLowerCase().hashCode() : 0;
    }
    
    @Override
    public int compareTo(SailfishURI o) {
        if(o == null) {
            return 1;
        }

        CompareToBuilder builder = new CompareToBuilder();

        builder.append(this.pluginAlias, o.pluginAlias, String.CASE_INSENSITIVE_ORDER);
        builder.append(this.classAlias, o.classAlias, String.CASE_INSENSITIVE_ORDER);
        builder.append(this.resourceName, o.resourceName, String.CASE_INSENSITIVE_ORDER);

        return builder.toComparison();
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }

        if(!(o instanceof SailfishURI)) {
            return false;
        }

        SailfishURI that = (SailfishURI)o;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(StringUtils.lowerCase(this.pluginAlias), StringUtils.lowerCase(that.pluginAlias));
        builder.append(StringUtils.lowerCase(this.classAlias), StringUtils.lowerCase(that.classAlias));
        builder.append(StringUtils.lowerCase(this.resourceName), StringUtils.lowerCase(that.resourceName));

        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(StringUtils.lowerCase(pluginAlias));
        builder.append(StringUtils.lowerCase(classAlias));
        builder.append(StringUtils.lowerCase(resourceName));

        return builder.toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if(pluginAlias != null) {
            builder.append(pluginAlias);
            builder.append(':');
        }

        if(classAlias != null) {
            builder.append(classAlias);
            builder.append('.');
        }

        if(resourceName != null) {
            builder.append(resourceName);
        }

        return builder.toString();
    }
}
