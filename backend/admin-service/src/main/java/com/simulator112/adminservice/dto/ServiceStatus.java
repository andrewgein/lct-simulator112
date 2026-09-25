package com.simulator112.adminservice.dto;

public record ServiceStatus(String service, boolean up, Double http5xxRate, Double heapUsedBytes) {}
