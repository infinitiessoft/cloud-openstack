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

import org.dasein.cloud.openstack.nova.os.NovaOpenStack;

import com.infinities.skyport.network.SkyportNetworkServices;
import com.infinities.skyport.network.SkyportVLANSupport;

/**
 * @author pohsun
 *
 */
public class SkyportNovaNetworkServices extends NovaNetworkServices implements SkyportNetworkServices {

	/**
	 * @param cloud
	 */
	public SkyportNovaNetworkServices(NovaOpenStack cloud) {
		super(cloud);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.infinities.skyport.network.SkyportNetworkServices#getSkyportVlanSupport
	 * ()
	 */
	@Override
	public SkyportVLANSupport getSkyportVlanSupport() {
		return new SkyportQuantum(getProvider());
	}

}
