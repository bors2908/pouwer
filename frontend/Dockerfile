ARG NODE_IMAGE=node:22-bookworm-slim

FROM ${NODE_IMAGE} AS build
WORKDIR /workspace/frontend
COPY docker.npmrc ./.npmrc
COPY package.json package-lock.json ./
RUN sed -i 's#http://localhost:9001/#http://host.docker.internal:9001/#g' package-lock.json \
    && npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine AS frontend-image
COPY --from=build /workspace/frontend/dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3001
