ARG RANDOMX_BUILDER_IMAGE=pouw-randomx-builder:local
ARG NODE_IMAGE=node:22-bookworm-slim
ARG NGINX_IMAGE=nginx:1.27-alpine
ARG NPM_REGISTRY=https://registry.npmjs.org/

FROM ${RANDOMX_BUILDER_IMAGE} AS randomx

FROM ${NODE_IMAGE} AS build
WORKDIR /workspace/frontend

COPY package.json package-lock.json ./
ARG NPM_REGISTRY
RUN NPM_REGISTRY="${NPM_REGISTRY}" node -e "const fs=require('fs');const p='package-lock.json';const lock=JSON.parse(fs.readFileSync(p,'utf8'));const raw=process.env.NPM_REGISTRY||'https://registry.npmjs.org/';const registry=raw.endsWith('/')?raw:raw+'/';let changed=0;const rewrite=(url)=>{if(typeof url!=='string')return url;const m=url.match(/^https?:\\/\\/[^/]+\\/repository\\/npm-public\\/(.*)$/);if(!m)return url;changed++;return registry+m[1];};if(lock.packages){for(const key of Object.keys(lock.packages)){const pkg=lock.packages[key];if(pkg&&pkg.resolved){pkg.resolved=rewrite(pkg.resolved);}}}if(lock.dependencies){const walk=(deps)=>{for(const name of Object.keys(deps||{})){const dep=deps[name];if(dep&&dep.resolved){dep.resolved=rewrite(dep.resolved);}if(dep&&dep.dependencies){walk(dep.dependencies);}}};walk(lock.dependencies);}fs.writeFileSync(p,JSON.stringify(lock,null,2)+'\\n');console.log('Rewritten lockfile URLs:',changed);" \
  && npm config set registry "${NPM_REGISTRY}" \
  && npm ci

COPY . .
RUN mkdir -p ./vendor/randomx
COPY --from=randomx /artifact/ ./vendor/randomx/
RUN npm run build

FROM ${NGINX_IMAGE} AS frontend-image
COPY --from=build /workspace/frontend/dist/ /usr/share/nginx/html/
EXPOSE 80

FROM scratch AS compose
COPY docker-compose.frontend.yml /docker-compose.yml
