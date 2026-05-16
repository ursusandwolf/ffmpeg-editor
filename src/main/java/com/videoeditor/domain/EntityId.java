package com.videoeditor.domain;

import java.util.UUID;

/**
 * Unique identifier for domain entities.
 */
public class EntityId {
    private final String value;

    public EntityId() {
        this.value = UUID.randomUUID().toString();
    }

    public EntityId(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityId)) return false;
        EntityId entityId = (EntityId) o;
        return value.equals(entityId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    public static EntityId generate() {
        return new EntityId();
    }
}
