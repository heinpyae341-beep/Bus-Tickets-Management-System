package com.myanmar.bus;

public final class RolePolicy {
  private RolePolicy() {}
  public static boolean canManageAdmins(String role) { return "GLOBAL_ADMIN".equals(role); }
  public static boolean canManageOperations(String role) { return "LOCAL_ADMIN".equals(role); }
  public static boolean canViewOperations(String role) { return "GLOBAL_ADMIN".equals(role) || "LOCAL_ADMIN".equals(role); }
}
