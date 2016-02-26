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
package com.infinities.skyport.openstack.nova.os.dc;

import java.util.Locale;

import org.dasein.cloud.openstack.nova.os.NovaLocationCapabilities;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;

import com.infinities.skyport.dc.SkyportDataCenterCapabilities;

/**
 * @author pohsun
 *
 */
public class SkyportNovaLocationCapabilities extends NovaLocationCapabilities implements SkyportDataCenterCapabilities {

	/**
	 * @param provider
	 */
	public SkyportNovaLocationCapabilities(NovaOpenStack provider) {
		super(provider);
	}

	@Override
	public String getProviderTermForDataCenter(Locale locale) {
		return "availability zone";
	}

	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// * com.infinities.skyport.dc.SkyportDataCenterCapabilities#supportsHosts()
	// */
	// @Override
	// public boolean supportsHosts() {
	// return this.getProvider().getProviderName().equalsIgnoreCase("other");
	// }

}
