package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SimulationDetailsFormatterTest {

    @Test
    void modelValueReturnsCorrectRowWhenSorted() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"id", "name"}, 0);
        model.addRow(new Object[]{"U2", "B"});
        model.addRow(new Object[]{"U1", "A"});
        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.toggleSortOrder(0);

        Object idView0 = SimulationDetailsFormatter.modelValue(table, 0, 0);
        Object idView1 = SimulationDetailsFormatter.modelValue(table, 1, 0);
        assertEquals("U1", idView0);
        assertEquals("U2", idView1);
    }

    @Test
    void formatUserAndReaderDoesNotCrashWithMissingFields() {
        I18n.setLanguage(I18n.Language.ZH);
        User user = new User("U1", User.Gender.MALE, "A", "B", User.UserType.EMPLOYEE);
        Badge badge = new Badge("CODE", "U1", LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now(), true);
        String text = SimulationDetailsFormatter.formatUser(user, badge, "CODE", Set.of("P1"));
        assertTrue(text.contains("用户ID"));
        assertTrue(text.contains("P1"));

        String r = SimulationDetailsFormatter.formatReader(null, new Resource("R1", "Door", Resource.ResourceType.DOOR, "L", "B", "F"), null);
        assertTrue(r.length() > 0);
    }
}

