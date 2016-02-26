/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.dasein.cloud.openstack.nova.os.network;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;

/**
 * @author pohsun
 *
 */
public class SkyportNetworkCapabilities extends NetworkCapabilities {

	/**
	 * @param cloud
	 */
	public SkyportNetworkCapabilities(NovaOpenStack cloud) {
		super(cloud);
	}

	@Override
	public boolean allowsNewNetworkInterfaceCreation() throws CloudException, InternalException {
		Quantum q = new Quantum(getProvider());
		return q.getNetworkType().equals(Quantum.QuantumType.QUANTUM);
	}

	@Nonnull
	@Override
	public String getProviderTermForNetworkInterface(@Nonnull Locale locale) {
		return "port";
	}

	@Override
	public boolean isNetworkInterfaceSupportEnabled() throws CloudException, InternalException {
		return true;
	}

}
