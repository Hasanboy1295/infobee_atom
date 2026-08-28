package com.infobee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "codes", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "code_value"}))
public class Code {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private CodeGroup group;

    @Column(name = "code_value", nullable = false, length = 50)
    private String codeValue;

    @Column(name = "code_label", nullable = false, length = 200)
    private String codeLabel;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean enabled = true;

    public Code() {}

    public Code(CodeGroup group, String codeValue, String codeLabel, Integer sortOrder) {
        this.group = group;
        this.codeValue = codeValue;
        this.codeLabel = codeLabel;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public CodeGroup getGroup() { return group; }
    public void setGroup(CodeGroup group) { this.group = group; }
    public String getCodeValue() { return codeValue; }
    public void setCodeValue(String codeValue) { this.codeValue = codeValue; }
    public String getCodeLabel() { return codeLabel; }
    public void setCodeLabel(String codeLabel) { this.codeLabel = codeLabel; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
