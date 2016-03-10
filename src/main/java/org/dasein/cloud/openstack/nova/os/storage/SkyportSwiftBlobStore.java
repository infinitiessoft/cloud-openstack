package org.dasein.cloud.openstack.nova.os.storage;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Tag;
import org.dasein.cloud.openstack.nova.os.NovaOpenStack;
import org.dasein.cloud.openstack.nova.os.SwiftMethod;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.TagUtils;

import com.infinities.skyport.openstack.nova.os.SkyportSwiftMethod;
import com.infinities.skyport.storage.SkyportBlobStoreSupport;

public class SkyportSwiftBlobStore extends SwiftBlobStore implements SkyportBlobStoreSupport {

	public SkyportSwiftBlobStore(NovaOpenStack provider) {
		super(provider);
	}

	@Override
	public void copy(@Nullable String sourceBucket, @Nullable String sourceObject, @Nullable String targetBucket,
			@Nullable String targetObject) throws InternalException, CloudException {
		APITrace.begin(getProvider(), "Blob.copy");
		try {
			if (sourceBucket == null || !exists(sourceBucket)) {
				throw new CloudException("No source bucket was specified");
			}
			if (targetBucket == null) {
				throw new CloudException("No target bucket was specified");
			}
			if (sourceObject == null) {
				throw new CloudException("No source object was specified");
			}
			if (targetObject == null) {
				targetObject = sourceObject;
			}
			copyFile(sourceBucket, sourceObject, targetBucket, targetObject);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Map<String, String> getMetadata(String ... strs) throws CloudException, InternalException {
		SwiftMethod method = new SwiftMethod(getProvider());
		Map<String, String> metaData = null;
		if (strs.length == 1) {
			 metaData = method.head(strs[0]);
		} else if (strs.length == 2) {
			metaData = method.head(strs[0], strs[1]);
		}
		if (metaData == null) {
			throw new IllegalArgumentException("Error Argument Length");
		} else {
			return metaData;
		}
	}

	@Override
    public void setTags( @Nonnull String bucketNames, @Nonnull Tag... tags ) throws CloudException, InternalException {
        setTags(new String[]{bucketNames}, tags);
    }

    @Override
    public void setTags( @Nonnull String[] bucketNames, @Nonnull Tag... tags ) throws CloudException, InternalException {
        for( String id : bucketNames ) {

            Tag[] collectionForDelete = TagUtils.getTagsForDelete(getBucket(id).getTags(), tags);

            if( collectionForDelete.length != 0 ) {
                removeTags(id, collectionForDelete);
            }

            updateTags(id, tags);
        }
    }
	
	
	@Override
	public void setObjectTags(@Nonnull String bucket, @Nonnull String object, @Nonnull Tag... tags) throws CloudException, InternalException {
		setObjectTags(bucket, new String[]{object}, tags);
	}
	
	@Override
	public void setObjectTags(@Nonnull String bucketName, @Nonnull String[] objectNames, @Nonnull Tag... tags) throws InternalException, CloudException {
		
		for (String id : objectNames) {
			
			Tag[] collectionForDelete = TagUtils.getTagsForDelete(getObject(bucketName, id).getTags(), tags);
			
			if( collectionForDelete.length != 0 ) {
                removeTags(id, collectionForDelete);
            }

			updateObjectTags(bucketName, id, tags);
		}
	}
	
	public void updateObjectTags(@Nonnull String bucketName, @Nonnull String objectName, @Nonnull Tag ... tags) throws CloudException, InternalException {
    	APITrace.begin(getProvider(), "Object.updateTags");
    	try {
    		SkyportSwiftMethod method = new SkyportSwiftMethod(getProvider());
    		method.put( bucketName , objectName, "X-Object-Meta-", tags);
    	}
    	finally {
    		APITrace.end();
    	}
    }
    
    @Override
    public void updateObjectTags(@Nonnull String bucketName, @Nonnull String[] objectNames, @Nonnull Tag ... tags) throws CloudException, InternalException {
    	for( String id : objectNames) {
    		updateObjectTags(bucketName, id, tags);
    	}
    }
    
    @Override
    public void removeObjectTags(@Nonnull String bucketName,@Nonnull String objectName, @Nonnull Tag ... tags) throws CloudException, InternalException {
    	APITrace.begin(getProvider(), "Object.removeTags");
    	try {
    		SkyportSwiftMethod method = new SkyportSwiftMethod(getProvider());
    		method.put( bucketName , objectName, "X-Remove-Object-Meta-", tags);
    	}
    	finally {
    		APITrace.end();
    	}
    }
    
    @Override
    public void removeObjectTags(@Nonnull String bucketName, @Nonnull String[] objectNames, @Nonnull Tag ... tags) throws CloudException, InternalException {
    	for( String id : objectNames ) {
    		removeObjectTags(bucketName, id, tags);
    	}
    }
}