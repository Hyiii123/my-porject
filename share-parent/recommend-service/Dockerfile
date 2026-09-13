FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -i https://mirrors.aliyun.com/pypi/simple/ -r requirements.txt
COPY course_reco/ ./course_reco/
COPY service/ ./service/
COPY data/ ./data/
EXPOSE 5000
CMD ["python", "service/server.py", "--host", "0.0.0.0", "--port", "5000"]
