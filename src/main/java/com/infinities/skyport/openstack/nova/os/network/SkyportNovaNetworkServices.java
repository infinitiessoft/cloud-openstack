package com.infinities.skyport.openstack.nova.os.network;

import javax.annotation.Nullable;

import org.dasein.cloud.network.VpnSupport;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.openstack.nova.os.compute.CustomNovaServer;
import org.dasein.cloud.openstack.nova.os.network.NovaNetworkServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.compute.SkyportNetworkServices;

public class SkyportNovaNetworkServices extends NovaNetworkServices implements SkyportNetworkServices {

	private static final Logger logger = LoggerFactory.getLogger(SkyportNovaNetworkServices.class);
	private CustomNovaServer novaServer;

	
	public SkyportNovaNetworkServices(NovaOpenStack cloud) {
		super(cloud);
	}
	
	@Override
    public @Nullable VpnSupport getVpnSupport() {
        return super.getVpnSupport();
    }

}
