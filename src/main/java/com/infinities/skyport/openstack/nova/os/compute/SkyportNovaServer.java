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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.net.util.SubnetUtils;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.SpotPriceHistory;
import org.dasein.cloud.compute.SpotPriceHistoryFilterOptions;
import org.dasein.cloud.compute.SpotVirtualMachineRequest;
import org.dasein.cloud.compute.SpotVirtualMachineRequestCreateOptions;
import org.dasein.cloud.compute.SpotVirtualMachineRequestFilterOptions;
import org.dasein.cloud.compute.VMFilterOptions;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VMScalingCapabilities;
import org.dasein.cloud.compute.VMScalingOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineCapabilities;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineProductFilterOptions;
import org.dasein.cloud.compute.VirtualMachineStatus;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VmStatistics;
import org.dasein.cloud.compute.VmStatusFilterOptions;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.IpAddress;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.openstack.nova.os.OpenStackProvider;
import org.dasein.cloud.openstack.nova.os.compute.CustomNovaServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.infinities.skyport.compute.SkyportVirtualMachineSupport;
import com.infinities.skyport.compute.entity.MinimalResource;
import com.infinities.skyport.compute.entity.NovaStyleVirtualMachine;
import com.infinities.skyport.network.SkyportRawAddress;

/**
 * This is a customized version of
 * org.dasein.cloud.openstack.nova.os.compute.NovaServer for implmenting
 * SkyportVirtualMachineSupport
 * <p>
 * Created by Pohsun Huang: 12/23/15 10:57 AM
 * </p>
 * 
 * @author Pohsun Huang
 * @version 2015.12 initial version
 * @since 2015.12
 */
public class SkyportNovaServer implements SkyportVirtualMachineSupport {

	private static final Logger logger = LoggerFactory.getLogger(SkyportNovaServer.class);
	private CustomNovaServer novaServer;


	/**
	 * @param provider
	 */
	public SkyportNovaServer(NovaOpenStack provider) {
		this.novaServer = new CustomNovaServer(provider);
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return novaServer.hashCode();
	}

	/**
	 * @param vmId
	 * @param options
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#alterVirtualMachine(java.lang.String,
	 *      org.dasein.cloud.compute.VMScalingOptions)
	 */
	@Deprecated
	@Override
	public VirtualMachine alterVirtualMachine(String vmId, VMScalingOptions options) throws InternalException,
			CloudException {
		return novaServer.alterVirtualMachine(vmId, options);
	}

	/**
	 * @param vmId
	 * @param firewalls
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#modifyInstance(java.lang.String,
	 *      java.lang.String[])
	 */
	@Deprecated
	@Override
	public VirtualMachine modifyInstance(String vmId, String[] firewalls) throws InternalException, CloudException {
		return novaServer.modifyInstance(vmId, firewalls);
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return novaServer.equals(obj);
	}

	/**
	 * @param virtualMachineId
	 * @param cpuCount
	 * @param ramInMB
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#alterVirtualMachineSize(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	@Override
	public VirtualMachine alterVirtualMachineSize(String virtualMachineId, String cpuCount, String ramInMB)
			throws InternalException, CloudException {
		return novaServer.alterVirtualMachineSize(virtualMachineId, cpuCount, ramInMB);
	}

	/**
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#getCapabilities()
	 */
	@Override
	public VirtualMachineCapabilities getCapabilities() throws InternalException, CloudException {
		return novaServer.getCapabilities();
	}

	/**
	 * @param virtualMachineId
	 * @param firewalls
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#alterVirtualMachineFirewalls(java.lang.String,
	 *      java.lang.String[])
	 */
	@Override
	public VirtualMachine alterVirtualMachineFirewalls(String virtualMachineId, String[] firewalls)
			throws InternalException, CloudException {
		return novaServer.alterVirtualMachineFirewalls(virtualMachineId, firewalls);
	}

	/**
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#cancelSpotDataFeedSubscription()
	 */
	@Override
	public void cancelSpotDataFeedSubscription() throws CloudException, InternalException {
		novaServer.cancelSpotDataFeedSubscription();
	}

	/**
	 * @param providerSpotVirtualMachineRequestID
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#cancelSpotVirtualMachineRequest(java.lang.String)
	 */
	@Override
	public void cancelSpotVirtualMachineRequest(String providerSpotVirtualMachineRequestID) throws CloudException,
			InternalException {
		novaServer.cancelSpotVirtualMachineRequest(providerSpotVirtualMachineRequestID);
	}

	/**
	 * @param vmId
	 * @param intoDcId
	 * @param name
	 * @param description
	 * @param powerOn
	 * @param firewallIds
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#clone(java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String, boolean,
	 *      java.lang.String[])
	 */
	@Override
	public VirtualMachine clone(String vmId, String intoDcId, String name, String description, boolean powerOn,
			String... firewallIds) throws InternalException, CloudException {
		return novaServer.clone(vmId, intoDcId, name, description, powerOn, firewallIds);
	}

	/**
	 * @param vmId
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#getConsoleOutput(java.lang.String)
	 */
	@Override
	public String getConsoleOutput(String vmId) throws CloudException, InternalException {
		return novaServer.getConsoleOutput(vmId);
	}

	/**
	 * @param options
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#createSpotVirtualMachineRequest(org.dasein.cloud.compute.SpotVirtualMachineRequestCreateOptions)
	 */
	@Override
	public SpotVirtualMachineRequest createSpotVirtualMachineRequest(SpotVirtualMachineRequestCreateOptions options)
			throws CloudException, InternalException {
		return novaServer.createSpotVirtualMachineRequest(options);
	}

	/**
	 * @param options
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#listSpotVirtualMachineRequests(org.dasein.cloud.compute.SpotVirtualMachineRequestFilterOptions)
	 */
	@Override
	public Iterable<SpotVirtualMachineRequest> listSpotVirtualMachineRequests(SpotVirtualMachineRequestFilterOptions options)
			throws CloudException, InternalException {
		return novaServer.listSpotVirtualMachineRequests(options);
	}

	/**
	 * @param productId
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#getProduct(java.lang.String)
	 */
	@Override
	public VirtualMachineProduct getProduct(String productId) throws InternalException, CloudException {
		return novaServer.getProduct(productId);
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#describeVerticalScalingCapabilities()
	 */
	@Deprecated
	@Override
	public VMScalingCapabilities describeVerticalScalingCapabilities() throws CloudException, InternalException {
		return novaServer.describeVerticalScalingCapabilities();
	}

	/**
	 * @param vmId
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#disableAnalytics(java.lang.String)
	 */
	@Override
	public void disableAnalytics(String vmId) throws InternalException, CloudException {
		novaServer.disableAnalytics(vmId);
	}

	/**
	 * @param vmId
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#enableAnalytics(java.lang.String)
	 */
	@Override
	public void enableAnalytics(String vmId) throws InternalException, CloudException {
		novaServer.enableAnalytics(vmId);
	}

	/**
	 * @param bucketName
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#enableSpotDataFeedSubscription(java.lang.String)
	 */
	@Override
	public void enableSpotDataFeedSubscription(String bucketName) throws CloudException, InternalException {
		novaServer.enableSpotDataFeedSubscription(bucketName);
	}

	/**
	 * @param vmId
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#getVirtualMachine(java.lang.String)
	 */
	@Override
	public VirtualMachine getVirtualMachine(String vmId) throws InternalException, CloudException {
		return novaServer.getVirtualMachine(vmId);
	}

	/**
	 * @param state
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getCostFactor(org.dasein.cloud.compute.VmState)
	 */
	@Deprecated
	@Override
	public int getCostFactor(VmState state) throws CloudException, InternalException {
		return novaServer.getCostFactor(state);
	}

	/**
	 * @param vmId
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getPassword(java.lang.String)
	 */
	@Override
	public String getPassword(String vmId) throws InternalException, CloudException {
		return novaServer.getPassword(vmId);
	}

	/**
	 * @param vmId
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getUserData(java.lang.String)
	 */
	@Override
	public String getUserData(String vmId) throws InternalException, CloudException {
		return novaServer.getUserData(vmId);
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getMaximumVirtualMachineCount()
	 */
	@Deprecated
	@Override
	public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
		return novaServer.getMaximumVirtualMachineCount();
	}

	/**
	 * @param locale
	 * @return
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getProviderTermForServer(java.util.Locale)
	 */
	@Deprecated
	@Override
	public String getProviderTermForServer(Locale locale) {
		return novaServer.getProviderTermForServer(locale);
	}

	/**
	 * @param vmId
	 * @param from
	 * @param to
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getVMStatistics(java.lang.String,
	 *      long, long)
	 */
	@Override
	public VmStatistics getVMStatistics(String vmId, long from, long to) throws InternalException, CloudException {
		return novaServer.getVMStatistics(vmId, from, to);
	}

	/**
	 * @param vmId
	 * @param from
	 * @param to
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getVMStatisticsForPeriod(java.lang.String,
	 *      long, long)
	 */
	@Override
	public Iterable<VmStatistics> getVMStatisticsForPeriod(String vmId, long from, long to) throws InternalException,
			CloudException {
		return novaServer.getVMStatisticsForPeriod(vmId, from, to);
	}

	/**
	 * @param cls
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#identifyImageRequirement(org.dasein.cloud.compute.ImageClass)
	 */
	@Deprecated
	@Override
	public Requirement identifyImageRequirement(ImageClass cls) throws CloudException, InternalException {
		return novaServer.identifyImageRequirement(cls);
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#identifyPasswordRequirement()
	 */
	@Deprecated
	@Override
	public Requirement identifyPasswordRequirement() throws CloudException, InternalException {
		return novaServer.identifyPasswordRequirement();
	}

	/**
	 * @param virtualMachineId
	 * @param productId
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#alterVirtualMachineProduct(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public VirtualMachine alterVirtualMachineProduct(String virtualMachineId, String productId) throws InternalException,
			CloudException {
		return novaServer.alterVirtualMachineProduct(virtualMachineId, productId);
	}

	/**
	 * @param platform
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#identifyPasswordRequirement(org.dasein.cloud.compute.Platform)
	 */
	@Deprecated
	@Override
	public Requirement identifyPasswordRequirement(Platform platform) throws CloudException, InternalException {
		return novaServer.identifyPasswordRequirement(platform);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return novaServer.toString();
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#identifyRootVolumeRequirement()
	 */
	@Deprecated
	@Override
	public Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
		return novaServer.identifyRootVolumeRequirement();
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#identifyShellKeyRequirement()
	 */
	@Deprecated
	@Override
	public Requirement identifyShellKeyRequirement() throws CloudException, InternalException {
		return novaServer.identifyShellKeyRequirement();
	}

	/**
	 * @param platform
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#identifyShellKeyRequirement(org.dasein.cloud.compute.Platform)
	 */
	@Deprecated
	@Override
	public Requirement identifyShellKeyRequirement(Platform platform) throws CloudException, InternalException {
		return novaServer.identifyShellKeyRequirement(platform);
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#identifyStaticIPRequirement()
	 */
	@Deprecated
	@Override
	public Requirement identifyStaticIPRequirement() throws CloudException, InternalException {
		return novaServer.identifyStaticIPRequirement();
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#identifyVlanRequirement()
	 */
	@Deprecated
	@Override
	public Requirement identifyVlanRequirement() throws CloudException, InternalException {
		return novaServer.identifyVlanRequirement();
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#isAPITerminationPreventable()
	 */
	@Deprecated
	@Override
	public boolean isAPITerminationPreventable() throws CloudException, InternalException {
		return novaServer.isAPITerminationPreventable();
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#isBasicAnalyticsSupported()
	 */
	@Deprecated
	@Override
	public boolean isBasicAnalyticsSupported() throws CloudException, InternalException {
		return novaServer.isBasicAnalyticsSupported();
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#isSubscribed()
	 */
	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return novaServer.isSubscribed();
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#isExtendedAnalyticsSupported()
	 */
	@Deprecated
	@Override
	public boolean isExtendedAnalyticsSupported() throws CloudException, InternalException {
		return novaServer.isExtendedAnalyticsSupported();
	}

	/**
	 * @param options
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#launch(org.dasein.cloud.compute.VMLaunchOptions)
	 */
	@Override
	public VirtualMachine launch(VMLaunchOptions options) throws CloudException, InternalException {
		return novaServer.launch(options);
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#isUserDataSupported()
	 */
	@Deprecated
	@Override
	public boolean isUserDataSupported() throws CloudException, InternalException {
		return novaServer.isUserDataSupported();
	}

	/**
	 * @param withLaunchOptions
	 * @param count
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#launchMany(org.dasein.cloud.compute.VMLaunchOptions,
	 *      int)
	 */
	@Override
	public Iterable<String> launchMany(VMLaunchOptions withLaunchOptions, int count) throws CloudException,
			InternalException {
		return novaServer.launchMany(withLaunchOptions, count);
	}

	/**
	 * @param fromMachineImageId
	 * @param product
	 * @param dataCenterId
	 * @param name
	 * @param description
	 * @param withKeypairId
	 * @param inVlanId
	 * @param withAnalytics
	 * @param asSandbox
	 * @param firewallIds
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#launch(java.lang.String,
	 *      org.dasein.cloud.compute.VirtualMachineProduct, java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String,
	 *      java.lang.String, boolean, boolean, java.lang.String[])
	 */
	@Deprecated
	@Override
	public VirtualMachine launch(String fromMachineImageId, VirtualMachineProduct product, String dataCenterId, String name,
			String description, String withKeypairId, String inVlanId, boolean withAnalytics, boolean asSandbox,
			String... firewallIds) throws InternalException, CloudException {
		return novaServer.launch(fromMachineImageId, product, dataCenterId, name, description, withKeypairId, inVlanId,
				withAnalytics, asSandbox, firewallIds);
	}

	/**
	 * @param fromMachineImageId
	 * @param product
	 * @param dataCenterId
	 * @param name
	 * @param description
	 * @param withKeypairId
	 * @param inVlanId
	 * @param withAnalytics
	 * @param asSandbox
	 * @param firewallIds
	 * @param tags
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#launch(java.lang.String,
	 *      org.dasein.cloud.compute.VirtualMachineProduct, java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String,
	 *      java.lang.String, boolean, boolean, java.lang.String[],
	 *      org.dasein.cloud.Tag[])
	 */
	@Deprecated
	@Override
	public VirtualMachine launch(String fromMachineImageId, VirtualMachineProduct product, String dataCenterId, String name,
			String description, String withKeypairId, String inVlanId, boolean withAnalytics, boolean asSandbox,
			String[] firewallIds, Tag... tags) throws InternalException, CloudException {
		return novaServer.launch(fromMachineImageId, product, dataCenterId, name, description, withKeypairId, inVlanId,
				withAnalytics, asSandbox, firewallIds, tags);
	}

	/**
	 * @param options
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#listSpotPriceHistories(org.dasein.cloud.compute.SpotPriceHistoryFilterOptions)
	 */
	@Override
	public Iterable<SpotPriceHistory> listSpotPriceHistories(SpotPriceHistoryFilterOptions options) throws CloudException,
			InternalException {
		return novaServer.listSpotPriceHistories(options);
	}

	/**
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#listSupportedArchitectures()
	 */
	@Deprecated
	@Override
	public Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
		return novaServer.listSupportedArchitectures();
	}

	/**
	 * @param vmId
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#listFirewalls(java.lang.String)
	 */
	@Override
	public Iterable<String> listFirewalls(String vmId) throws InternalException, CloudException {
		return novaServer.listFirewalls(vmId);
	}

	/**
	 * @param options
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#listVirtualMachines(org.dasein.cloud.compute.VMFilterOptions)
	 */
	@Override
	public Iterable<VirtualMachine> listVirtualMachines(VMFilterOptions options) throws InternalException, CloudException {
		return novaServer.listVirtualMachines(options);
	}

	/**
	 * @param vmId
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#stop(java.lang.String)
	 */
	@Override
	public final void stop(String vmId) throws InternalException, CloudException {
		novaServer.stop(vmId);
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#supportsAnalytics()
	 */
	@Deprecated
	@Override
	public final boolean supportsAnalytics() throws CloudException, InternalException {
		return novaServer.supportsAnalytics();
	}

	/**
	 * @param vm
	 * @return
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#supportsPauseUnpause(org.dasein.cloud.compute.VirtualMachine)
	 */
	@Deprecated
	@Override
	public boolean supportsPauseUnpause(VirtualMachine vm) {
		return novaServer.supportsPauseUnpause(vm);
	}

	/**
	 * @param flavorId
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#getFlavorRef(java.lang.String)
	 */
	public String getFlavorRef(String flavorId) throws InternalException, CloudException {
		return novaServer.getFlavorRef(flavorId);
	}

	/**
	 * @param vm
	 * @return
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#supportsStartStop(org.dasein.cloud.compute.VirtualMachine)
	 */
	@Deprecated
	@Override
	public boolean supportsStartStop(VirtualMachine vm) {
		return novaServer.supportsStartStop(vm);
	}

	/**
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#listAllProducts()
	 */
	@Override
	public Iterable<VirtualMachineProduct> listAllProducts() throws CloudException, InternalException {
		return novaServer.listAllProducts();
	}

	/**
	 * @param vm
	 * @return
	 * @deprecated
	 * @see org.dasein.cloud.compute.AbstractVMSupport#supportsSuspendResume(org.dasein.cloud.compute.VirtualMachine)
	 */
	@Deprecated
	@Override
	public boolean supportsSuspendResume(VirtualMachine vm) {
		return novaServer.supportsSuspendResume(vm);
	}

	/**
	 * @param machineImageId
	 * @param options
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#listProducts(java.lang.String,
	 *      org.dasein.cloud.compute.VirtualMachineProductFilterOptions)
	 */
	@Override
	public Iterable<VirtualMachineProduct> listProducts(String machineImageId, VirtualMachineProductFilterOptions options)
			throws InternalException, CloudException {
		return novaServer.listProducts(machineImageId, options);
	}

	/**
	 * @param vmId
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#terminate(java.lang.String)
	 */
	@Override
	public void terminate(String vmId) throws CloudException, InternalException {
		novaServer.terminate(vmId);
	}

	/**
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#listVirtualMachineStatus()
	 */
	@Override
	public Iterable<ResourceStatus> listVirtualMachineStatus() throws InternalException, CloudException {
		return novaServer.listVirtualMachineStatus();
	}

	/**
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#listVirtualMachines()
	 */
	@Override
	public Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
		return novaServer.listVirtualMachines();
	}

	/**
	 * @param action
	 * @return
	 * @see org.dasein.cloud.compute.AbstractVMSupport#mapServiceAction(org.dasein.cloud.identity.ServiceAction)
	 */
	@Override
	public String[] mapServiceAction(ServiceAction action) {
		return novaServer.mapServiceAction(action);
	}

	/**
	 * @param vmId
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#pause(java.lang.String)
	 */
	@Override
	public void pause(String vmId) throws InternalException, CloudException {
		novaServer.pause(vmId);
	}

	/**
	 * @param vmIds
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getVMStatus(java.lang.String[])
	 */
	@Override
	public Iterable<VirtualMachineStatus> getVMStatus(String... vmIds) throws InternalException, CloudException {
		return novaServer.getVMStatus(vmIds);
	}

	/**
	 * @param filterOptions
	 * @return
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.compute.AbstractVMSupport#getVMStatus(org.dasein.cloud.compute.VmStatusFilterOptions)
	 */
	@Override
	public Iterable<VirtualMachineStatus> getVMStatus(VmStatusFilterOptions filterOptions) throws InternalException,
			CloudException {
		return novaServer.getVMStatus(filterOptions);
	}

	/**
	 * @param vmId
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#resume(java.lang.String)
	 */
	@Override
	public void resume(String vmId) throws InternalException, CloudException {
		novaServer.resume(vmId);
	}

	/**
	 * @param vmId
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#start(java.lang.String)
	 */
	@Override
	public void start(String vmId) throws InternalException, CloudException {
		novaServer.start(vmId);
	}

	/**
	 * @param vmId
	 * @param force
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#stop(java.lang.String,
	 *      boolean)
	 */
	@Override
	public void stop(String vmId, boolean force) throws InternalException, CloudException {
		novaServer.stop(vmId, force);
	}

	/**
	 * @param vmId
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#suspend(java.lang.String)
	 */
	@Override
	public void suspend(String vmId) throws InternalException, CloudException {
		novaServer.suspend(vmId);
	}

	/**
	 * @param vmId
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#unpause(java.lang.String)
	 */
	@Override
	public void unpause(String vmId) throws InternalException, CloudException {
		novaServer.unpause(vmId);
	}

	/**
	 * @param vmId
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#reboot(java.lang.String)
	 */
	@Override
	public void reboot(String vmId) throws CloudException, InternalException {
		novaServer.reboot(vmId);
	}

	/**
	 * @param vmId
	 * @param explanation
	 * @throws InternalException
	 * @throws CloudException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#terminate(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void terminate(String vmId, String explanation) throws InternalException, CloudException {
		novaServer.terminate(vmId, explanation);
	}

	/**
	 * @param vmId
	 * @param tags
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#setTags(java.lang.String,
	 *      org.dasein.cloud.Tag[])
	 */
	@Override
	public void setTags(String vmId, Tag... tags) throws CloudException, InternalException {
		novaServer.setTags(vmId, tags);
	}

	/**
	 * @param vmIds
	 * @param tags
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#setTags(java.lang.String[],
	 *      org.dasein.cloud.Tag[])
	 */
	@Override
	public void setTags(String[] vmIds, Tag... tags) throws CloudException, InternalException {
		novaServer.setTags(vmIds, tags);
	}

	/**
	 * @param vmId
	 * @param tags
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#updateTags(java.lang.String,
	 *      org.dasein.cloud.Tag[])
	 */
	@Override
	public void updateTags(String vmId, Tag... tags) throws CloudException, InternalException {
		novaServer.updateTags(vmId, tags);
	}

	/**
	 * @param vmIds
	 * @param tags
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#updateTags(java.lang.String[],
	 *      org.dasein.cloud.Tag[])
	 */
	@Override
	public void updateTags(String[] vmIds, Tag... tags) throws CloudException, InternalException {
		novaServer.updateTags(vmIds, tags);
	}

	/**
	 * @param vmId
	 * @param tags
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#removeTags(java.lang.String,
	 *      org.dasein.cloud.Tag[])
	 */
	@Override
	public void removeTags(String vmId, Tag... tags) throws CloudException, InternalException {
		novaServer.removeTags(vmId, tags);
	}

	/**
	 * @param vmIds
	 * @param tags
	 * @throws CloudException
	 * @throws InternalException
	 * @see org.dasein.cloud.openstack.nova.os.compute.NovaServer#removeTags(java.lang.String[],
	 *      org.dasein.cloud.Tag[])
	 */
	@Override
	public void removeTags(String[] vmIds, Tag... tags) throws CloudException, InternalException {
		novaServer.removeTags(vmIds, tags);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.compute.SkyportVirtualMachineSupport#
	 * listMinimalVirtualMachines()
	 */
	@Override
	public Iterable<MinimalResource> listMinimalVirtualMachines() throws InternalException, CloudException {
		JSONObject ob = novaServer.getMethod().getServers("/servers", null, true);
		List<MinimalResource> servers = new ArrayList<MinimalResource>();

		try {
			if (ob != null && ob.has("servers")) {
				JSONArray list = ob.getJSONArray("servers");

				for (int i = 0; i < list.length(); i++) {
					JSONObject server = list.getJSONObject(i);
					MinimalResource vm = toMinimalResource(server);

					if (vm != null) {
						servers.add(vm);
					}

				}
			}
		} catch (JSONException e) {
			logger.error("listMinimalResource(): Unable to identify expected values in JSON: " + e.getMessage());
			e.printStackTrace();
			throw new CloudException(CloudErrorType.COMMUNICATION, 200, "invalidJson",
					"Missing JSON element for servers in " + ob.toString());
		}
		return servers;

	}

	/**
	 * @param server
	 * @return
	 * @throws JSONException
	 */
	private MinimalResource toMinimalResource(JSONObject server) throws JSONException {
		if (server == null) {
			return null;
		}
		String serverId = null;
		String serverName = null;
		if (server.has("id")) {
			serverId = server.getString("id");
		}
		if (serverId == null) {
			return null;
		}
		if (server.has("name")) {
			serverName = server.getString("name");
		}

		return new MinimalResource(serverId, serverName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infinities.skyport.compute.SkyportVirtualMachineSupport#
	 * listNovaStyleVirtualMachines()
	 */
	@Override
	public Iterable<NovaStyleVirtualMachine> listNovaStyleVirtualMachines() throws InternalException, CloudException {
		List<NovaStyleVirtualMachine> servers = new ArrayList<NovaStyleVirtualMachine>();
		JSONObject ob = novaServer.getMethod().getServers("/servers", null, true);
		try {
			Iterable<IpAddress> ipv4 = Collections.emptyList(), ipv6 = Collections.emptyList();
			Iterable<VLAN> nets = Collections.emptyList();
			NetworkServices services = novaServer.getNovaOpenStack().getNetworkServices();

			if (services != null) {
				IpAddressSupport support = services.getIpAddressSupport();

				if (support != null) {
					ipv4 = support.listIpPool(IPVersion.IPV4, false);
					ipv6 = support.listIpPool(IPVersion.IPV6, false);
				}

				VLANSupport vs = services.getVlanSupport();

				if (vs != null) {
					nets = vs.listVlans();
				}
			}

			if (ob != null && ob.has("servers")) {
				JSONArray list = ob.getJSONArray("servers");

				for (int i = 0; i < list.length(); i++) {
					JSONObject server = list.getJSONObject(i);
					NovaStyleVirtualMachine vm = toNovaStyleVirtualMachine(server, ipv4, ipv6, nets);

					if (vm != null) {
						servers.add(vm);
					}

				}
			}
		} catch (JSONException e) {
			logger.error("listVirtualMachines(): Unable to identify expected values in JSON: " + e.getMessage());
			e.printStackTrace();
			throw new CloudException(CloudErrorType.COMMUNICATION, 200, "invalidJson",
					"Missing JSON element for servers in " + ob.toString());
		}
		return servers;
	}

	/**
	 * @param server
	 * @param nets
	 * @param ipv6
	 * @param ipv4
	 * @return
	 * @throws InternalException
	 * @throws JSONException
	 * @throws CloudException
	 */
	private NovaStyleVirtualMachine toNovaStyleVirtualMachine(JSONObject server, Iterable<IpAddress> ipv4,
			Iterable<IpAddress> ipv6, Iterable<VLAN> networks) throws InternalException, JSONException, CloudException {
		if (server == null) {
			return null;
		}
		NovaStyleVirtualMachine vm = new NovaStyleVirtualMachine();
		String description = null;

		// vm.setCurrentState(VmState.RUNNING);
		vm.setArchitecture(Architecture.I64);
		vm.setClonable(false);
		vm.setCreationTimestamp(-1L);
		vm.setImagable(false);
		vm.setLastBootTimestamp(-1L);
		vm.setLastPauseTimestamp(-1L);
		vm.setPausable(false);
		vm.setPersistent(true);
		vm.setPlatform(Platform.UNKNOWN);
		vm.setRebootable(true);
		vm.setProviderOwnerId(novaServer.getTenantId());

		if (novaServer.getCloudProvider().equals(OpenStackProvider.RACKSPACE)) {
			vm.setPersistent(false);
		}

		if (server.has("id")) {
			vm.setProviderVirtualMachineId(server.getString("id"));
		} else
			return null;
		if (server.has("name")) {
			vm.setName(server.getString("name"));
		}
		if (server.has("description") && !server.isNull("description")) {
			description = server.getString("description");
		}
		if (server.has("kernel_id")) {
			vm.setProviderKernelImageId(server.getString("kernel_id"));
		}
		if (server.has("ramdisk_id")) {
			vm.setProviderRamdiskImageId(server.getString("ramdisk_id"));
		}
		JSONObject md = (server.has("metadata") && !server.isNull("metadata")) ? server.getJSONObject("metadata") : null;

		Map<String, String> map = new HashMap<String, String>();
		boolean imaging = false;

		if (md != null) {
			if (md.has("org.dasein.description") && vm.getDescription() == null) {
				description = md.getString("org.dasein.description");
			} else if (md.has("Server Label")) {
				description = md.getString("Server Label");
			}
			if (md.has("org.dasein.platform")) {
				try {
					vm.setPlatform(Platform.valueOf(md.getString("org.dasein.platform")));
				} catch (Throwable ignore) {
					// ignore
				}
			}
			String[] keys = JSONObject.getNames(md);

			if (keys != null) {
				for (String key : keys) {
					String value = md.getString(key);

					if (value != null) {
						map.put(key, value);
					}
				}
			}
		}
		if (server.has("OS-EXT-STS:task_state") && !server.isNull("OS-EXT-STS:task_state")) {
			String t = server.getString("OS-EXT-STS:task_state");

			map.put("OS-EXT-STS:task_state", t);
			imaging = t.equalsIgnoreCase("image_snapshot");
		}
		if (description == null) {
			if (vm.getName() == null) {
				vm.setName(vm.getProviderVirtualMachineId());
			}
			vm.setDescription(vm.getName());
		} else {
			vm.setDescription(description);
		}
		if (server.has("hostId")) {
			map.put("host", server.getString("hostId"));
		}
		vm.setTags(map);
		if (server.has("image") && !server.isNull("image")) {
			try {
				JSONObject img = server.getJSONObject("image");

				if (img.has("id")) {
					vm.setProviderMachineImageId(img.getString("id"));
				}
			} catch (JSONException ex) {
				logger.error("Unable to parse the image object");
				try {
					server.getString("image");
					logger.error("Image object has been returned as a string from cloud " + server.getString("image"));
				} catch (JSONException ignore) {
				}
			}
		}
		if (server.has("flavor")) {
			JSONObject f = server.getJSONObject("flavor");

			if (f.has("id")) {
				vm.setProductId(f.getString("id"));
			}
		} else if (server.has("flavorId")) {
			vm.setProductId(server.getString("flavorId"));
		}
		if (server.has("adminPass")) {
			vm.setRootPassword(server.getString("adminPass"));
		}
		if (server.has("key_name")) {
			vm.setProviderShellKeyIds(server.getString("key_name"));
		}
		if (server.has("status")) {
			String s = server.getString("status").toLowerCase();

			if (s.equals("active")) {
				vm.setCurrentState(VmState.RUNNING);
			} else if (s.startsWith("build")) {
				vm.setCurrentState(VmState.PENDING);
			} else if (s.equals("deleted")) {
				vm.setCurrentState(VmState.TERMINATED);
			} else if (s.equals("suspended")) {
				vm.setCurrentState(VmState.SUSPENDED);
			} else if (s.equalsIgnoreCase("paused")) {
				vm.setCurrentState(VmState.PAUSED);
			} else if (s.equalsIgnoreCase("stopped") || s.equalsIgnoreCase("shutoff")) {
				vm.setCurrentState(VmState.STOPPED);
			} else if (s.equalsIgnoreCase("stopping")) {
				vm.setCurrentState(VmState.STOPPING);
			} else if (s.equalsIgnoreCase("pausing")) {
				vm.setCurrentState(VmState.PAUSING);
			} else if (s.equalsIgnoreCase("suspending")) {
				vm.setCurrentState(VmState.SUSPENDING);
			} else if (s.equals("error")) {
				vm.setCurrentState(VmState.ERROR);
			} else if (s.equals("reboot") || s.equals("hard_reboot")) {
				vm.setCurrentState(VmState.REBOOTING);
			} else {
				logger.warn("toVirtualMachine(): Unknown server state: " + s);
				vm.setCurrentState(VmState.PENDING);
			}
		}
		if (vm.getCurrentState() == null && imaging) {
			vm.setCurrentState(VmState.PENDING);
		}
		if (server.has("created")) {
			vm.setCreationTimestamp(NovaOpenStack.parseTimestamp(server.getString("created")));
		}
		if (server.has("addresses")) {
			JSONObject addrs = server.getJSONObject("addresses");
			String[] names = JSONObject.getNames(addrs);

			if (names != null && names.length > 0) {
				List<SkyportRawAddress> pub = new ArrayList<SkyportRawAddress>();
				List<SkyportRawAddress> priv = new ArrayList<SkyportRawAddress>();

				for (String name : names) {
					JSONArray arr = addrs.getJSONArray(name);

					String subnet = null;
					for (int i = 0; i < arr.length(); i++) {
						SkyportRawAddress addr = null;
						String type = null;

						if (novaServer.getMinorVersion() == 0 && novaServer.getMajorVersion() == 1) {
							addr = new SkyportRawAddress(arr.getString(i).trim(), IPVersion.IPV4, name);
						} else {
							JSONObject a = arr.getJSONObject(i);
							type = a.optString("OS-EXT-IPS:type");

							if (a.has("version") && a.getInt("version") == 4 && a.has("addr")) {
								subnet = a.getString("addr");
								addr = new SkyportRawAddress(a.getString("addr"), IPVersion.IPV4, name);
							} else if (a.has("version") && a.getInt("version") == 6 && a.has("addr")) {
								subnet = a.getString("addr");
								addr = new SkyportRawAddress(a.getString("addr"), IPVersion.IPV6, name);
							}
						}
						if (addr != null) {
							if ("public".equalsIgnoreCase(name) || "internet".equalsIgnoreCase(name)) {
								pub.add(addr);
							} else if ("floating".equalsIgnoreCase(type)) {
								pub.add(addr);
							} else if ("fixed".equalsIgnoreCase(type)) {
								priv.add(addr);
							} else if (addr.isPublicIpAddress()) {
								pub.add(addr);
							} else {
								priv.add(addr);
							}
						}
					}
					if (vm.getProviderVlanId() == null) { // &&
															// !name.equals("public")
															// &&
															// !name.equals("private")
															// &&
															// !name.equals("nova_fixed")
															// ) {
						for (VLAN network : networks) {
							if (network.getName().equals(name)) {
								vm.setProviderVlanId(network.getProviderVlanId());
								// get subnet
								NetworkServices services = novaServer.getNovaOpenStack().getNetworkServices();
								VLANSupport support = services.getVlanSupport();
								Iterable<Subnet> subnets = support.listSubnets(network.getProviderVlanId());
								for (Subnet sub : subnets) {
									try {
										SubnetUtils utils = new SubnetUtils(sub.getCidr());

										if (utils.getInfo().isInRange(subnet)) {
											vm.setProviderSubnetId(sub.getProviderSubnetId());
											break;
										}
									} catch (IllegalArgumentException arg) {
										logger.warn("Couldn't match against an invalid CIDR: " + sub.getCidr());
										continue;
									}
								}
								break;
							}
						}
					}
				}
				vm.setPublicAddresses(pub.toArray(new SkyportRawAddress[pub.size()]));
				vm.setPrivateAddresses(priv.toArray(new SkyportRawAddress[priv.size()]));
			}
			SkyportRawAddress[] raw = vm.getPublicAddresses();

			if (raw != null) {
				for (SkyportRawAddress addr : vm.getPublicAddresses()) {
					if (addr.getVersion().equals(IPVersion.IPV4)) {
						for (IpAddress a : ipv4) {
							if (a.getRawAddress().getIpAddress().equals(addr.getIpAddress())) {
								vm.setProviderAssignedIpAddressId(a.getProviderIpAddressId());
								break;
							}
						}
					} else if (addr.getVersion().equals(IPVersion.IPV6)) {
						for (IpAddress a : ipv6) {
							if (a.getRawAddress().getIpAddress().equals(addr.getIpAddress())) {
								vm.setProviderAssignedIpAddressId(a.getProviderIpAddressId());
								break;
							}
						}
					}
				}
			}
			if (vm.getProviderAssignedIpAddressId() == null) {
				for (IpAddress addr : ipv4) {
					String serverId = addr.getServerId();

					if (serverId != null && serverId.equals(vm.getProviderVirtualMachineId())) {
						vm.setProviderAssignedIpAddressId(addr.getProviderIpAddressId());
						break;
					}
				}
				if (vm.getProviderAssignedIpAddressId() == null) {
					for (IpAddress addr : ipv6) {
						String serverId = addr.getServerId();

						if (serverId != null && addr.getServerId().equals(vm.getProviderVirtualMachineId())) {
							vm.setProviderAssignedIpAddressId(addr.getProviderIpAddressId());
							break;
						}
					}
				}
			}
			if (vm.getProviderAssignedIpAddressId() == null) {
				for (IpAddress addr : ipv6) {
					if (addr.getServerId().equals(vm.getProviderVirtualMachineId())) {
						vm.setProviderAssignedIpAddressId(addr.getProviderIpAddressId());
						break;
					}
				}
			}
		}
		vm.setProviderRegionId(novaServer.getRegionId());
		vm.setProviderDataCenterId(vm.getProviderRegionId() + "-a");
		vm.setTerminationTimestamp(-1L);
		if (vm.getName() == null) {
			vm.setName(vm.getProviderVirtualMachineId());
		}
		if (vm.getDescription() == null) {
			vm.setDescription(vm.getName());
		}

		if (Platform.UNKNOWN.equals(vm.getPlatform())) {
			vm.setPlatform(novaServer.getPlatform(vm.getName(), vm.getDescription(), vm.getProviderMachineImageId()));
		}
		vm.setImagable(vm.getCurrentState() == null);
		vm.setRebootable(vm.getCurrentState() == null);

		if (novaServer.getCloudProvider().equals(OpenStackProvider.RACKSPACE)) {
			// Rackspace does not support the concept for firewalls in servers
			vm.setProviderFirewallIds(null);
		} else {
			Iterable<String> fwIds = novaServer.listFirewalls(vm.getProviderVirtualMachineId(), server);
			int count = Iterators.size(fwIds.iterator());

			String[] ids = new String[count];
			int i = 0;

			for (String id : fwIds) {
				ids[i++] = id;
			}
			vm.setProviderFirewallIds(ids);
		}
		return vm;
	}

}
