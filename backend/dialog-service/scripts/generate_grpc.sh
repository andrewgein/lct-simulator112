#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
PROTO_ROOT="../shared/src/main/proto"

python3 -m grpc_tools.protoc \
  -I"$PROTO_ROOT" \
  --python_out=app/grpc \
  --pyi_out=app/grpc \
  --grpc_python_out=app/grpc \
  "$PROTO_ROOT"/com/simulator112/incident/incident_context.proto \
  "$PROTO_ROOT"/com/simulator112/context/context_service.proto

# Generated imports are rooted at `com`; this project exposes generated modules under `app.grpc.com`.
find app/grpc -type f \( -name '*_pb2.py' -o -name '*_pb2_grpc.py' -o -name '*.pyi' \) \
  -exec perl -pi -e 's/^from com\.simulator112/from app.grpc.com.simulator112/' {} +
