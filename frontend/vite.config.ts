import { defineConfig } from 'vite';
import { readFileSync } from 'fs';
import { resolve } from 'path';

const DEV_SERVER_PORT = 3001;
const COOP_HEADER = 'Cross-Origin-Opener-Policy';
const COEP_HEADER = 'Cross-Origin-Embedder-Policy';
const COOP_VALUE = 'same-origin';
const COEP_VALUE = 'require-corp';
const SECURITY_HEADERS = {
  [COOP_HEADER]: COOP_VALUE,
  [COEP_HEADER]: COEP_VALUE,
} as const;

const RANDOMX_REQUEST_PATH = '/randomx-web.js';
const CAPTCHA_STANDALONE_REQUEST_PATH = '/captcha-standalone.js';
const CAPTCHA_STANDALONE_ENTRY_PATH = '/src/captcha-standalone.ts';
const RANDOMX_COMMIT_ID = '7a439f3eec74';
const RANDOMX_DIST_FILE = `randomx-web.${RANDOMX_COMMIT_ID}.js`;
const RANDOMX_DIST_PATH = resolve(__dirname, `./vendor/randomx/${RANDOMX_DIST_FILE}`);
const CHALLENGE_HTML_PATH = resolve(__dirname, './pages/challenge.html');
const BAN_HTML_PATH = resolve(__dirname, './pages/ban.html');
const CAPTCHA_STANDALONE_BUILD_PATH = resolve(__dirname, './src/captcha-standalone.ts');

export default defineConfig({
  server: {
    host: '0.0.0.0',
    port: DEV_SERVER_PORT,
    allowedHosts: ['example.com', 'localhost'],
    open: false,
    cors: true,
    headers: SECURITY_HEADERS,
  },
  build: {
    rollupOptions: {
      input: {
        challenge: CHALLENGE_HTML_PATH,
        ban: BAN_HTML_PATH,
        captchaStandalone: CAPTCHA_STANDALONE_BUILD_PATH,
      },
      output: {
        entryFileNames: (chunkInfo) =>
          chunkInfo.name === 'captchaStandalone' ? 'captcha-standalone.js' : 'assets/[name]-[hash].js',
      },
    },
  },
  worker: {
    rollupOptions: {
      external: [RANDOMX_REQUEST_PATH],
    },
  },
  plugins: [
    {
      name: 'serve-captcha-standalone',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url?.startsWith(CAPTCHA_STANDALONE_REQUEST_PATH)) {
            res.setHeader('Content-Type', 'application/javascript');
            for (const [header, value] of Object.entries(SECURITY_HEADERS)) {
              res.setHeader(header, value);
            }
            res.end(
              `(() => {` +
                `const s = document.createElement("script");` +
                `s.type = "module";` +
                `s.src = "${CAPTCHA_STANDALONE_ENTRY_PATH}";` +
                `document.head.appendChild(s);` +
              `})();`
            );
          } else {
            next();
          }
        });
      },
    },
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
