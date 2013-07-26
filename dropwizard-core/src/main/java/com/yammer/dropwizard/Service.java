package com.yammer.dropwizard;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.yammer.dropwizard.cli.CheckCommand;
import com.yammer.dropwizard.cli.Cli;
import com.yammer.dropwizard.cli.Command;
import com.yammer.dropwizard.cli.ServerCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.ConfigurationLoader;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.LoggingFactory;
import com.yammer.dropwizard.util.Generics;

import java.util.Collections;
import java.util.List;

/**
 * The base class for Dropwizard services.
 *
 * @param <T> the type of configuration class for this service
 */
public abstract class Service<T extends Configuration> {
    static {
        // make sure spinning up Hibernate Validator doesn't yell at us
        LoggingFactory.bootstrap();
    }

    /**
     * Returns the {@link Class} of the configuration class type parameter.
     *
     * @return the configuration class
     * @see Generics#getTypeParameter(Class, Class)
     */
    public final Class<T> getConfigurationClass() {
        return Generics.getTypeParameter(getClass(), Configuration.class);
    }

    /**
     * Initializes the service bootstrap.
     *
     * @param bootstrap the service bootstrap
     */
    public abstract void initialize(Bootstrap<T> bootstrap);

    /**
     * When the service runs, this is called after the {@link Bundle}s are run. Override it to add
     * providers, resources, etc. for your service.
     *
     * @param configuration the parsed {@link Configuration} object
     * @param environment   the service's {@link Environment}
     * @throws Exception if something goes wrong
     */
    public abstract void run(T configuration, Environment environment) throws Exception;

    /**
     * Asks the service for any custom commands that might need to be set up before the initialize/run cycle.
     *
     * @return List of Command objects to return.
     */
    protected List<? extends Command> getCustomCommands() {
        return Collections.emptyList();
    }

    /**
     * Parses command-line arguments and runs the service. Call this method from a {@code public
     * static void main} entry point in your application.
     *
     * @param arguments the command-line arguments
     * @throws Exception if something goes wrong
     */
    public void run(String[] arguments) throws Exception {
        final Bootstrap<T> bootstrap = buildAndConfigureBootstrap();
        final Cli cli = getCli(bootstrap);

        Command runner = cli.getCommandFromArguments(arguments);
        if (runner instanceof ConfigurationLoader) {
            ConfigurationLoader<T> loader = (ConfigurationLoader<T>)runner;
            bootstrap.setConfiguration(Optional.of(loader.loadConfiguration(cli.parseArgs(arguments))));
        }
        initialize(bootstrap);
        cli.run(arguments);
    }


    /**
     * Build and configure the Bootstrap object.
     *
     * @return Bootstrap object configured with any configured commands.
     */
    private Bootstrap<T> buildAndConfigureBootstrap() {
        Bootstrap<T> bootstrap = new Bootstrap<T>(this);
        registerCommands(getAllCommands(), bootstrap);
        return bootstrap;
    }

    /**
     * Create the Cli instance for this class and bootstrap.
     *
     * @param bootstrap the bootstrap object to use.
     * @return Cli configured for this class and bootstrap.
     */
    private Cli getCli(Bootstrap<T> bootstrap) {
        return new Cli(this.getClass(),bootstrap);
    }


    /**
     * Returns the full list of Command objects this service would like. This is the union of normal Dropwizard
     * commands with the custom commands the Service returns from getCustomCommands.
     *
     * @return List of all Commands the service will use.
     */
    private List<? extends Command> getAllCommands() {
        ImmutableList<? extends Command> allCommands = ImmutableList.<Command>builder()
                .addAll(getDropwizardCommands())
                .addAll(getCustomCommands())
                .build();
        return allCommands;
    }

    /**
     * Returns the list of Command objects Dropwizard supports by default.
     *
     * @return List of the Command objects provided by default.
     */
    private List<? extends Command> getDropwizardCommands() {
        return ImmutableList.<Command>of(new ServerCommand<T>(this), new CheckCommand<T>(this));
    }


    /**
     * Registers the commands in the list with the passed-in bootstrap.
     *
     * @param commands Command objects to register.
     * @param bootstrap the Bootstrap object to use for registering the Command objects.
     */
    private void registerCommands(List<? extends Command> commands, Bootstrap<T> bootstrap) {
        for (Command command : commands) {
            bootstrap.addCommand(command);
        }
    }
}
