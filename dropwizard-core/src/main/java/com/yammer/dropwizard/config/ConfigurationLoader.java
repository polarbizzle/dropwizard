package com.yammer.dropwizard.config;

import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;

/**
 * Defines an interface for objects that can create Configuration objects from the command-line arguments.
 *
 * @author derrick.schneider@opower.com
 */
public interface ConfigurationLoader<T extends Configuration> {

    /**
     * Given a set of command-line arguments parsed into a Namespace, create a Configuration object.
     *
     * This is for scenarios where the service wants to pass in something that can create the Configuration
     * object, which is then passed to a separate Service.initialize method.
     *
     * @param namespace the result of the parsed arguments.
     * @return
     * @throws IOException if there's an IO error.
     * @throws ConfigurationException if there's an error creating the Configuration itself
     */
    T loadConfiguration(Namespace namespace) throws IOException, ConfigurationException;

}
