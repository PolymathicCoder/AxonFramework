/*
 * Copyright (c) 2010-2011. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.serializer;

import org.axonframework.common.SerializationException;
import org.axonframework.common.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Serializer implementation that uses Java serialization to serialize and deserialize object instances. This
 * implementation is very suitable if the life span of the serialized objects allows classes to remain unchanged. If
 * Class definitions need to be changed during the object's life cycle, another implementation, like the
 * {@link XStreamSerializer} might be a more suitable alternative.
 *
 * @author Allard Buijze
 * @since 2.0
 */
public class JavaSerializer implements Serializer {

    private static final Logger logger = LoggerFactory.getLogger(JavaSerializer.class);

    @SuppressWarnings({"NonSerializableObjectPassedToObjectStream"})
    @Override
    public SerializedType serialize(Object object, OutputStream outputStream) throws IOException {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            try {
                oos.writeObject(object);
            } finally {
                oos.flush();
            }
        } catch (IOException e) {
            throw new SerializationException("An exception occurred writing serialized data to the output stream", e);
        }
        return new SimpleSerializedType(object.getClass().getName(), revisionOf(object.getClass()));
    }

    @Override
    public SerializedObject serialize(Object instance) {
        return serializeObject(instance);
    }

    @Override
    public Object deserialize(SerializedObject serializedObject) {
        InputStream stream = serializedObject.getStream();
        try {
            return deserialize(stream);
        } catch (IOException e) {
            throw new SerializationException("The theoretically impossible has just happened: "
                                                     + "An IOException while reading to a ByteArrayInputStream.", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @Override
    public Class classForType(SerializedType type) {
        try {
            return Class.forName(type.getName());
        } catch (ClassNotFoundException e) {
            logger.warn("Could not load class for serialized type [{}] revision {}",
                        type.getName(), type.getRevision());
            return null;
        }
    }

    /**
     * Returns the revision number for the given <code>type</code>. The default implementation checks for an {@link
     * Revision @Revision} annotation, and returns <code>0</code> if none was found. This method can be safely
     * overridden by subclasses.
     * <p/>
     * The revision number is used by upcasters to decide whether they need to process a certain serialized event.
     * Generally, the revision number needs to be increased each time the structure of an event has been changed in an
     * incompatible manner.
     *
     * @param type The type for which to return the revision number
     * @return the revision number for the given <code>type</code>
     */
    protected int revisionOf(Class<?> type) {
        Revision revision = type.getAnnotation(Revision.class);
        return revision == null ? 0 : revision.value();
    }

    private SerializedObject serializeObject(Object instance) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            serialize(instance, baos);
        } catch (IOException e) {
            throw new SerializationException("The theoretically impossible has just happened: "
                                                     + "An IOException while writing to a ByteArrayOutputStream.", e);
        }
        return new SimpleSerializedObject(baos.toByteArray(), instance.getClass().getName(),
                                          revisionOf(instance.getClass()));
    }

    private Object deserialize(InputStream inputStream) throws IOException {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(inputStream);
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new SerializationException("An error occurred while deserializing: " + e.getMessage(), e);
        }
    }
}
