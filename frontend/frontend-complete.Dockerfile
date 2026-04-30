ARG NODE_IMAGE=node:22-bookworm-slim

FROM ${NODE_IMAGE} AS build
WORKDIR /workspace/frontend

COPY package.json package-lock.json .npmrc ./
RUN sed -i 's#https://nexus.c-com-system.net/repository/npm-public/#https://registry.npmjs.org/#g' package-lock.json \
  && npm ci

COPY . .
RUN npm run build

FROM ${NODE_IMAGE} AS frontend-image
WORKDIR /workspace/frontend
COPY --from=build /workspace/frontend/ /workspace/frontend/
EXPOSE 3001
ENTRYPOINT ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "3001"]

FROM scratch AS compose
COPY docker-compose.frontend.yml /docker-compose.yml
