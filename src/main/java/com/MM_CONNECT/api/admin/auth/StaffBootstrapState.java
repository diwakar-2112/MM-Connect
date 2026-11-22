package com.MM_CONNECT.api.admin.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_bootstrap_state")
public class StaffBootstrapState {
    @Id private Short id;
    protected StaffBootstrapState() {}
}
