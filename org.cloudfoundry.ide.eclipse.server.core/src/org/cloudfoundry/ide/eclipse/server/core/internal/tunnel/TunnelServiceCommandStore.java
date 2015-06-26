/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.tunnel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Typically there should only be one store instance available per session.
 * 
 */
public class TunnelServiceCommandStore {

	public static final String SERVICE_COMMANDS_PREF = CloudFoundryPlugin.PLUGIN_ID + ".service.tunnel.commands"; //$NON-NLS-1$

	private final static ObjectMapper mapper = new ObjectMapper();

	private TunnelServiceCommands cachedCommands;

	private final PredefinedServiceCommands predefinedCommands;

	public TunnelServiceCommandStore(PredefinedServiceCommands predefinedCommands) {
		this.predefinedCommands = predefinedCommands;
	}

	public synchronized ITunnelServiceCommands getTunnelServiceCommands() throws CoreException {
		loadCommandsFromStore();

		// Cached commands are the serialisable version. Return a subtype with
		// additional information that is not persisted,
		// like pre-defined commands

		return cachedCommands != null && predefinedCommands != null ? new CommandDefinitionsWithPredefinition(
				cachedCommands, predefinedCommands) : cachedCommands;
	}

	protected void loadCommandsFromStore() throws CoreException {
		String storedValue = CloudFoundryPlugin.getDefault().getPreferences().get(SERVICE_COMMANDS_PREF, null);
		cachedCommands = parseAndUpdateTunnelServiceCommands(storedValue);
	}

	protected TunnelServiceCommands parseAndUpdateTunnelServiceCommands(String json) throws CoreException {
		TunnelServiceCommands commands = null;
		if (json != null) {
			try {
				commands = mapper.readValue(json, TunnelServiceCommands.class);
			}
			catch (IOException e) {

				CloudFoundryPlugin.logError("Error while reading Java Map from JSON response: ", e); //$NON-NLS-1$
			}
		}

		if (commands == null) {
			// initialise commands for the first time
			commands = new TunnelServiceCommands();

			// Set a default terminal for the command
			CommandTerminal defaultTerminal = CommandTerminal.getDefaultOSTerminal();
			if (defaultTerminal != null) {
				commands.setDefaultTerminal(defaultTerminal);
			}

			ServiceCommandManager manager = new ServiceCommandManager(commands);
			manager.addServices(ServiceInfo.values());

			if (predefinedCommands != null) {
				manager.addPredefinedCommands(predefinedCommands);
			}

		}

		return commands;
	}

	public synchronized String storeServerServiceCommands(ITunnelServiceCommands commands) throws CoreException {

		String serialisedCommands = null;

		// Resolve the commands that need to be persisted, in case they are part
		// of a wrapper
		// that has information that should not be serialised
		if (commands instanceof CommandDefinitionsWithPredefinition) {
			cachedCommands = ((CommandDefinitionsWithPredefinition) commands).getSerialisableCommands();
		}
		else if (commands instanceof TunnelServiceCommands) {
			cachedCommands = (TunnelServiceCommands) commands;
		}
		if (cachedCommands != null) {
			if (mapper.canSerialize(cachedCommands.getClass())) {
				try {
					serialisedCommands = mapper.writeValueAsString(cachedCommands);
				}
				catch (IOException e) {
					throw new CoreException(CloudFoundryPlugin.getErrorStatus(
							Messages.TunnelServiceCommandStore_ERROR_SERIALIZE_JAVAMAP, e));
				}
			}
			else {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus(NLS.bind(Messages.TunnelServiceCommandStore_ERROR_VALUE_CANNOT_SERILIZE,  
						cachedCommands.getClass().getName())));
			}
		}

		if (serialisedCommands != null) {
			IEclipsePreferences prefs = CloudFoundryPlugin.getDefault().getPreferences();
			prefs.put(SERVICE_COMMANDS_PREF, serialisedCommands);
			try {
				prefs.flush();
			}
			catch (BackingStoreException e) {
				CloudFoundryPlugin.logError(e);
			}

		}

		return serialisedCommands;

	}

	public synchronized List<ServiceCommand> getCommandsForService(CloudService cloudService, boolean forceLoad)
			throws CoreException {
		List<ServiceCommand> commands = new ArrayList<ServiceCommand>();
		if (forceLoad) {
			loadCommandsFromStore();
		}
		if (cachedCommands != null && cachedCommands.getServices() != null) {
			String vendor = CloudUtil.getServiceVendor(cloudService);
			for (ServerService service : cachedCommands.getServices()) {
				if (service.getServiceInfo().getVendor().equals(vendor)) {
					commands = service.getCommands();
					break;
				}
			}
		}
		return commands;
	}

	public static TunnelServiceCommandStore getCurrentStore() {
		return CloudFoundryPlugin.getDefault().getTunnelCommandsStore();
	}

}
