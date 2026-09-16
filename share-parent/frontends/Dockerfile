ARG TJ_NODE_REGISTRY=docker.m.daocloud.io
FROM ${TJ_NODE_REGISTRY}/library/node:20-alpine AS builder

ARG APP_DIR=frontends/portal
ARG BUILD_COMMAND=build:docker
ARG NPM_REGISTRY=https://registry.npmmirror.com
WORKDIR /src

RUN npm config set registry "${NPM_REGISTRY}" \
    && npm install -g pnpm

COPY frontends/pnpm-workspace.yaml frontends/package.json ./frontends/
COPY frontends/packages/ ./frontends/packages/
COPY ${APP_DIR}/package.json ./${APP_DIR}/

RUN cd ${APP_DIR} && pnpm install --dangerously-allow-all-builds

COPY ${APP_DIR}/ ./${APP_DIR}/
RUN cd ${APP_DIR} && pnpm run ${BUILD_COMMAND}

FROM ${TJ_NODE_REGISTRY}/library/nginx:1.27-alpine
COPY frontends/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /src/${APP_DIR}/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
