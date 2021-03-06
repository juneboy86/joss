package org.javaswift.joss.command.shared.identity.access;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EndPoint {

    public String adminURL;

    public String region;

    public String internalURL;

    public String publicURL;

    public String id;

}
