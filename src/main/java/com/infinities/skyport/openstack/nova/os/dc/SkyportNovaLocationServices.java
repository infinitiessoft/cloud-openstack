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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterCapabilities;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.openstack.nova.os.NovaLocationServices;
import org.dasein.cloud.openstack.nova.os.NovaMethod;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.infinities.skyport.dc.SkyportDataCenterServices;

/**
 * @author pohsun
 *
 */
public class SkyportNovaLocationServices extends NovaLocationServices implements SkyportDataCenterServices {

	static private final Logger logger = NovaOpenStack.getLogger(SkyportNovaLocationServices.class, "std");


	/**
	 * @param provider
	 */
	public SkyportNovaLocationServices(NovaOpenStack provider) {
		super(provider);
	}

	@Override
	public Collection<DataCenter> listDataCenters(String providerRegionId) throws InternalException, CloudException {
		APITrace.begin(getProvider(), "DC.listDataCenters");
		if (!getProvider().getProviderName().equalsIgnoreCase("other")) {
			try {
				Region region = getRegion(providerRegionId);

				if (region == null) {
					throw new CloudException("No such region: " + providerRegionId);
				}
				DataCenter dc = new DataCenter();

				dc.setActive(true);
				dc.setAvailable(true);
				dc.setName(region.getProviderRegionId() + "-a");
				dc.setProviderDataCenterId(region.getProviderRegionId() + "-a");
				dc.setRegionId(providerRegionId);
				return Collections.singletonList(dc);
			} finally {
				APITrace.end();
			}
		} else {
			try {
				Cache<DataCenter> cache =
						Cache.getInstance(getProvider(), "datacenters", DataCenter.class, CacheLevel.REGION_ACCOUNT,
								new TimePeriod<Day>(1, TimePeriod.DAY));
				Iterable<DataCenter> refs = cache.get(getContext());

				if (refs != null) {
					return Lists.newArrayList(refs);
				}

				JSONObject ob = getMethod().getServers("/os-availability-zone", null, true);
				List<DataCenter> dcs = new ArrayList<DataCenter>();

				try {
					if (ob != null && ob.has("availabilityZoneInfo")) {
						JSONArray list = ob.getJSONArray("availabilityZoneInfo");

						for (int i = 0; i < list.length(); i++) {
							JSONObject p = list.getJSONObject(i);
							DataCenter dc = new DataCenter();

							if (p.has("zoneName")) {
								String zoneName = p.getString("zoneName");
								if ("internal".equalsIgnoreCase(zoneName)) {
									continue;
								}
								boolean available = p.getJSONObject("zoneState").getBoolean("available");
								dc.setActive(true);
								dc.setAvailable(available);
								dc.setName(zoneName);
								dc.setProviderDataCenterId(zoneName);
								dc.setRegionId(providerRegionId);

							} else {
								continue;
							}
							dcs.add(dc);
						}
					}
				} catch (JSONException e) {
					logger.error("DC.listDataCenters(): Unable to identify expected values in JSON: " + e.getMessage());
					throw new CloudException(CloudErrorType.COMMUNICATION, 200, "invalidJson",
							"Missing JSON element for flavors: " + e.getMessage());
				}
				Iterable<DataCenter> iterable = dcs;
				cache.put(getContext(), iterable);
				return dcs;
			} finally {
				APITrace.end();
			}
		}
	}

	public NovaMethod getMethod() {
		return new NovaMethod(getProvider());
	}


	private transient volatile SkyportNovaLocationCapabilities capabilities;


	@Override
	public @Nonnull DataCenterCapabilities getCapabilities() throws InternalException, CloudException {
		if (capabilities == null) {
			capabilities = new SkyportNovaLocationCapabilities(getProvider());
		}
		return capabilities;
	}

	// /*
	// * (non-Javadoc)
	// *
	// * @see org.dasein.cloud.dc.DataCenterServices#getCapabilities()
	// */
	// @Override
	// public SkyportDataCenterCapabilities getSkyportCapabilities() throws
	// InternalException, CloudException {
	// return new SkyportNovaLocationCapabilities(getProvider());
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.infinities.skyport.dc.SkyportDataCenterServices#getHost(java.lang
	 * .String)
	 */
	// @Override
	// public Host getHost(String providerHostId) throws InternalException,
	// CloudException {
	// APITrace.begin(getProvider(), "DC.getHost");
	// try {
	// ProviderContext ctx = getProvider().getContext();
	//
	// if (ctx == null) {
	// throw new CloudException("No context exists for this request");
	// }
	// String regionId = ctx.getRegionId();
	//
	// if (regionId == null) {
	// throw new CloudException("No region is known for zones request");
	// }
	// for (DataCenter dc : listDataCenters(regionId)) {
	// for (Host host : listHosts(dc.getProviderDataCenterId())) {
	// if (host.getProviderHostId().equals(providerHostId)) {
	// return host;
	// }
	// }
	// }
	// return null;
	// } finally {
	// APITrace.end();
	// }
	// }

	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// * com.infinities.skyport.dc.SkyportDataCenterServices#listHosts(java.lang
	// * .String)
	// */
	// @Override
	// public Iterable<Host> listHosts(String providerDataCenterId) throws
	// InternalException, CloudException {
	// APITrace.begin(getProvider(), "DC.listHosts");
	// try {
	// if (!this.getSkyportCapabilities().supportsHosts()) {
	// return null;
	// }
	// Cache<Host> cache =
	// Cache.getInstance(getProvider(), "hosts", Host.class,
	// CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1,
	// TimePeriod.DAY));
	// Iterable<Host> refs = cache.get(getContext());
	//
	// if (refs != null) {
	// return Lists.newArrayList(refs);
	// }
	//
	// JSONObject ob = getMethod().getServers("/os-hosts", null, true);
	// List<Host> hosts = new ArrayList<Host>();
	//
	// try {
	// if (ob != null && ob.has("hosts")) {
	// JSONArray list = ob.getJSONArray("hosts");
	//
	// for (int i = 0; i < list.length(); i++) {
	// JSONObject p = list.getJSONObject(i);
	// Host host = new Host();
	//
	// if (p.has("host_name")) {
	// String hostName = p.getString("hostName");
	// boolean available = p.getJSONObject("zoneState").getBoolean("available");
	// dc.setActive(true);
	// dc.setAvailable(available);
	// dc.setName(zoneName);
	// dc.setProviderDataCenterId(zoneName);
	// dc.setRegionId(providerRegionId);
	//
	// } else {
	// continue;
	// }
	// hosts.add(host);
	// }
	// }
	// } catch (JSONException e) {
	// logger.error("DC.listHosts(): Unable to identify expected values in JSON: "
	// + e.getMessage());
	// throw new CloudException(CloudErrorType.COMMUNICATION, 200,
	// "invalidJson",
	// "Missing JSON element for flavors: " + e.getMessage());
	// }
	// Iterable<Host> iterable = hosts;
	// cache.put(getContext(), iterable);
	// return hosts;
	// } finally {
	// APITrace.end();
	// }
	// }

}
