ARG TJ_NODE_REGISTRY=docker.m.daocloud.io
FROM ${TJ_NODE_REGISTRY}/library/node:20-alpine AS builder

ARG APP_DIR=frontends/portal
ARG BUILD_COMMAND=build:docker
ARG NPM_REGISTRY=https://registry.npmmirror.com
WORKDIR /src

COPY ${APP_DIR}/package*.json ./
RUN npm config set registry "${NPM_REGISTRY}" \
    && if [ -f package-lock.json ]; then npm ci --no-audit --no-fund; else npm install --no-audit --no-fund; fi
COPY ${APP_DIR}/ ./
RUN npm run ${BUILD_COMMAND}

FROM ${TJ_NODE_REGISTRY}/library/nginx:1.27-alpine
COPY frontends/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /src/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
