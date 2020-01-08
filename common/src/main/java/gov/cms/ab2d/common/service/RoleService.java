package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Role;

public interface RoleService {

    Role findRoleByName(String name);
}
