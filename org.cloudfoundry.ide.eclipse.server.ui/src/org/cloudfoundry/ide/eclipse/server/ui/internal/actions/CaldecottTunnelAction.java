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
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.tunnel.CaldecottUIHelper;
import org.eclipse.jface.action.Action;

public class CaldecottTunnelAction extends Action {

	protected final CloudFoundryServer cloudServer;

	public CaldecottTunnelAction(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		setActionValues();
	}

	protected void setActionValues() {
		setText(Messages.CaldecottTunnelAction_TEXT_SHOW_TUNNEL);
		setImageDescriptor(CloudFoundryImages.CONNECT);
		setToolTipText(Messages.CaldecottTunnelAction_TEXT_SHOW_ACTIVE_TUNNEL);
		setEnabled(true);
	}

	public void run() {
		new CaldecottUIHelper(cloudServer).openCaldecottTunnelWizard();
	}
}
