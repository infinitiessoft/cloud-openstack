package org.dasein.cloud.openstack.nova.os.storage;

import org.dasein.cloud.openstack.nova.os.NovaOpenStack;

import com.infinities.skyport.storage.SkyportBlobStoreSupport;
import com.infinities.skyport.storage.SkyportStorageServices;

public class SkyportSwiftStorageServices extends SwiftStorageServices implements SkyportStorageServices{

	public SkyportSwiftStorageServices(NovaOpenStack provider) {
		super(provider);
	}
	
	@Override
	public SkyportBlobStoreSupport getSkyportOnlineStorageSupport() {
		return new SkyportSwiftBlobStore(getProvider());
	}

}
