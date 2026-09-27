package com.simulator112.adminservice.application.port.out;

import com.simulator112.adminservice.domain.model.PrometheusSample;
import java.util.List;

public interface MetricsQueryPort {
  List<PrometheusSample> query(String promql);
}
