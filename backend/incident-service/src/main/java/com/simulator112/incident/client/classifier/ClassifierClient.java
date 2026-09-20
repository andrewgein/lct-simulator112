package com.simulator112.incident.client.classifier;

import com.simulator112.incident.dto.view.classifier.ClassifierEntryView;

public interface ClassifierClient {
  ClassifierEntryView getEntry(String classifierCode);
}
