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

const CAPTCHA_ENTRY_BY_REQUEST_PATH = {
  '/captcha-standalone-sha256.js': '/src/entries/captcha-standalone-sha256.ts',
  '/captcha-standalone-bitcoin.js': '/src/entries/captcha-standalone-bitcoin.ts',
  '/captcha-standalone-randomx.js': '/src/entries/captcha-standalone-randomx.ts',
} as const;
const CHALLENGE_HTML_PATH = resolve(__dirname, './pages/challenge.html');
const BAN_HTML_PATH = resolve(__dirname, './pages/ban.html');
const CAPTCHA_STANDALONE_SHA256_BUILD_PATH = resolve(__dirname, './src/entries/captcha-standalone-sha256.ts');
const CAPTCHA_STANDALONE_BITCOIN_BUILD_PATH = resolve(__dirname, './src/entries/captcha-standalone-bitcoin.ts');
const CAPTCHA_STANDALONE_RANDOMX_BUILD_PATH = resolve(__dirname, './src/entries/captcha-standalone-randomx.ts');

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
        captchaStandaloneSha256: CAPTCHA_STANDALONE_SHA256_BUILD_PATH,
        captchaStandaloneBitcoin: CAPTCHA_STANDALONE_BITCOIN_BUILD_PATH,
        captchaStandaloneRandomx: CAPTCHA_STANDALONE_RANDOMX_BUILD_PATH,
      },
      output: {
        entryFileNames: (chunkInfo) => {
          switch (chunkInfo.name) {
            case 'captchaStandaloneSha256':
              return 'captcha-standalone-sha256.js';
            case 'captchaStandaloneBitcoin':
              return 'captcha-standalone-bitcoin.js';
            case 'captchaStandaloneRandomx':
              return 'captcha-standalone-randomx.js';
            default:
              return 'assets/[name]-[hash].js';
          }
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
          const entryPath = requestUrl
            ? CAPTCHA_ENTRY_BY_REQUEST_PATH[requestUrl as keyof typeof CAPTCHA_ENTRY_BY_REQUEST_PATH]
            : undefined;

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
