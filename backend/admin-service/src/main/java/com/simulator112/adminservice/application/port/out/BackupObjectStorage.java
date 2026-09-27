package com.simulator112.adminservice.application.port.out;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface BackupObjectStorage {
  void ensureBucketReady();

  long upload(Path file, String key) throws IOException;

  List<String> listKeys(String prefix);

  InputStream open(String key);

  void applyRetentionPolicy();
}
