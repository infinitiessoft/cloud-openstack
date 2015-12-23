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
package com.infinities.skyport.openstack.nova.os.compute;

import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.openstack.nova.os.compute.NovaComputeServices;

import com.infinities.skyport.compute.SkyportComputeServices;
import com.infinities.skyport.compute.SkyportVirtualMachineSupport;

/**
 * This is a customized version of
 * org.dasein.cloud.openstack.nova.os.compute.NovaComputeServices for
 * implmenting SkyportComputeServices
 * <p>
 * Created by Pohsun Huang: 12/23/15 10:57 AM
 * </p>
 * 
 * @author Pohsun Huang
 * @version 2015.12 initial version
 * @since 2015.12
 */
public class SkyportNovaComputeServices extends NovaComputeServices implements SkyportComputeServices {

	/**
	 * @param provider
	 */
	public SkyportNovaComputeServices(NovaOpenStack provider) {
		super(provider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.compute.SkyportComputeServices#
	 * getSkyportVirtualMachineSupport()
	 */
	@Override
	public SkyportVirtualMachineSupport getSkyportVirtualMachineSupport() {
		return new SkyportNovaServer(super.getProvider());
	}

}
