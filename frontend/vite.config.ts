import { defineConfig } from 'vite';
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

const CHALLENGE_HTML_PATH = resolve(__dirname, './pages/challenge.html');
const BAN_HTML_PATH = resolve(__dirname, './pages/ban.html');

const CAPTCHA_STANDALONE_ENTRIES = [
  {
    rollupName: 'captchaStandaloneSha256',
    requestPath: '/captcha-standalone-sha256.js',
    sourcePath: '/src/entries/captcha-standalone-sha256.ts',
  },
  {
    rollupName: 'captchaStandaloneBitcoin',
    requestPath: '/captcha-standalone-bitcoin.js',
    sourcePath: '/src/entries/captcha-standalone-bitcoin.ts',
  },
  {
    rollupName: 'captchaStandaloneRandomx',
    requestPath: '/captcha-standalone-randomx.js',
    sourcePath: '/src/entries/captcha-standalone-randomx.ts',
  },
] as const;

const CAPTCHA_ENTRY_BY_REQUEST_PATH = Object.fromEntries(
  CAPTCHA_STANDALONE_ENTRIES.map((entry) => [entry.requestPath, entry.sourcePath])
);

const CAPTCHA_FILE_BY_ROLLUP_NAME = Object.fromEntries(
  CAPTCHA_STANDALONE_ENTRIES.map((entry) => [entry.rollupName, entry.requestPath.slice(1)])
);

const CAPTCHA_ROLLUP_INPUT = Object.fromEntries(
  CAPTCHA_STANDALONE_ENTRIES.map((entry) => [entry.rollupName, resolve(__dirname, `.${entry.sourcePath}`)])
);

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
        ...CAPTCHA_ROLLUP_INPUT,
      },
      output: {
        entryFileNames: (chunkInfo) => {
          const fileName = CAPTCHA_FILE_BY_ROLLUP_NAME[chunkInfo.name];
          if (fileName) {
            return fileName;
          }
          return 'assets/[name]-[hash].js';
        },
      },
    },
  },
  worker: {
    format: 'es',
  },
  plugins: [
    {
      name: 'serve-captcha-standalone',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          const requestUrl = req.url?.split('?')[0];
          const entryPath = requestUrl ? CAPTCHA_ENTRY_BY_REQUEST_PATH[requestUrl] : undefined;

          if (entryPath) {
            res.setHeader('Content-Type', 'application/javascript');
            for (const [header, value] of Object.entries(SECURITY_HEADERS)) {
              res.setHeader(header, value);
            }
            res.end(
              `(() => {` +
                `const s = document.createElement("script");` +
                `s.type = "module";` +
                `s.src = "${entryPath}";` +
                `document.head.appendChild(s);` +
              `})();`
            );
          } else {
            next();
          }
        });
      },
    },
  ],
});
