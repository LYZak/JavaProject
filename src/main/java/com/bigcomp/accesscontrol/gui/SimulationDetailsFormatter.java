package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;

import javax.swing.JTable;
import java.time.format.DateTimeFormatter;
import java.util.Set;

final class SimulationDetailsFormatter {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static Object modelValue(JTable table, int viewRow, int modelCol) {
        if (table == null || viewRow < 0) {
            return null;
        }
        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0) {
            return null;
        }
        return table.getModel().getValueAt(row, modelCol);
    }

    static String formatUser(User user, Badge badge, String badgeCode, Set<String> profiles) {
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.t("simw.details.type.user")).append('\n');
        sb.append(I18n.t("simw.details.userId")).append(": ").append(user != null ? user.getId() : I18n.t("common.unknown")).append('\n');
        sb.append(I18n.t("simw.details.userName")).append(": ").append(user != null ? user.getFullName() : I18n.t("common.unknown")).append('\n');
        sb.append(I18n.t("simw.details.userGender")).append(": ").append(user != null ? user.getGender() : I18n.t("common.unknown")).append('\n');
        sb.append(I18n.t("simw.details.userType")).append(": ").append(user != null ? user.getUserType() : I18n.t("common.unknown")).append('\n');
        sb.append(I18n.t("simw.details.badgeId")).append(": ").append(user != null && user.getBadgeId() != null ? user.getBadgeId() : "").append('\n');
        sb.append(I18n.t("simw.details.badgeCode")).append(": ").append(badgeCode != null ? badgeCode : "").append('\n');

        if (badge != null) {
            sb.append(I18n.t("simw.details.badgeValid")).append(": ").append(badge.isValid()).append('\n');
            sb.append(I18n.t("simw.details.badgeCreatedAt")).append(": ").append(badge.getCreationDate() != null ? badge.getCreationDate().format(TS) : "").append('\n');
            sb.append(I18n.t("simw.details.badgeExpiresAt")).append(": ").append(badge.getExpirationDate() != null ? badge.getExpirationDate().format(TS) : "").append('\n');
            sb.append(I18n.t("simw.details.badgeLastUpdateAt")).append(": ").append(badge.getLastUpdateDate() != null ? badge.getLastUpdateDate().format(TS) : "").append('\n');
            sb.append(I18n.t("simw.details.badgeNeedsUpdate")).append(": ").append(badge.needsUpdate()).append('\n');
        }

        sb.append(I18n.t("simw.details.profiles")).append(": ");
        if (profiles == null || profiles.isEmpty()) {
            sb.append(I18n.t("common.none"));
        } else {
            sb.append(String.join(", ", profiles));
        }
        sb.append('\n');
        return sb.toString();
    }

    static String formatReader(BadgeReader reader, Resource resource, String groupName) {
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.t("simw.details.type.reader")).append('\n');
        sb.append(I18n.t("simw.details.readerId")).append(": ").append(reader != null ? reader.getId() : I18n.t("common.unknown")).append('\n');
        sb.append(I18n.t("simw.details.resourceId")).append(": ").append(reader != null ? reader.getResourceId() : "").append('\n');
        sb.append(I18n.t("simw.details.groupName")).append(": ").append(groupName != null ? groupName : I18n.t("common.none")).append('\n');

        if (resource != null) {
            sb.append(I18n.t("simw.details.resourceName")).append(": ").append(resource.getName()).append('\n');
            sb.append(I18n.t("simw.details.resourceType")).append(": ").append(resource.getType()).append('\n');
            sb.append(I18n.t("simw.details.resourceLocation")).append(": ").append(resource.getLocation()).append('\n');
            sb.append(I18n.t("simw.details.resourceBuilding")).append(": ").append(resource.getBuilding()).append('\n');
            sb.append(I18n.t("simw.details.resourceFloor")).append(": ").append(resource.getFloor()).append('\n');
            sb.append(I18n.t("simw.details.resourceState")).append(": ").append(resource.getState()).append('\n');
            sb.append(I18n.t("simw.details.badgeReaderId")).append(": ").append(resource.getBadgeReaderId()).append('\n');
        }

        return sb.toString();
    }

    private SimulationDetailsFormatter() {
    }
}

