/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.cdec.artifacts;

import com.codenvy.cdec.ArtifactNotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallManagerArtifact extends AbstractArtifact {
    public static final String NAME = "installation-manager";

    @Inject
    public InstallManagerArtifact() {
        super(NAME);
    }

    @Override
    public void install(Path pathToBinaries) throws IOException {
        try {
            unpack(pathToBinaries);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        // TODO
//        restart()
    }

    // TODO remove duplicate of getCurrentVersion(String accessToken)
    @Override
    public String getCurrentVersion() throws IOException {
        return getCurrentVersion(null);
    }
    
    @Override
    public String getCurrentVersion(String accessToken) throws IOException {
        try (InputStream in = Artifact.class.getClassLoader().getResourceAsStream("codenvy/BuildInfo.properties")) {
            Properties props = new Properties();
            props.load(in);

            if (props.containsKey("version")) {
                return (String)props.get("version");
            } else {
                throw new ArtifactNotFoundException(this.getName());
            }
        }
    }

    @Override
    public boolean isValidSubscriptionRequired() {
        return false;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        URL location = InstallManagerArtifact.class.getClass().getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(location.toURI());
    }
}