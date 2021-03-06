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
package com.infinities.skyport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dasein.cloud.Cloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.dc.Region;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.compute.entity.MinimalResource;
import com.infinities.skyport.openstack.OpenstackServiceProvider;
import com.infinities.skyport.testcase.IntegrationTest;

@Category(IntegrationTest.class)
public class OpenstackServiceProviderIT {

	private static final Logger logger = LoggerFactory.getLogger(OpenstackServiceProviderIT.class);
	private OpenstackServiceProvider provider;


	@Before
	public void setUp() throws Exception {
		provider = new OpenstackServiceProvider();
		String endpoint = "http://192.168.0.96:35357/v2.0/";
		String accountNumber = "7fb44a8ffc6e419497d94b8b2c13fee5";
		String accessPublic = "admin";
		String accessPrivate = "crowbar";
		String cloudName = "Suse cloud";
		String providerName = "Other";
		String regionId = "RegionOne";

		// Use that information to register the cloud
		Cloud cloud = Cloud.register(providerName, cloudName, endpoint, OpenstackServiceProvider.class);

		// Find what additional fields are necessary to connect to the cloud
		ContextRequirements requirements = cloud.buildProvider().getContextRequirements();
		List<ContextRequirements.Field> fields = requirements.getConfigurableValues();

		// Load the values for the required fields from the system properties
		@SuppressWarnings("rawtypes")
		List<ProviderContext.Value> values = new ArrayList<ProviderContext.Value>();
		values.add(ProviderContext.Value.parseValue(fields.get(0), accessPublic, accessPrivate));
		values.add(ProviderContext.Value.parseValue(fields.get(1), "internalURL"));

		StringBuilder requireds = new StringBuilder();
		requireds.append("Required fields:");
		for (ContextRequirements.Field f : fields) {
			if (f.required) {
				requireds.append("\t" + f.name + "(" + f.type + "): " + f.description);
			}
		}
		logger.debug("{}", requireds.toString());
		StringBuilder optionals = new StringBuilder();
		optionals.append("Optional fields:");
		for (ContextRequirements.Field f : fields) {
			if (!f.required) {
				optionals.append("\t" + f.name + "(" + f.type + "): " + f.description);
			}
		}
		logger.debug("{}", optionals.toString());
		for (ProviderContext.Value value : values) {
			logger.debug("name:{}, value:{}", new Object[] { value.name });
		}

		ProviderContext ctx =
				cloud.createContext(accountNumber, regionId, values.toArray(new ProviderContext.Value[values.size()]));
		provider = (OpenstackServiceProvider) ctx.connect();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws InternalException, CloudException {
		Iterable<VirtualMachineProduct> products =
				provider.getComputeServices().getVirtualMachineSupport().listAllProducts();
		Iterator<VirtualMachineProduct> iterator = products.iterator();
		while (iterator.hasNext()) {
			VirtualMachineProduct product = iterator.next();
			System.err.println(product.getName());
			System.err.println(product.getCpuCount());
			System.err.println(product.getDataCenterId());
			System.err.println(product.getDescription());
			System.err.println(product.getProviderProductId());
			System.err.println(product.getStandardHourlyRate());
			System.err.println(product.getArchitectures());
			System.err.println(product.getRamSize());
			System.err.println(product.getRootVolumeSize());
			System.err.println(product.getStatus());
			System.err.println(product.getVisibleScope());
		}

	}

	@Test
	public void testListVirtualMachine() throws InternalException, CloudException {
		Iterable<VirtualMachine> vms = provider.getComputeServices().getVirtualMachineSupport().listVirtualMachines();
		Iterator<VirtualMachine> iterator = vms.iterator();
		System.err.println("testing");
		while (iterator.hasNext()) {
			VirtualMachine vm = iterator.next();
			System.err.println(vm.toString());
		}

	}

	@Test
	public void testListMinimalVirtualMachine() throws InternalException, CloudException {
		Iterable<MinimalResource> vms =
				provider.getSkyportComputeServices().getSkyportVirtualMachineSupport().listMinimalVirtualMachines();
		Iterator<MinimalResource> iterator = vms.iterator();
		while (iterator.hasNext()) {
			MinimalResource vm = iterator.next();
			System.err.println(vm.toString());
		}

	}

	@Test
	public void testListRegion() throws InternalException, CloudException {
		Iterable<Region> regions = provider.getAuthenticationContext().listRegions();
		Iterator<Region> iterator = regions.iterator();
		while (iterator.hasNext()) {
			Region region = iterator.next();
			System.err.println(region.toString());
		}

	}

}
