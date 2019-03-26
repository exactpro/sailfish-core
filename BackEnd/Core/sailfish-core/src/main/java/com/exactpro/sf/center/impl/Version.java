/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.center.impl;

import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.exactpro.sf.center.impl.Version.VersionComponent.BUILD;
import static com.exactpro.sf.center.impl.Version.VersionComponent.MAINTENANCE;
import static com.exactpro.sf.center.impl.Version.VersionComponent.MAJOR;
import static com.exactpro.sf.center.impl.Version.VersionComponent.MINOR;

public class Version extends AbstractVersion {

    private static final String VERSION_REGEX = Stream.of(VersionComponent.values())
        .map(Enum::name)
        .map(value -> "(?<" + value + ">\\d+)")
        .collect(Collectors.joining("\\.", "^", "$"));
    private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEX);

    private final int major;
    private final int minor;
    private final int maintenance;
    private final int build;
    private final String alias;
    private final String branch;
    private final String artifactName;

    protected Version(int major, int minor, int maintenance, int build, String alias, String branch, String artifactName) {
        this.major = major;
        this.minor = minor;
        this.maintenance = maintenance;
        this.build = build;
        this.alias = alias;
        this.branch = branch;
        this.artifactName = artifactName;
    }

    /**
     * @param versionFile - file with VERSION content from plugin
     */
    public static Version loadVersion(File versionFile) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(versionFile))) {
            Properties properties = new Properties();

            properties.load(inputStream);
            String version = Objects.requireNonNull(properties.getProperty("version"), "'version' property is missing");

            Matcher matcher = VERSION_PATTERN.matcher(version);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("'version' property has incorrect format. Actual '" + version + "', Expected '" + VERSION_REGEX + "'");
            }

            return new Version(
                MAJOR.extractComponent(matcher),
                MINOR.extractComponent(matcher),
                MAINTENANCE.extractComponent(matcher),
                BUILD.extractComponent(matcher),
                Objects.requireNonNull(properties.getProperty("plugin_alias"), "'plugin_alias' property is skipped"),
                Objects.requireNonNull(properties.getProperty("branch"), "'branch' property is skipped"),
                Objects.requireNonNull(properties.getProperty("name"), "'name' property is skipped")
            );
        }
    }

    @Override
    public int getMajor() {
        return major;
    }

    @Override
    public int getMinor() {
        return minor;
    }

    @Override
    public int getMaintenance() {
        return maintenance;
    }

    @Override
    public int getBuild() {
        return build;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getBranch() {
        return branch;
    }

    @Override
    public String getArtifactName() {
        return artifactName;
    }

    /**
     * Ordered enum of version components
     */
    protected enum VersionComponent {
        MAJOR,
        MINOR,
        MAINTENANCE,
        BUILD;

        int extractComponent(Matcher matcher) {
            return Integer.parseInt(matcher.group(name()));
        }
    }
}