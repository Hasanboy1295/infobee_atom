package com.infobee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "code_groups", uniqueConstraints = @UniqueConstraint(columnNames = {"group_code"}))
public class CodeGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", nullable = false, length = 50)
    private String groupCode;

    @Column(nullable = false, length = 200)
    private String groupName;

    @Column(columnDefinition = "TEXT")
    private String description;

    public CodeGroup() {}

    public CodeGroup(String groupCode, String groupName, String description) {
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
