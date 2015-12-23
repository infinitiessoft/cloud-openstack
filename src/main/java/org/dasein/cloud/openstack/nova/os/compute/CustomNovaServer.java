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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.openstack.nova.os.NovaMethod;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.openstack.nova.os.OpenStackProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

	// private final Logger logger =
	// LoggerFactory.getLogger(CustomNovaServer.class);

	/**
	 * @param provider
	 */
	public CustomNovaServer(NovaOpenStack provider) {
		super(provider);
	}

	@Override
	public NovaMethod getMethod() {
		return new NovaMethod(getProvider());
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
	public Platform getPlatform(String vmName, String vmDescription, String imageId) throws CloudException,
			InternalException {
		return super.getPlatform(vmName, vmDescription, imageId);
	}
}
