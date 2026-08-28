package com.infobee.dto;

import java.util.List;

public record BatchTransitionRequest(
    List<Long> ids,
    String action,   // approve, reject, cancel
    String note
) {}
