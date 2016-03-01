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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.network.NICState;
import org.dasein.cloud.network.NetworkInterface;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANCapabilities;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VlanCreateOptions;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.util.APITrace;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.infinities.skyport.network.NICAttachOptions;
import com.infinities.skyport.network.NICDetachOptions;
import com.infinities.skyport.network.SkyportVLANSupport;

/**
 * @author pohsun
 *
 */
public class SkyportQuantum extends Quantum implements SkyportVLANSupport {

	static private final Logger logger = NovaOpenStack.getLogger(Quantum.class, "std");


	/**
	 * @param provider
	 */
	public SkyportQuantum(NovaOpenStack provider) {
		super(provider);
	}

	@Override
	public String attachNetworkInterface(@Nonnull NICAttachOptions options) throws CloudException, InternalException {
		APITrace.begin(getProvider(), "VLAN.attachNetworkInterface");
		try {
			if (!getNetworkType().equals(QuantumType.QUANTUM)) {
				throw new OperationNotSupportedException("Cannot attach network interface in an OpenStack network of type: "
						+ getNetworkType());
			}
			Map<String, Object> json = new HashMap<String, Object>();
			Map<String, Object> interfaceAttachment = new HashMap<String, Object>();

			String nicId = options.getNicId();
			String vlanId = options.getVlanId();
			String vmId = options.getVmId();

			if (!Strings.isNullOrEmpty(nicId)) {
				interfaceAttachment.put("port_id", nicId);
			} else if (!Strings.isNullOrEmpty(vlanId)) {
				interfaceAttachment.put("net_id", vlanId);
			}
			json.put("interfaceAttachment", interfaceAttachment);

			JSONObject result =
					getMethod().postServers("/servers/", vmId + getPortInterfaceResources(), new JSONObject(json), false);
			if (result != null && result.has("interfaceAttachment")) {
				try {
					if (result != null && result.has("interfaceAttachment")) {
						JSONObject obj = result.getJSONObject("interfaceAttachment");
						if (obj.has("port_id")) {
							return obj.getString("port_id");
						}
					}
				} catch (JSONException e) {
					logger.error("Unable to understand listNetworkInterfacesForVm response: " + e.getMessage());
					throw new CloudException(e);
				}
			}
			throw new CloudException("Unable to get port_id from listNetworkInterfacesForVm response: " + result.toString());
		} finally {
			APITrace.end();
		}
	}

	public String getPortInterfaceResources() {
		return "/os-interface";
	}

	@Override
	public @Nonnull NetworkInterface createNetworkInterface(@Nonnull NICCreateOptions options) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "VLAN.createNetworkInterface");
		String subnetId = options.getSubnetId();
		String vlanId = options.getVlanId();
		String vmName = options.getName();
		String[] firewallIds = options.getFirewallIds();

		try {
			if (!getNetworkType().equals(QuantumType.QUANTUM)) {
				throw new OperationNotSupportedException("Cannot create network interface in an OpenStack network of type: "
						+ getNetworkType());
			}

			Map<String, Object> wrapper = new HashMap<String, Object>();
			Map<String, Object> json = new HashMap<String, Object>();

			json.put("name", "Port for " + vmName);
			if (!Strings.isNullOrEmpty(vlanId)) {
				json.put("network_id", vlanId);
			}
			if (firewallIds != null && firewallIds.length > 0) {
				JSONArray firewalls = new JSONArray();
				for (String firewall : firewallIds) {
					firewalls.put(firewall);
				}
				json.put("security_groups", firewalls);
			}

			if (!Strings.isNullOrEmpty(subnetId)) {
				List<Map<String, Object>> ips = new ArrayList<Map<String, Object>>();
				Map<String, Object> ip = new HashMap<String, Object>();
				ip.put("subnet_id", subnetId);
				ips.add(ip);
				json.put("fixed_ips", ips);
			}

			wrapper.put("port", json);

			JSONObject result = getMethod().postNetworks(getPortResource(), null, new JSONObject(wrapper), false);
			if (result != null && result.has("port")) {
				try {
					JSONObject ob = result.getJSONObject("port");
					if (ob.has("id")) {
						return toNetworkInterface(ob);
					}
				} catch (JSONException e) {
					logger.error("Unable to understand create response: " + e.getMessage());
					throw new CloudException(e);
				}
			}
			logger.error("No port was created by the create attempt, and no error was returned");
			throw new CloudException("No port was created");

		} finally {
			APITrace.end();
		}

	}

	@Override
	public void detachNetworkInterface(@Nonnull NICDetachOptions options) throws CloudException, InternalException {
		APITrace.begin(getProvider(), "VLAN.detachNetworkInterface");
		if (Strings.isNullOrEmpty(options.getVmId())) {
			throw new IllegalArgumentException("invalid vmId");
		}
		if (Strings.isNullOrEmpty(options.getNicId())) {
			throw new IllegalArgumentException("invalid nicId");
		}
		String vmId = options.getVmId();
		String nicId = options.getNicId();

		try {
			if (!getNetworkType().equals(QuantumType.QUANTUM)) {
				throw new OperationNotSupportedException("Cannot detach network interface in an OpenStack network of type: "
						+ getNetworkType());
			}

			getMethod().deleteServers("/servers", vmId + getPortInterfaceResources() + "/" + nicId);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void removeNetworkInterface(@Nonnull String nicId) throws CloudException, InternalException {
		APITrace.begin(getProvider(), "VLAN.removeNetworkInterface");
		try {
			if (!getNetworkType().equals(QuantumType.QUANTUM)) {
				throw new OperationNotSupportedException("Cannot remove network interface in an OpenStack network of type: "
						+ getNetworkType());
			}
			getMethod().deleteNetworks(getPortResource(), nicId + ".json");
		} catch (CloudException e) {
			if (e.getHttpCode() == HttpStatus.SC_NOT_FOUND) {
				logger.warn("Error while deleting port [" + nicId + "], but it is probably fine");
			} else {
				throw e;
			}
		} finally {
			APITrace.end();
		}
	}

	@Override
	public @Nonnull Iterable<NetworkInterface> listNetworkInterfaces() throws CloudException, InternalException {
		APITrace.begin(getProvider(), "VLAN.listNetworkInterfaces");
		try {
			if (!getNetworkType().equals(QuantumType.QUANTUM)) {
				throw new OperationNotSupportedException("Cannot list network interfaces in an OpenStack network of type: "
						+ getNetworkType());
			}

			JSONObject result = getMethod().getNetworks(getPortResource(), null, false);
			if (result != null && result.has("ports")) {
				List<NetworkInterface> interfaces = new ArrayList<NetworkInterface>();
				try {
					JSONArray ports = result.getJSONArray("ports");
					for (int i = 0; i < ports.length(); i++) {
						interfaces.add(toNetworkInterface(ports.getJSONObject(i)));
					}
				} catch (JSONException e) {
					logger.error("Unable to understand listPorts response: " + e.getMessage());
					throw new CloudException(e);
				}
				return interfaces;
			}
			return new ArrayList<NetworkInterface>();
		} finally {
			APITrace.end();
		}
	}

	@Override
	public @Nonnull Iterable<NetworkInterface> listNetworkInterfacesForVM(@Nonnull String forVmId) throws CloudException,
			InternalException {
		APITrace.begin(getProvider(), "VLAN.listNetworkInterfacesForVm");
		try {
			if (!getNetworkType().equals(QuantumType.QUANTUM)) {
				throw new OperationNotSupportedException("Cannot list network interfaces in an OpenStack network of type: "
						+ getNetworkType());
			}

			JSONObject result = getMethod().getServers("/servers/", forVmId + getPortInterfaceResources(), false);
			if (result != null && result.has("interfaceAttachments")) {
				List<NetworkInterface> interfaces = new ArrayList<NetworkInterface>();
				try {
					JSONArray interfaceAttachments = result.getJSONArray("interfaceAttachments");
					for (int i = 0; i < interfaceAttachments.length(); i++) {
						interfaces.add(toNetworkInterface(interfaceAttachments.getJSONObject(i)));
					}
				} catch (JSONException e) {
					logger.error("Unable to understand listNetworkInterfacesForVm response: " + e.getMessage());
					throw new CloudException(e);
				}
				return interfaces;
			}
			return new ArrayList<NetworkInterface>();
		} finally {
			APITrace.end();
		}
	}

	protected @Nullable NetworkInterface toNetworkInterface(@Nonnull JSONObject port) throws CloudException,
			InternalException {
		try {
			NetworkInterface nic = new NetworkInterface();

			nic.setProviderOwnerId(getTenantId());
			nic.setCurrentState(NICState.AVAILABLE);
			nic.setProviderRegionId(getCurrentRegionId());

			if (port.has("id")) {
				nic.setProviderNetworkInterfaceId(port.getString("id"));
			}
			if (port.has("port_id")) {
				nic.setProviderNetworkInterfaceId(port.getString("port_id"));
			}
			if (port.has("name")) {
				nic.setName(port.getString("name"));
			}

			if (port.has("fixed_ips")) {
				JSONArray ips = port.getJSONArray("fixed_ips");
				RawAddress[] addresses = new RawAddress[ips.length()];
				for (int i = 0; i < ips.length(); i++) {
					JSONObject ip = ips.getJSONObject(i);
					RawAddress address = new RawAddress(ip.getString("ip_address"));
					addresses[i] = address;
					if (ip.has("subnet_id")) {
						nic.setProviderSubnetId(ip.getString("subnet_id"));
					}
				}
				nic.setIpAddresses(addresses);

			}
			if (port.has("mac_address")) {
				nic.setMacAddress(port.getString("mac_address"));
			}
			if (port.has("mac_addr")) {
				nic.setMacAddress(port.getString("mac_addr"));
			}
			if (port.has("status")) {
				nic.setCurrentState(toNICState(port.getString("status")));
			}
			if (port.has("network_id")) {
				nic.setProviderVlanId(port.getString("network_id"));
			}
			if (port.has("net_id")) {
				nic.setProviderVlanId(port.getString("net_id"));
			}
			if (port.has("binding:host_id")) {
				nic.getTags().put("binding:host_id", port.getString("binding:host_id"));
			}
			if (port.has("device_owner")) {
				nic.getTags().put("device_owner", port.getString("device_owner"));
			}
			if (port.has("device_id")) {
				nic.getTags().put("device_id", port.getString("device_id"));
			}
			if (port.has("admin_state_up")) {
				nic.getTags().put("admin_state_up", port.getString("admin_state_up"));
			}
			if (port.has("binding:vnic_type")) {
				nic.getTags().put("binding:vnic_type", port.getString("binding:vnic_type"));
			}
			if (port.has("binding:vif_type")) {
				nic.getTags().put("binding:vif_type", port.getString("binding:vif_type"));
			}

			return nic;
		} catch (JSONException e) {
			throw new CloudException("Invalid JSON from cloud: " + e.getMessage());
		}
	}

	private @Nonnull String getNetworkResource() throws CloudException, InternalException {
		QuantumType type = getNetworkType();
		if (type.equals(QuantumType.QUANTUM)) {
			return getNetworkResourceVersion() + QuantumType.QUANTUM.getNetworkResource();
		}
		return type.getNetworkResource();
	}

	private @Nonnull String getPortResource() throws CloudException, InternalException {
		QuantumType type = getNetworkType();
		if (type.equals(QuantumType.QUANTUM)) {
			return getNetworkResourceVersion() + QuantumType.QUANTUM.getPortResource();
		}
		return type.getSubnetResource();
	}

	protected @Nonnull NICState toNICState(@Nonnull String s) {
		if (s.equalsIgnoreCase("active")) {
			return NICState.IN_USE;
		}
		return NICState.PENDING;
	}


	private transient volatile SkyportNetworkCapabilities capabilities;


	@Nonnull
	@Override
	public VLANCapabilities getCapabilities() throws InternalException, CloudException {
		if (capabilities == null) {
			capabilities = new SkyportNetworkCapabilities(getProvider());
		}
		return capabilities;
	}
	
	@Override
    public @Nonnull VLAN createVlan(@Nonnull VlanCreateOptions options) throws CloudException, InternalException {
		return createVlan(options.getCidr(), options.getName(), options.getDescription(), options.getDomain(), options.getDnsServers(), options.getNtpServers());
    }

	@Override
	protected @Nullable VLAN toVLAN(@Nonnull JSONObject network) throws CloudException, InternalException {
        try {
            VLAN v = new VLAN();

            v.setProviderOwnerId(getTenantId());
            v.setCurrentState(VLANState.AVAILABLE);
            v.setProviderRegionId(getCurrentRegionId());
            // FIXME: REMOVE: vlans are not constrained to a DC in openstack
            //            Iterable<DataCenter> dc = getProvider().getDataCenterServices().listDataCenters(getContext().getRegionId());
            //            v.setProviderDataCenterId(dc.iterator().next().getProviderDataCenterId());
            v.setVisibleScope(VisibleScope.ACCOUNT_REGION);
            
            if( network.has("id") ) {
                v.setProviderVlanId(network.getString("id"));
            }
            if( network.has("name") ) {
                v.setName(network.getString("name"));
            }
            else if( network.has("label") ) {
                v.setName(network.getString("label"));
            }
            if( network.has("cidr") ) {
                v.setCidr(network.getString("cidr"));
            }
            if( network.has("status") ) {
                v.setCurrentState(toState(network.getString("status")));
            }
            if( network.has("metadata") ) {
                JSONObject md = network.getJSONObject("metadata");
                String[] names = JSONObject.getNames(md);

                if( names != null && names.length > 0 ) {
                    for( String n : names ) {
                        String value = md.getString(n);

                        if( value != null ) {
                            v.setTag(n, value);
                            if( n.equals("org.dasein.description") && v.getDescription() == null ) {
                                v.setDescription(value);
                            }
                            else if( n.equals("org.dasein.domain") && v.getDomainName() == null ) {
                                v.setDomainName(value);
                            }
                            else if( n.startsWith("org.dasein.dns.") && !n.equals("org.dasein.dsn.") && v.getDnsServers().length < 1 ) {
                                List<String> dns = new ArrayList<String>();

                                try {
                                    int idx = Integer.parseInt(n.substring("org.dasein.dns.".length() + 1));
                                    if( value != null ) {
                                        dns.add(idx - 1, value);
                                    }
                                }
                                catch( NumberFormatException ignore ) {
                                    // ignore
                                }
                                v.setDnsServers(dns.toArray(new String[dns.size()]));
                            }
                            else if( n.startsWith("org.dasein.ntp.") && !n.equals("org.dasein.ntp.") && v.getNtpServers().length < 1 ) {
                                List<String> ntp = new ArrayList<String>();

                                try {
                                    int idx = Integer.parseInt(n.substring("org.dasein.ntp.".length()));
                                    if( value != null ) {
                                        ntp.add(idx - 1, value);
                                    }
                                }
                                catch( NumberFormatException ignore ) {
                                }
                                v.setNtpServers(ntp.toArray(new String[ntp.size()]));
                            }
                        }
                    }
                }
            }
            if( v.getProviderVlanId() == null ) {
                return null;
            }
            if( v.getCidr() == null ) {
                v.setCidr("0.0.0.0/0");
            }
            if( v.getName() == null ) {
                v.setName(v.getCidr());
                if( v.getName() == null ) {
                    v.setName(v.getProviderVlanId());
                }
            }
            if( v.getDescription() == null ) {
                v.setDescription(v.getName());
            }
            v.setSupportedTraffic(IPVersion.IPV4, IPVersion.IPV6);
            
            // Add by tata
            if (network.has("tenant_id")) {
            	v.setTag("tenant_id", network.getString("tenant_id"));
            }
            
            if (network.has("subnets")) {
            	v.setTag("subnets", network.getString("subnets"));
            }
            return v;
        }
        catch( JSONException e ) {
            throw new CloudException("Invalid JSON from cloud: " + e.getMessage());
        }
	}
}
