ARG NODE_IMAGE=node:22-bookworm-slim

FROM ${NODE_IMAGE} AS build
WORKDIR /workspace/frontend
COPY docker.npmrc ./.npmrc
COPY package.json ./
RUN npm install

COPY . .
RUN npm run build

FROM ${NODE_IMAGE} AS frontend-image
WORKDIR /workspace/frontend
COPY --from=build /workspace/frontend/ /workspace/frontend/
EXPOSE 3001
ENTRYPOINT ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "3001"]
