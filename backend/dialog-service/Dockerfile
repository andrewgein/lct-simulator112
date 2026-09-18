FROM python:3.12-slim

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app
COPY requirements.txt .

# silero-stress requires PyTorch. Install its CPU-only wheel explicitly so pip does
# not pull the much larger CUDA runtime from PyPI into this CPU service image.
ARG TORCH_INDEX_URL=https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir --no-compile --index-url "$TORCH_INDEX_URL" torch \
    && pip install --no-cache-dir --no-compile -r requirements.txt
COPY app ./app
COPY resources ./resources
EXPOSE 8005
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8005"]
