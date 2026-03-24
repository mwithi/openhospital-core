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
package org.isf.plugin.test.stub;

import org.isf.plugin.model.FieldPermission;
import org.isf.plugin.model.PluginCapability;
import org.isf.plugin.model.PluginDescriptor;
import org.isf.plugin.model.PluginPermission;
import org.isf.plugin.model.field.AdmissionField;
import org.isf.plugin.model.field.DomainField;
import org.isf.plugin.model.field.LaboratoryField;
import org.isf.plugin.model.field.PatientField;
import org.isf.plugin.model.field.PharmacyField;
import org.isf.plugin.model.field.WardField;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a {@code manifest.json} resource from the classpath and builds
 * a {@link PluginDescriptor} from it.
 *
 * <p>Used by {@link StubPluginContext#fromManifest(String)} to allow tests
 * to use the real manifest as the source of truth rather than duplicating
 * descriptor values in test setup code.
 *
 * <p>This class is intentionally package-private — use it only via
 * {@link StubPluginContext#fromManifest(String)}.
 */
final class ManifestReader {

    private ManifestReader() {}

    static PluginDescriptor read(String resourcePath) {
        String json = loadResource(resourcePath);
        JSONObject obj = new JSONObject(json);

        PluginDescriptor.Builder builder = PluginDescriptor.builder()
                .pluginId(obj.getString("pluginId"))
                .version(obj.getString("version"))
                .name(obj.getString("name"))
                .entryPoint(obj.getString("entryPoint"))
                .minCoreVersion(obj.getString("minCoreVersion"));

        if (obj.has("description"))
            builder.description(obj.getString("description"));
        if (obj.has("vendor"))
            builder.vendor(obj.getString("vendor"));
        if (obj.has("license"))
            builder.license(obj.getString("license"));
        if (obj.has("maxCoreVersion"))
            builder.maxCoreVersion(obj.getString("maxCoreVersion"));

        if (obj.has("capabilities")) {
            List<PluginCapability> caps = new ArrayList<>();
            for (Object c : obj.getJSONArray("capabilities")) {
                caps.add(PluginCapability.valueOf((String) c));
            }
            builder.capabilities(caps);
        }

        if (obj.has("permissions")) {
            List<PluginPermission> perms = new ArrayList<>();
            for (Object p : obj.getJSONArray("permissions")) {
                perms.add(PluginPermission.valueOf((String) p));
            }
            builder.permissions(perms);
        }

        if (obj.has("fieldPermissions")) {
            List<FieldPermission> fps = new ArrayList<>();
            for (Object fp : obj.getJSONArray("fieldPermissions")) {
                fps.add(parseFieldPermission((JSONObject) fp));
            }
            builder.fieldPermissions(fps);
        }

        return builder.build();
    }

    private static FieldPermission parseFieldPermission(JSONObject fp) {
        String domain  = fp.getString("domain");
        String access  = fp.getString("access");
        String purpose = fp.getString("purpose");
        JSONArray fields = fp.getJSONArray("fields");

        List<DomainField> domainFields = new ArrayList<>();
        for (int i = 0; i < fields.length(); i++) {
            domainFields.add(resolveField(domain, fields.getString(i)));
        }

        DomainField[] arr = domainFields.toArray(new DomainField[0]);
        if ("WRITE".equalsIgnoreCase(access)) {
            return FieldPermission.write(castToConcreteType(domain, arr)).purpose(purpose);
        } else {
            return FieldPermission.read(castToConcreteType(domain, arr)).purpose(purpose);
        }
    }

    private static DomainField resolveField(String domain, String fieldName) {
        if ("Patient".equals(domain))    return findByName(PatientField.values(),    fieldName);
        if ("Admission".equals(domain))  return findByName(AdmissionField.values(),  fieldName);
        if ("Laboratory".equals(domain)) return findByName(LaboratoryField.values(), fieldName);
        if ("Ward".equals(domain))       return findByName(WardField.values(),       fieldName);
        if ("Pharmacy".equals(domain))   return findByName(PharmacyField.values(),   fieldName);
        throw new IllegalArgumentException("Unknown domain: " + domain);
    }

    private static <F extends DomainField> F findByName(F[] values, String fieldName) {
        for (F f : values) {
            if (f.fieldName().equals(fieldName)) return f;
        }
        throw new IllegalArgumentException("Unknown field '" + fieldName + "'");
    }

    @SuppressWarnings("unchecked")
    private static <F extends DomainField> F[] castToConcreteType(
            String domain, DomainField[] fields) {
        if ("Patient".equals(domain))    return (F[]) toArray(fields, PatientField.class);
        if ("Admission".equals(domain))  return (F[]) toArray(fields, AdmissionField.class);
        if ("Laboratory".equals(domain)) return (F[]) toArray(fields, LaboratoryField.class);
        if ("Ward".equals(domain))       return (F[]) toArray(fields, WardField.class);
        if ("Pharmacy".equals(domain))   return (F[]) toArray(fields, PharmacyField.class);
        throw new IllegalArgumentException("Unknown domain: " + domain);
    }

    private static <F extends DomainField> F[] toArray(DomainField[] fields,
                                                        Class<F> type) {
        @SuppressWarnings("unchecked")
        F[] arr = (F[]) java.lang.reflect.Array.newInstance(type, fields.length);
        for (int i = 0; i < fields.length; i++) {
            arr[i] = type.cast(fields[i]);
        }
        return arr;
    }

    private static String loadResource(String resourcePath) {
        try (InputStream is = ManifestReader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException(
                    "manifest.json not found on classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read manifest: " + resourcePath, e);
        }
    }
}
