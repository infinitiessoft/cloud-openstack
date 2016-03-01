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
package org.dasein.cloud.openstack.nova.os.compute;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.openstack.nova.os.OpenStackProvider;
import org.dasein.cloud.openstack.nova.os.network.NovaNetworkServices;
import org.dasein.cloud.openstack.nova.os.network.Quantum;
import org.dasein.cloud.util.APITrace;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.compute.VMUpdateOptions;
import com.infinities.skyport.openstack.nova.os.SkyportNovaMethod;

/**
 * This is a customized version of
 * org.dasein.cloud.openstack.nova.os.compute.NovaServer for accessing
 * NovaServer
 * <p>
 * Created by Pohsun Huang: 12/23/15 10:57 AM
 * </p>
 * 
 * @author Pohsun Huang
 * @version 2015.12 initial version
 * @since 2015.12
 */
public class CustomNovaServer extends NovaServer {

	private final Logger logger = LoggerFactory.getLogger(CustomNovaServer.class);


	/**
	 * @param provider
	 */
	public CustomNovaServer(NovaOpenStack provider) {
		super(provider);
	}

	@Override
	public SkyportNovaMethod getMethod() {
		return new SkyportNovaMethod(getProvider());
	}

	public NovaOpenStack getNovaOpenStack() {
		return getProvider();
	}

	@Override
	public String getTenantId() throws CloudException, InternalException {
		return super.getTenantId();
	}

	@Override
	public OpenStackProvider getCloudProvider() {
		return super.getCloudProvider();
	}

	@Override
	public int getMinorVersion() throws CloudException, InternalException {
		return super.getMinorVersion();
	}

	@Override
	public int getMajorVersion() throws CloudException, InternalException {
		return super.getMajorVersion();
	}

	@Override
	public String getRegionId() throws InternalException {
		return super.getRegionId();
	}

	public @Nonnull Iterable<String> listFirewalls(@Nonnull String vmId, @Nonnull JSONObject server)
			throws InternalException, CloudException {
		try {
			if (server.has("security_groups")) {
				NetworkServices services = getProvider().getNetworkServices();
				Iterable<Firewall> firewalls = null;

				if (services != null) {
					FirewallSupport support = services.getFirewallSupport();

					if (support != null) {
						firewalls = support.list();
					}
				}
				if (firewalls == null) {
					firewalls = Collections.emptyList();
				}
				JSONArray groups = server.getJSONArray("security_groups");
				List<String> results = new ArrayList<String>();

				for (int i = 0; i < groups.length(); i++) {
					JSONObject group = groups.getJSONObject(i);
					String id = group.has("id") ? group.getString("id") : null;
					String name = group.has("name") ? group.getString("name") : null;

					if (id != null || name != null) {
						for (Firewall fw : firewalls) {
							if (id != null) {
								if (id.equals(fw.getProviderFirewallId())) {
									results.add(id);
								}
							} else if (name.equals(fw.getName())) {
								results.add(fw.getProviderFirewallId());
							}
						}
					}
				}
				return results;
			} else {
				List<String> results = new ArrayList<String>();

				JSONObject ob = getMethod().getServers("/os-security-groups/servers", vmId + "/os-security-groups", true);

				if (ob != null) {

					if (ob.has("security_groups")) {
						JSONArray groups = ob.getJSONArray("security_groups");

						for (int i = 0; i < groups.length(); i++) {
							JSONObject group = groups.getJSONObject(i);

							if (group.has("id")) {
								results.add(group.getString("id"));
							}
						}
					}
				}
				return results;
			}
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}

	@Override
	public @Nonnull VirtualMachine launch(@Nonnull VMLaunchOptions options) throws CloudException, InternalException {
		APITrace.begin(getProvider(), "VM.launch");
		VirtualMachine vm = null;
		String portId = null;
		try {
			MachineImage targetImage =
					getProvider().getComputeServices().getImageSupport().getImage(options.getMachineImageId());

			// Additional LPAR Call
			boolean isBareMetal = false;
			try {
				String lparMetadataKey = "hypervisor_type";
				String lparMetadataValue = "Hitachi";
				JSONObject ob =
						getMethod().getServers("/images/" + options.getMachineImageId() + "/metadata", lparMetadataKey,
								false);
				if (ob.has("metadata")) {
					JSONObject metadata = ob.getJSONObject("metadata");
					if (metadata.has(lparMetadataKey) && metadata.getString(lparMetadataKey).equals(lparMetadataValue))
						isBareMetal = true;
				}
			} catch (Exception ex) {
				// Something failed while checking Hitachi LPAR metadata
				logger.error("Failed to find Hitachi LPAR metadata");
			}

			if (targetImage == null) {
				throw new CloudException("No such machine image: " + options.getMachineImageId());
			}
			Map<String, Object> wrapper = new HashMap<String, Object>();
			Map<String, Object> json = new HashMap<String, Object>();

			json.put("name", options.getHostName());
			if (options.getBootstrapPassword() != null) {
				json.put("adminPass", options.getBootstrapPassword());
			}
			if (options.getUserData() != null) {
				try {
					json.put("user_data", Base64.encodeBase64String(options.getUserData().getBytes("utf-8")));
				} catch (UnsupportedEncodingException e) {
					throw new InternalException(e);
				}
			}
			if (getMinorVersion() == 0 && getMajorVersion() == 1) {
				json.put("imageId", String.valueOf(options.getMachineImageId()));
				json.put("flavorId", options.getStandardProductId());
			} else {
				if (getProvider().getProviderName().equals("HP")) {
					json.put("imageRef", options.getMachineImageId());
				} else {
					json.put("imageRef",
							getProvider().getComputeServices().getImageSupport().getImageRef(options.getMachineImageId()));
				}
				json.put("flavorRef", getFlavorRef(options.getStandardProductId()));
			}

			if (options.getVlanId() != null) {
				List<Map<String, Object>> vlans = new ArrayList<Map<String, Object>>();
				Map<String, Object> vlan = new HashMap<String, Object>();

				vlan.put("uuid", options.getVlanId());
				vlans.add(vlan);
				json.put("networks", vlans);
			} else {
				if (options.getSubnetId() != null && !getProvider().isRackspace()) {
					NovaNetworkServices services = getProvider().getNetworkServices();

					if (services != null) {
						Quantum support = services.getVlanSupport();

						if (support != null) {
							List<Map<String, Object>> vlans = new ArrayList<Map<String, Object>>();
							Map<String, Object> vlan = new HashMap<String, Object>();

							try {
								portId =
										support.createPort(options.getSubnetId(), options.getHostName(),
												options.getFirewallIds());
								vlan.put("port", portId);
								vlans.add(vlan);
								json.put("networks", vlans);
								options.withMetaData("org.dasein.portId", portId);
							} catch (CloudException e) {
								if (e.getHttpCode() != 403) {
									throw new CloudException(e.getMessage());
								}

								logger.warn("Unable to create port - trying to launch into general network");
								Subnet subnet = support.getSubnet(options.getSubnetId());

								vlan.put("uuid", subnet.getProviderVlanId());
								vlans.add(vlan);
								json.put("networks", vlans);
							}
						}
					}
				}
			}
			if (options.getBootstrapKey() != null) {
				json.put("key_name", options.getBootstrapKey());
			}
			if (options.getDataCenterId() != null) {
				json.put("os-availability-zone:availability_zone", options.getDataCenterId());
			}
			if (options.getFirewallIds().length > 0) {
				List<Map<String, Object>> firewalls = new ArrayList<Map<String, Object>>();

				for (String id : options.getFirewallIds()) {
					NetworkServices services = getProvider().getNetworkServices();
					Firewall firewall = null;

					if (services != null) {
						FirewallSupport support = services.getFirewallSupport();

						if (support != null) {
							firewall = support.getFirewall(id);
						}
					}
					if (firewall != null) {
						Map<String, Object> fw = new HashMap<String, Object>();

						fw.put("name", firewall.getName());
						firewalls.add(fw);
					}
				}
				json.put("security_groups", firewalls);
			}

			if (isBareMetal) {
				Map<String, String> blockDeviceMapping = new HashMap<String, String>();
				// blockDeviceMapping.put("device_name", "/dev/sdb1");
				blockDeviceMapping.put("boot_index", "0");
				blockDeviceMapping.put("uuid",
						getProvider().getComputeServices().getImageSupport().getImageRef(options.getMachineImageId()));
				// blockDeviceMapping.put("guest_format", "ephemeral");
				String volumeSize = "";
				if (targetImage.getTag("minDisk") != null) {
					volumeSize = (String) targetImage.getTag("minDisk");
				} else {
					String minSize = (String) targetImage.getTag("minSize");
					volumeSize = roundUpToGB(Long.valueOf(minSize)) + "";
				}
				blockDeviceMapping.put("volume_size", volumeSize);
				blockDeviceMapping.put("source_type", "image");
				blockDeviceMapping.put("destination_type", "volume");
				blockDeviceMapping.put("delete_on_termination", "True");
				json.put("block_device_mapping_v2", blockDeviceMapping);
			}

			if (!targetImage.getPlatform().equals(Platform.UNKNOWN)) {
				options.withMetaData("org.dasein.platform", targetImage.getPlatform().name());
			}
			options.withMetaData("org.dasein.description", options.getDescription());
			Map<String, Object> tmpMeta = options.getMetaData();
			Map<String, Object> newMeta = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : tmpMeta.entrySet()) {
				if (entry.getValue() != null) { // null values not supported by
												// openstack
					newMeta.put(entry.getKey().toString(), entry.getValue());
				}
			}
			json.put("metadata", newMeta);
			wrapper.put("server", json);
			JSONObject result =
					getMethod().postServers(isBareMetal ? "/os-volumes_boot" : "/servers", null, new JSONObject(wrapper),
							true);

			if (result.has("server")) {
				try {
					Collection<IpAddress> ips = Collections.emptyList();
					Collection<VLAN> nets = Collections.emptyList();

					JSONObject server = result.getJSONObject("server");
					vm = toVirtualMachine(server, ips, ips, nets);

					if (vm != null) {
						String vmId = vm.getProviderVirtualMachineId();
						long timeout = System.currentTimeMillis() + 5 * 60 * 1000;
						while ((vm == null || vm.getCurrentState() == null) && System.currentTimeMillis() < timeout) {
							try {
								Thread.sleep(5000);
							} catch (InterruptedException ignore) {
							}
							vm = getVirtualMachine(vmId);
						}
						if (vm == null || vm.getCurrentState() == null) {
							throw new CloudException("VM failed to launch with a meaningful status");
						}
						return vm;
					}
				} catch (JSONException e) {
					logger.error("launch(): Unable to understand launch response: " + e.getMessage());
					if (logger.isTraceEnabled()) {
						e.printStackTrace();
					}
					throw new CloudException(e);
				}
			}
			logger.error("launch(): No server was created by the launch attempt, and no error was returned");
			throw new CloudException("No virtual machine was launched");

		} finally {
			if (portId != null && (vm == null || VmState.ERROR.equals(vm.getCurrentState()))) { // if
																								// launch
																								// fails
																								// or
																								// instance
																								// in
																								// error
																								// state
																								// -
																								// remove
																								// port
				Quantum quantum = getProvider().getNetworkServices().getVlanSupport();
				if (quantum != null) {
					quantum.removePort(portId);
				}
			}
			APITrace.end();
		}
	}

	@Override
	public Platform getPlatform(String vmName, String vmDescription, String imageId) throws CloudException,
			InternalException {
		return super.getPlatform(vmName, vmDescription, imageId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.compute.SkyportVirtualMachineSupport#
	 * updateVirtualMachine(java.lang.String,
	 * com.infinities.skyport.compute.VMUpdateOptions)
	 */
	public VirtualMachine updateVirtualMachine(String virtualMachineId, VMUpdateOptions options) throws InternalException,
			CloudException {
		APITrace.begin(getProvider(), "VM.updateVirtualMachine");
		try {
			Map<String, Object> json = new HashMap<String, Object>();
			Map<String, Object> server = new HashMap<String, Object>();
			String name = options.getName();

			server.put("name", name);
			json.put("server", server);
			getMethod().putServers("/servers", virtualMachineId, new JSONObject(json), null);
			VirtualMachine vm = getVirtualMachine(virtualMachineId);
			return vm;
		} finally {
			APITrace.end();
		}
	}

}
