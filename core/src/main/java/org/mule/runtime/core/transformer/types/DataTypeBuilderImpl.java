/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.types;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.util.generics.GenericsUtils.getCollectionType;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.MimeTypes;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * Provides a way to build immutable {@link DataType} objects.
 *
 * @since 4.0
 */
public class DataTypeBuilderImpl<T> implements DataTypeBuilder<T>
{
    private static ConcurrentHashMap<String, ProxyIndicator> proxyClassCache = new ConcurrentHashMap<>();

    private static LoadingCache<DataTypeBuilderImpl, DataType> dataTypeCache = newBuilder().softValues().build(new CacheLoader<DataTypeBuilderImpl, DataType>()
    {
        @Override
        public DataType load(DataTypeBuilderImpl key) throws Exception
        {
            return key.doBuild();
        }
    });

    private static final String CHARSET_PARAM = "charset";

    private Class<T> type = (Class<T>) Object.class;
    private Class<?> itemType = Object.class;
    private String mimeType = MimeTypes.ANY;
    private String encoding = null;

    private AtomicBoolean built = new AtomicBoolean(false);

    /**
     * Sets the given type for the {@link DataType} to be built. See {@link DataType#getType()}.
     * 
     * @param type the java type to set.
     * @return this builder.
     */
    @Override
    public DataTypeBuilder<T> type(Class<T> type)
    {
        validateAlreadyBuilt();

        checkNotNull(type, "'type' cannot be null.");
        this.type = (Class<T>) handleProxy(type);

        return this;
    }

    /*
     * Special case where proxies are used for testing.
     */
    protected Class<?> handleProxy(Class<?> type)
    {
        if (isProxyClass(type))
        {
            return type.getInterfaces()[0];
        }
        else
        {
            return type;
        }
    }

    /**
     * Cache which classes are proxies. Very experimental
     */
    protected static <T> boolean isProxyClass(Class<T> type)
    {
        String typeName = type.getName();
        ProxyIndicator indicator = proxyClassCache.get(typeName);
        if (indicator != null)
        {
            Class classInMap = indicator.getTargetClass();
            if (classInMap == type)
            {
                return indicator.isProxy();
            }
            else if (classInMap != null)
            {
                // We have duplicate class names from different active classloaders. Skip the
                // optimization for this one
                return Proxy.isProxyClass(type);
            }
        }
        // Either there's no indicator in the map or there's one that is due to be replaced
        boolean isProxy = Proxy.isProxyClass(type);
        proxyClassCache.put(typeName, new ProxyIndicator(type, isProxy));
        return isProxy;
    }

    /**
     * map value
     */
    private static final class ProxyIndicator
    {
        private final WeakReference<Class> targetClassRef;
        private final boolean isProxy;

        ProxyIndicator(Class targetClass, boolean proxy)
        {
            this.targetClassRef = new WeakReference<Class>(targetClass);
            isProxy = proxy;
        }

        public Class getTargetClass()
        {
            return targetClassRef.get();
        }

        public boolean isProxy()
        {
            return isProxy;
        }
    }

    /**
     * Sets the given types for the {@link CollectionDataType} to be built. See
     * {@link CollectionDataType#getType()} and {@link CollectionDataType#getItemType()}.
     * 
     * @param collectionType the java collection type to set.
     * @param itemType the java type to set.
     * @return this builder.
     * @throws IllegalArgumentException if the given collectionType is not a descendant of
     *             {@link Collection}.
     */
    @Override
    public <I> DataTypeBuilder<T> collectionType(Class<T> collectionType, Class<I> itemType)
    {
        validateAlreadyBuilt();

        checkNotNull(collectionType, "'collectionType' cannot be null.");
        if (!Collection.class.isAssignableFrom(collectionType))
        {
            throw new IllegalArgumentException("collectionType " + collectionType.getName() + " is not a Collection type");
        }
        checkNotNull(itemType, "'itemType' cannot be null.");

        this.type = (Class<T>) handleProxy(collectionType);
        this.itemType = handleProxy(itemType);
        return this;
    }

    /**
     * Sets the given mimeType string. See {@link DataType#getMimeType()}.
     * <p>
     * If the MIME type for the given string has a {@code charset} parameter, that will be set as
     * the encoding for the {@link DataType} being built. That encoding can be overridden by calling
     * {@link #encoding(String)}.
     * 
     * @param mimeType the MIME type string to set
     * @return this builder.
     * @throws IllegalArgumentException if the given MIME type string is invalid.
     */
    @Override
    public DataTypeBuilder<T> mimeType(String mimeType) throws IllegalArgumentException
    {
        validateAlreadyBuilt();

        if (mimeType == null)
        {
            this.mimeType = MimeTypes.ANY;
        }
        else
        {
            try
            {
                MimeType mt = new MimeType(mimeType);
                this.mimeType = mt.getPrimaryType() + "/" + mt.getSubType();
                if (mt.getParameter(CHARSET_PARAM) != null)
                {
                    encoding = mt.getParameter(CHARSET_PARAM);
                }
            }
            catch (MimeTypeParseException e)
            {
                throw new IllegalArgumentException("MimeType cannot be parsed: " + mimeType);
            }
        }
        return this;
    }

    /**
     * Sets the given encoding. See {@link DataType#getEncoding()}.
     * 
     * @param encoding the encoding to set.
     * @return this builder.
     */
    @Override
    public DataTypeBuilder<T> encoding(String encoding)
    {
        validateAlreadyBuilt();

        if (encoding != null && encoding.trim().length() > 0)
        {
            // Checks that the encoding is valid and supported
            Charset.forName(encoding);
        }

        this.encoding = encoding;

        return this;
    }

    /**
     * Copies the attributes form the given {@link DataType} into this builder.
     * <p>
     * This is useful for using existing {@link DataType}s as templates for new ones. Just copying
     * them is not necessary since those are immutable.
     * 
     * @return this builder.
     */
    @Override
    public DataTypeBuilder<T> from(DataType<T> typeFrom)
    {
        validateAlreadyBuilt();

        if (typeFrom instanceof CollectionDataType)
        {
            this.type = typeFrom.getType();
            this.itemType = ((CollectionDataType) typeFrom).getItemType();
        }
        else
        {
            this.type = typeFrom.getType();
        }

        this.mimeType = typeFrom.getMimeType();
        this.encoding = typeFrom.getEncoding();

        return this;
    }

    /**
     * Builds a new {@link DataType} with the values set in this builder.
     * 
     * @return a newly built {@link DataType}.
     */
    @Override
    public DataType<T> build()
    {
        if (!built.compareAndSet(false, true))
        {
            throwAlreadyBuilt();
        }

        return dataTypeCache.getUnchecked(this);
    }

    protected DataType<T> doBuild()
    {
        if (Collection.class.isAssignableFrom(type))
        {
            if (itemType == null)
            {
                itemType = getCollectionType((Class<? extends Collection<?>>) type);
            }

            return new CollectionDataType(type, itemType, mimeType, encoding);
        }
        else
        {
            return new SimpleDataType<>(type, mimeType, encoding);
        }
    }

    protected void validateAlreadyBuilt()
    {
        if (built.get())
        {
            throwAlreadyBuilt();
        }
    }

    protected void throwAlreadyBuilt()
    {
        throw new IllegalStateException("DataType was already built from this builder. Reusing builder instances is not allowed.");
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, itemType, mimeType, encoding);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }
        if (obj.getClass() != getClass())
        {
            return false;
        }
        DataTypeBuilderImpl other = (DataTypeBuilderImpl) obj;

        return Objects.equals(type, other.type)
               && Objects.equals(itemType, other.itemType)
               && Objects.equals(mimeType, other.mimeType)
               && Objects.equals(encoding, other.encoding);
    }
}
