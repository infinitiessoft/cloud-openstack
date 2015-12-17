package com.infinities.skyport;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.apache.http.HttpStatus;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.openstack.nova.os.AuthenticationContext;
import org.dasein.cloud.openstack.nova.os.NovaException;
import org.dasein.cloud.openstack.nova.os.NovaMethod;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;

import com.infinities.skyport.annotation.Provider;

@Provider(enumeration = { "DELL", "DREAMHOST", "HP", "IBM", "METACLOUD", "RACKSPACE", "OTHER" })
public class OpenstackServiceProvider extends NovaOpenStack implements ServiceProvider {

	@Override
	public void initialize() {

	}

	@Override
	public synchronized @Nonnull AuthenticationContext getAuthenticationContext() throws CloudException, InternalException {
		APITrace.begin(this, "Cloud.getAuthenticationContext");
		try {
			Cache<AuthenticationContext> cache =
					Cache.getInstance(this, "authenticationContext", AuthenticationContext.class, CacheLevel.REGION_ACCOUNT,
							new TimePeriod<Day>(1, TimePeriod.DAY));
			ProviderContext ctx = getContext();

			if (ctx == null) {
				throw new CloudException("No context was set for this request");
			}
			Iterable<AuthenticationContext> current = cache.get(ctx);
			AuthenticationContext authenticationContext = null;

			NovaMethod method = new CustomNovaMethod(this);

			if (current != null) {
				authenticationContext = current.iterator().next();
			} else {
				try {
					authenticationContext = method.authenticate();
				} finally {
					if (authenticationContext == null) {
						NovaException.ExceptionItems items = new NovaException.ExceptionItems();

						items.code = HttpStatus.SC_UNAUTHORIZED;
						items.type = CloudErrorType.AUTHENTICATION;
						items.message = "unauthorized";
						items.details = "The API keys failed to authenticate with the specified endpoint.";
						throw new NovaException(items);
					}
					cache.put(ctx, Collections.singletonList(authenticationContext));
					return authenticationContext;
				}
			}
			return authenticationContext;
		} finally {
			APITrace.end();
		}
	}

	@Override
	public @Nonnull ContextRequirements getContextRequirements() {
		return new ContextRequirements(new ContextRequirements.Field("apiKey", "The API Keypair",
				ContextRequirements.FieldType.KEYPAIR, ContextRequirements.Field.ACCESS_KEYS, true),
				new ContextRequirements.Field("urlType", "The API URL Type: public or internal",
						ContextRequirements.FieldType.TEXT, true));
	}
}
