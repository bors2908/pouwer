import { defineConfig } from 'vite';
import { readFileSync } from 'fs';
import { resolve } from 'path';

const DEV_SERVER_PORT = 3000;
const COOP_HEADER = 'Cross-Origin-Opener-Policy';
const COEP_HEADER = 'Cross-Origin-Embedder-Policy';
const COOP_VALUE = 'same-origin';
const COEP_VALUE = 'require-corp';
const SECURITY_HEADERS = {
  [COOP_HEADER]: COOP_VALUE,
  [COEP_HEADER]: COEP_VALUE,
} as const;

const RANDOMX_REQUEST_PATH = '/randomx-web.js';
const RANDOMX_COMMIT_ID = '7a439f3eec74';
const RANDOMX_DIST_FILE = `randomx-web.${RANDOMX_COMMIT_ID}.js`;
const RANDOMX_DIST_PATH = resolve(__dirname, `./vendor/randomx/${RANDOMX_DIST_FILE}`);

export default defineConfig({
  server: {
    port: DEV_SERVER_PORT,
    headers: SECURITY_HEADERS,
  },
  worker: {
    rollupOptions: {
      external: [RANDOMX_REQUEST_PATH],
    },
  },
  plugins: [
    {
      name: 'serve-randomx-web',
      resolveId(id) {
        if (id === RANDOMX_REQUEST_PATH) {
          return id;
        }
      },
      load(id) {
        if (id === RANDOMX_REQUEST_PATH) {
          return readFileSync(RANDOMX_DIST_PATH, 'utf-8');
        }
      },
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url === RANDOMX_REQUEST_PATH) {
            try {
              const content = readFileSync(RANDOMX_DIST_PATH);
              res.setHeader('Content-Type', 'application/javascript');
              for (const [header, value] of Object.entries(SECURITY_HEADERS)) {
                res.setHeader(header, value);
              }
              res.end(content);
            } catch {
              res.statusCode = 404;
              res.end(`File not found: ${RANDOMX_DIST_PATH}`);
            }
          } else {
            next();
          }
        });
      },
    },
  ],
});
