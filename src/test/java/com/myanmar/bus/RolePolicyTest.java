package com.myanmar.bus;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RolePolicyTest {
  @Test void globalAdminManagesOnlyAdminsAndViewsOperations() {
    assertTrue(RolePolicy.canManageAdmins("GLOBAL_ADMIN"));
    assertFalse(RolePolicy.canManageOperations("GLOBAL_ADMIN"));
    assertTrue(RolePolicy.canViewOperations("GLOBAL_ADMIN"));
  }
  @Test void localAdminManagesOperationsButNotAdmins() {
    assertFalse(RolePolicy.canManageAdmins("LOCAL_ADMIN"));
    assertTrue(RolePolicy.canManageOperations("LOCAL_ADMIN"));
    assertTrue(RolePolicy.canViewOperations("LOCAL_ADMIN"));
  }
}
