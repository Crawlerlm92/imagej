/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2013 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.command;

import imagej.module.Module;
import imagej.module.ModuleInfo;
import imagej.module.ModuleService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.log.LogService;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.plugin.event.PluginsAddedEvent;
import org.scijava.plugin.event.PluginsRemovedEvent;
import org.scijava.service.Service;
import org.scijava.util.ListUtils;

/**
 * Default service for working with {@link Command}s. Available commands are
 * obtained from the plugin service. Loading of the actual command classes can
 * be deferred until a particular command's first execution.
 * 
 * @author Curtis Rueden
 * @see Command
 */
@Plugin(type = Service.class)
public class DefaultCommandService extends AbstractPTService<Command> implements
	CommandService
{

	@Parameter
	private LogService log;

	@Parameter
	private EventService eventService;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private ModuleService moduleService;

	/** Mapping from vanilla plugin metadata to command metadata objects. */
	private HashMap<PluginInfo<?>, CommandInfo> commandMap;

	// -- CommandService methods --

	@Override
	public EventService getEventService() {
		return eventService;
	}

	@Override
	public PluginService getPluginService() {
		return pluginService;
	}

	@Override
	public ModuleService getModuleService() {
		return moduleService;
	}

	@Override
	public List<CommandInfo> getCommands() {
		return getCommandsOfType(Command.class);
	}

	@Override
	public <CT extends Command> List<CommandInfo> getCommands(
		final List<PluginInfo<CT>> plugins)
	{
		final List<CommandInfo> commands = getCommandsUnknown(downcast(plugins));
		return commands;
	}

	@Override
	public <CT extends Command> List<CommandInfo> getCommandsOfType(
		Class<CT> type)
	{
		return getCommands(pluginService.getPluginsOfType(type));
	}

	@Override
	public <C extends Command> CommandInfo getCommand(
		final Class<C> commandClass)
	{
		return ListUtils.first(getCommandsOfClass(commandClass));
	}

	@Override
	public CommandInfo getCommand(final String className) {
		return ListUtils.first(getCommandsOfClass(className));
	}

	@Override
	public <C extends Command> List<CommandInfo> getCommandsOfClass(
		final Class<C> commandClass)
	{
		final List<PluginInfo<Command>> plugins =
			pluginService.getPluginsOfClass(commandClass, Command.class);
		final List<CommandInfo> commands = getCommands(plugins);
		return commands;
	}

	@Override
	public List<CommandInfo> getCommandsOfClass(final String className)
	{
		final List<PluginInfo<SciJavaPlugin>> plugins =
			pluginService.getPluginsOfClass(className);
		final List<CommandInfo> commands = getCommandsUnknown(downcast(plugins));
		return commands;
	}

	@Override
	public Future<CommandModule> run(final String className,
		final boolean process, final Object... inputs)
	{
		return run(getOrCreate(className), process, inputs);
	}

	@Override
	public Future<CommandModule> run(final String className,
		final boolean process, final Map<String, Object> inputMap)
	{
		return run(getOrCreate(className), process, inputMap);
	}

	@Override
	public <C extends Command> Future<CommandModule> run(
		final Class<C> commandClass, final boolean process, final Object... inputs)
	{
		return run(getOrCreate(commandClass), process, inputs);
	}

	@Override
	public <C extends Command> Future<CommandModule> run(
		final Class<C> commandClass, final boolean process,
		final Map<String, Object> inputMap)
	{
		return run(getOrCreate(commandClass), process, inputMap);
	}

	@Override
	public Future<CommandModule> run(final CommandInfo info,
		final boolean process, final Object... inputs)
	{
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final Future<CommandModule> future =
			(Future) moduleService.run(info, process, inputs);
		return future;
	}

	@Override
	public Future<CommandModule> run(final CommandInfo info,
		final boolean process, final Map<String, Object> inputMap)
	{
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final Future<CommandModule> future =
			(Future) moduleService.run(info, process, inputMap);
		return future;
	}

	// -- PTService methods --

	@Override
	public Class<Command> getPluginType() {
		return Command.class;
	}

	// -- Service methods --

	@Override
	public void initialize() {
		commandMap = new HashMap<PluginInfo<?>, CommandInfo>();

		// inform the module service of available commands
		final List<PluginInfo<Command>> plugins =
			pluginService.getPluginsOfType(Command.class);
		addCommands(plugins);
	}

	// -- Event handlers --

	@EventHandler
	protected void onEvent(final PluginsRemovedEvent event) {
		removeCommands(event.getItems());
	}

	@EventHandler
	protected void onEvent(final PluginsAddedEvent event) {
		final ArrayList<PluginInfo<Command>> commands =
			new ArrayList<PluginInfo<Command>>();
		findCommandPlugins(event.getItems(), commands);
		addCommands(commands);
	}

	// -- Helper methods --

	/**
	 * Gets a {@link CommandInfo} for the given class name, creating a new one if
	 * none are registered with the service.
	 */
	private CommandInfo getOrCreate(String className) {
		final CommandInfo command = getCommand(className);
		if (command != null) return command;
		return new CommandInfo(className);
	}

	/**
	 * Gets a {@link CommandInfo} for the given class, creating a new one if
	 * none are registered with the service.
	 */
	private <C extends Command> CommandInfo getOrCreate(Class<C> commandClass) {
		final CommandInfo command = getCommand(commandClass);
		if (command != null) return command;
		return new CommandInfo(commandClass);
	}

	/** Adds new commands to the module service. */
	private void addCommands(final List<PluginInfo<Command>> plugins) {
		// extract commands from the list of plugins
		final List<CommandInfo> commands = new ArrayList<CommandInfo>();
		for (final PluginInfo<Command> info : plugins) {
			final CommandInfo commandInfo = wrapAsCommand(info);
			commands.add(commandInfo);

			// record association between plugin info and derived command info
			commandMap.put(info, commandInfo);
		}

		// add extracted commands to the module service
		moduleService.addModules(commands);
	}

	/** Removes old commands from the module service. */
	private void removeCommands(final List<PluginInfo<?>> plugins) {
		final List<CommandInfo> commands = getCommandsUnknown(plugins);

		for (final CommandInfo info : commands) {
			// clear association between plugin info and derived command info
			commandMap.remove(info);
		}

		// remove extracted commands from the module service
		moduleService.removeModules(commands);
	}

	/**
	 * Gets the command corresponding to each plugin on the given list. The
	 * linkage is obtained from the {@link #commandMap}.
	 */
	private List<CommandInfo> getCommandsUnknown(
		final List<PluginInfo<?>> plugins)
	{
		final List<CommandInfo> commands = new ArrayList<CommandInfo>();
		for (final PluginInfo<?> info : plugins) {
			final CommandInfo commandInfo = commandMap.get(info);
			if (commandInfo == null) continue;
			commands.add(commandInfo);
		}
		return commands;
	}

	/**
	 * Transfers command plugins from the source list to the destination list.
	 * 
	 * @param srcList The list to scan for matching plugins.
	 * @param destList The list to which matching plugins are added.
	 */
	private void findCommandPlugins(final List<? extends PluginInfo<?>> srcList,
		final List<PluginInfo<Command>> destList)
	{
		for (final PluginInfo<?> info : srcList) {
			if (isCommand(info)) {
				@SuppressWarnings("unchecked")
				final PluginInfo<Command> match = (PluginInfo<Command>) info;
				destList.add(match);
			}
		}
	}

	/** Determines whether the given plugin is a command. */
	private boolean isCommand(final PluginInfo<?> info) {
		return Command.class.isAssignableFrom(info.getPluginType());
	}

	/** Converts the given plugin into a command. */
	private CommandInfo wrapAsCommand(final PluginInfo<Command> pluginInfo) {
		if (pluginInfo instanceof CommandInfo) {
			// plugin info is already a command info
			return (CommandInfo) pluginInfo;
		}
		// wrap the plugin's metadata in a command info
		return new CommandInfo(pluginInfo);
	}

	/** A HACK for downcasting a list of plugins. */
	private <PT extends SciJavaPlugin> List<PluginInfo<?>> downcast(
		final List<PluginInfo<PT>> plugins)
	{
		// HACK: It seems that neither List<PluginInfo<PT>> nor
		// List<PluginInfo<? extends PT>> are usable to fulfill a method argument
		// of type List<PluginInfo<?>>. Probably something relating to (lack of)
		// covariance of generics that I am too stupid to understand.
		// So we brute force it!
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final List<PluginInfo<?>> typedPlugins = (List) plugins;
		return typedPlugins;
	}

	// -- Deprecated methods --

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Future<Module> run(String className, Object... inputs) {
		return (Future) run(className, true, inputs);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Future<Module> run(String className, Map<String, Object> inputMap) {
		return (Future) run(className, true, inputMap);
	}

	@Override
	public <C extends Command> Future<CommandModule> run(Class<C> commandClass,
		Object... inputs)
	{
		return run(commandClass, true, inputs);
	}

	@Override
	public <C extends Command> Future<CommandModule> run(Class<C> commandClass,
		Map<String, Object> inputMap)
	{
		return run(commandClass, true, inputMap);
	}

	@Override
	public Future<Module> run(ModuleInfo info, Object... inputs) {
		return moduleService.run(info, true, inputs);
	}

	@Override
	public Future<Module> run(ModuleInfo info, Map<String, Object> inputMap) {
		return moduleService.run(info, true, inputMap);
	}

	@Override
	public <M extends Module> Future<M> run(M module, Object... inputs) {
		return moduleService.run(module, true, inputs);
	}

	@Override
	public <M extends Module> Future<M>
		run(M module, Map<String, Object> inputMap)
	{
		return moduleService.run(module, true, inputMap);
	}

}
