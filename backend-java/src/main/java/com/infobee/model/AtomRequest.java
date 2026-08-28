package com.infobee.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "atom_requests")
public class AtomRequest extends BaseRequest {
    public AtomRequest() {}
}
