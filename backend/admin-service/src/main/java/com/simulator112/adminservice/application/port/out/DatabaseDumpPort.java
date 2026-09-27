package com.simulator112.adminservice.application.port.out;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface DatabaseDumpPort {
  List<String> configuredDatabases();

  Path dumpDatabase(String database) throws IOException, InterruptedException;

  Optional<Path> encryptSecrets();
}
