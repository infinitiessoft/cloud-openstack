package com.infinities.skyport;

import java.util.Iterator;
import java.util.List;

import org.dasein.cloud.Cloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenstackServiceProviderTest {

	private static final Logger logger = LoggerFactory.getLogger(OpenstackServiceProviderTest.class);
	private CloudProvider provider;


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
		ProviderContext.Value[] values = new ProviderContext.Value[fields.size()];
		values[0] = ProviderContext.Value.parseValue(fields.get(0), accessPublic, accessPrivate);

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
		// for (ContextRequirements.Field f : fields) {
		// StringBuilder field = new StringBuilder();
		// field.append("Loading '" + f.name + "' from ");
		// if (f.type.equals(ContextRequirements.FieldType.KEYPAIR)) {
		// field.append("'" + f.name + "_SHARED' and '" + f.name + "_SECRET'");
		// String shared = configuration.getProperties().getProperty(f.name +
		// "_SHARED");
		// String secret = configuration.getProperties().getProperty(f.name +
		// "_SECRET");
		//
		// if (shared != null && secret != null) {
		// values[i] = ProviderContext.Value.parseValue(f, shared, secret);
		// } else if (f.required) {
		// throw new IllegalArgumentException("Missing required field: " +
		// f.name);
		// }
		// } else {
		// field.append("'" + f.name + "'");
		// String value = configuration.getProperties().getProperty(f.name);
		//
		// if (value != null) {
		// values[i] = ProviderContext.Value.parseValue(f, value);
		// } else if (f.required) {
		// throw new IllegalArgumentException("Missing required field: " +
		// f.name);
		// }
		// }
		// i++;
		// logger.debug("{}", field.toString());
		// }

		ProviderContext ctx = cloud.createContext(accountNumber, regionId, values);
		provider = ctx.connect();
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

}
