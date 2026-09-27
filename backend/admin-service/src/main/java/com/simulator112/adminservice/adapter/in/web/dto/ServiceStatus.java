package com.simulator112.adminservice.adapter.in.web.dto;

public record ServiceStatus(String service, boolean up, Double http5xxRate, Double heapUsedBytes) {}
