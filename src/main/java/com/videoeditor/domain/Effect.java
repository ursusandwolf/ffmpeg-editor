package com.videoeditor.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Effect that can be applied to a clip.
 */
public class Effect {
    public enum EffectType {
        FADE_IN,
        FADE_OUT,
        WATERMARK,
        MUTE,
        SPEED_CHANGE,
        CROP,
        RESIZE
    }

    private final EffectType type;
    private final Map<String, Object> parameters;

    public Effect(EffectType type) {
        this.type = type;
        this.parameters = new HashMap<>();
    }

    public Effect(EffectType type, Map<String, Object> parameters) {
        this.type = type;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public EffectType getType() {
        return type;
    }

    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    @Override
    public String toString() {
        return "Effect{type=" + type + ", parameters=" + parameters + "}";
    }
}
