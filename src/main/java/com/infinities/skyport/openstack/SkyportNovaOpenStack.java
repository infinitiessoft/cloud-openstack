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
package com.infinities.skyport.openstack;

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

/**
 * This is a customized version of
 * org.dasein.cloud.openstack.nova.os.NovaOpenStack.
 * <p>
 * Created by Pohsun Huang: 12/23/15 10:57 AM
 * </p>
 * 
 * @author Pohsun Huang
 * @version 2015.12 initial version
 * @since 2015.12
 */
public class SkyportNovaOpenStack extends NovaOpenStack {

	@Override
	public synchronized @Nonnull AuthenticationContext getAuthenticationContext() throws CloudException, InternalException {
		APITrace.begin(this, "Cloud.getAuthenticationContext");
		try {
			Cache<AuthenticationContext> cache = Cache.getInstance(this, "authenticationContext",
					AuthenticationContext.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
			ProviderContext ctx = getContext();

			if (ctx == null) {
				throw new CloudException("No context was set for this request");
			}
			Iterable<AuthenticationContext> current = cache.get(ctx);
			AuthenticationContext authenticationContext = null;

			NovaMethod method = new SkyportNovaMethod(this);

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
				new ContextRequirements.Field("urlType", "The API URL Type: public or internal(default:public)",
						ContextRequirements.FieldType.TEXT, false));
	}
}
