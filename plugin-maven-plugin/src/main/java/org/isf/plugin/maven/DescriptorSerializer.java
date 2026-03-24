/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2024 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 */
package org.isf.plugin.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.isf.plugin.model.FieldPermission;
import org.isf.plugin.model.PluginDescriptor;
import org.isf.plugin.model.PluginDescriptor.ExternalConnection;
import org.isf.plugin.model.ui.RouteDescriptor;
import org.isf.plugin.model.ui.SlotContribution;
import org.isf.plugin.model.ui.UiContribution;

/**
 * Serializes a {@link PluginDescriptor} to a pretty-printed JSON string
 * suitable for use as {@code manifest.json}.
 *
 * <p>Uses Jackson for correct escaping, Unicode handling, and consistent
 * formatting. Lives in the {@code plugin-maven-plugin} — not in the SPI —
 * so that the SPI itself remains dependency-free.
 */
final class DescriptorSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private DescriptorSerializer() {}

    static String toJson(PluginDescriptor d) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();

        root.put("pluginId",       d.getPluginId());
        root.put("version",        d.getVersion());
        root.put("name",           d.getName());
        putNullable(root, "description", d.getDescription());
        putNullable(root, "vendor",      d.getVendor());
        putNullable(root, "license",     d.getLicense());
        root.put("entryPoint",     d.getEntryPoint());
        root.put("minCoreVersion", d.getMinCoreVersion());
        putNullable(root, "maxCoreVersion", d.getMaxCoreVersion());

        ArrayNode caps = root.putArray("capabilities");
        d.getCapabilities().forEach(c -> caps.add(c.name()));

        ArrayNode perms = root.putArray("permissions");
        d.getPermissions().forEach(p -> perms.add(p.name()));

        ArrayNode deps = root.putArray("dependencies");
        d.getDependencies().forEach(deps::add);

        ArrayNode conns = root.putArray("externalConnections");
        for (ExternalConnection c : d.getExternalConnections()) {
            ObjectNode cn = conns.addObject();
            cn.put("host",      c.host());
            cn.put("port",      c.port());
            cn.put("protocol",  c.protocol());
            cn.put("purpose",   c.purpose());
            cn.put("direction", c.direction().name());
        }

        ArrayNode fps = root.putArray("fieldPermissions");
        for (FieldPermission fp : d.getFieldPermissions()) {
            ObjectNode fn = fps.addObject();
            fn.put("domain",  fp.getDomainName());
            fn.put("access",  fp.getAccess().name());
            ArrayNode fields = fn.putArray("fields");
            fp.fieldNames().forEach(fields::add);
            fn.put("purpose", fp.getPurpose());
        }

        if (d.getUiContribution() != null) {
            root.set("uiContribution", serializeUi(d.getUiContribution()));
        }

        return MAPPER.writeValueAsString(root);
    }

    private static ObjectNode serializeUi(UiContribution ui) {
        ObjectNode node = MAPPER.createObjectNode();

        if (ui.getBundle() != null) {
            ObjectNode bundle = node.putObject("bundle");
            bundle.put("entry",      ui.getBundle().entry());
            bundle.put("remoteName", ui.getBundle().remoteName());
        }

        if (ui.hasRoutes()) {
            ArrayNode routes = node.putArray("routes");
            for (RouteDescriptor r : ui.getRoutes()) {
                ObjectNode rn = routes.addObject();
                rn.put("path",     r.path());
                rn.put("label",    r.label());
                rn.put("menuPath", r.menuPath());
                if (r.permission() != null) {
                    rn.put("permission", r.permission().name());
                }
            }
        }

        if (ui.hasSlots()) {
            ArrayNode slots = node.putArray("slots");
            for (SlotContribution s : ui.getSlots()) {
                ObjectNode sn = slots.addObject();
                sn.put("slotId", s.slotId());
                sn.put("mode",   s.mode().name());
            }
        }

        return node;
    }

    private static void putNullable(ObjectNode node, String key, String value) {
        if (value == null) {
            node.putNull(key);
        } else {
            node.put(key, value);
        }
    }
}
