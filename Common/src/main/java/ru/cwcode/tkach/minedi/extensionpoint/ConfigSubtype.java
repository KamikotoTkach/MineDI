package ru.cwcode.tkach.minedi.extensionpoint;

/**
 * A subtype of a {@link ru.cwcode.tkach.minedi.annotation.ConfigSubtypes} base, published by the plugin that declares it.
 *
 * @param name   the type id, from {@code @JsonTypeName}
 * @param plugin the plugin that published it
 */
public record ConfigSubtype(Class<?> base, Class<?> type, String name, String plugin) {
}
