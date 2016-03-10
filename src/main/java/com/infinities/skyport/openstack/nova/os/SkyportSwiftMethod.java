package com.infinities.skyport.openstack.nova.os;

import java.util.HashMap;

import javax.annotation.Nonnull;

import org.apache.http.HttpStatus;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Tag;
import org.dasein.cloud.openstack.nova.os.AuthenticationContext;
import org.dasein.cloud.openstack.nova.os.NovaException;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.openstack.nova.os.SwiftMethod;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyportSwiftMethod extends SwiftMethod{
	
	private static final Logger logger = LoggerFactory.getLogger(SkyportSwiftMethod.class);

	protected NovaOpenStack provider;
	
	public SkyportSwiftMethod(NovaOpenStack provider) {
		super(provider);
		this.provider = provider;
	}
	
	public void put(@Nonnull String bucket, @Nonnull String object, @Nonnull String prefix, @Nonnull Tag ... tags) throws CloudException, InternalException {
    	AuthenticationContext context = provider.getAuthenticationContext();
    	String endpoint = context.getStorageUrl();
    	if( endpoint == null ) {
    		throw new CloudException("No storage endpoint exists for " + context.getMyRegion());
    	}
    	try {
    		HashMap<String,String> customHeaders = new HashMap<String,String>();
    		for (int i = 0; i < tags.length ; i++ ) {
    			customHeaders.put(prefix + tags[i].getKey(), tags[i].getValue() != null ? tags[i].getValue() : "");
    		}
    		putHeaders(context.getAuthToken(), endpoint, "/" + bucket + "/" + object, customHeaders);
    	}
    	catch (NovaException ex) {
    		if (ex.getHttpCode() == HttpStatus.SC_UNAUTHORIZED) {
    			Cache<AuthenticationContext> cache = Cache.getInstance(provider, "authenticationContext", AuthenticationContext.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
    			cache.clear();
    			put(bucket, prefix, tags);
    		}
    		else {
    			logger.error("Error while updating the tags for bucket - " + bucket + ": " + ex.getMessage());
    		}
    	}
    }

}
