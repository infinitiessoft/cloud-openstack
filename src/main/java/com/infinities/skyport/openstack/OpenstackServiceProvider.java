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
package com.infinities.skyport.openstack;

import org.apache.commons.lang3.concurrent.ConcurrentException;

import com.infinities.skyport.ServiceProvider;
import com.infinities.skyport.annotation.Provider;
import com.infinities.skyport.compute.SkyportComputeServices;
import com.infinities.skyport.openstack.nova.os.compute.SkyportNovaComputeServices;

@Provider(enumeration = { "DELL", "DREAMHOST", "HP", "IBM", "METACLOUD", "RACKSPACE", "OTHER" })
public class OpenstackServiceProvider extends SkyportNovaOpenStack implements ServiceProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.ServiceProvider#initialize()
	 */
	@Override
	public void initialize() throws ConcurrentException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.ServiceProvider#getComputeServices()
	 */
	@Override
	public SkyportComputeServices getSkyportComputeServices() {
		return new SkyportNovaComputeServices(this);
	}

}