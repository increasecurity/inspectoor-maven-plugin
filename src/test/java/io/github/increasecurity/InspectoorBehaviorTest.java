package io.github.increasecurity;

import io.github.increasecurity.model.CheckSpecMode;
import io.github.increasecurity.model.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectoorBehaviorTest {

    @Test
    void supportsAllSpecificationCheckModes() {
        assertEquals(CheckSpecMode.NONE, CheckSpecMode.fromString(null));
        assertEquals(CheckSpecMode.NONE, CheckSpecMode.fromString("none"));
        assertEquals(CheckSpecMode.WARN, CheckSpecMode.fromString(" warn "));
        assertEquals(CheckSpecMode.FAIL, CheckSpecMode.fromString("FAIL"));
    }

    @Test
    void serializesProjectWithoutEmptyFields() {
        Project project = new Project();
        project.setName("service");
        project.setVersion("1.0.0");

        String json = InspectoorUtil.toJson(project);

        assertTrue(json.contains("\"name\":\"service\""));
        assertTrue(json.contains("\"version\":\"1.0.0\""));
        assertFalse(json.contains("\"description\""));
        assertTrue(json.contains("\"specs\":[]"));
    }
}
